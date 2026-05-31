package com.uitstalie.nutrition.nutrition.server;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.uitstalie.nutrition.nutrition.api.data.NutritionDataRegistry;
import com.uitstalie.nutrition.nutrition.capabilities.nutrition.NutritionCapability;
import com.uitstalie.nutrition.nutrition.registry.AttributeTypeRegistry;
import com.uitstalie.nutrition.nutrition.service.NutritionAutoGenerateService;
import com.uitstalie.nutrition.nutrition.util.log.Log;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 营养系统命令：/nutrition set/get/list。
 *
 * <p>用于调试和加速测试。通过 {@code com.uitstalie.nutrition.nutrition.Nutrition}
 * 在 {@code NeoForge.EVENT_BUS} 上注册。</p>
 */
public final class NutritionCommand {

    private static final float NUTRITION_PERCENT_DIVISOR = 1000.0f;

    private NutritionCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("nutrition")
                        .then(Commands.literal("get")
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .executes(ctx -> getNutrition(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "group")))))
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100_000))
                                                .executes(ctx -> setNutrition(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "group"),
                                                        IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(Commands.literal("list")
                                .executes(ctx -> listNutrition(ctx.getSource())))
                        .then(Commands.literal("autogen")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> autoGenerate(ctx.getSource())))
                        .then(Commands.literal("find-seeds")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> findSeeds(ctx.getSource(), null))
                                .then(Commands.argument("mod", StringArgumentType.word())
                                        .executes(ctx -> findSeeds(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mod")))))
                        .then(Commands.literal("export")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> exportItems(ctx.getSource())))
        );
    }

    private static int getNutrition(CommandSourceStack src, String group) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Must be used by a player"));
            return 0;
        }
        NutritionCapability cap = player.getData(AttributeTypeRegistry.NutritionCapability);
        int value = cap.getNutritionData().getNutrition(group);
        float pct = value / NUTRITION_PERCENT_DIVISOR;

        src.sendSuccess(() -> Component.literal(
                group + ": " + value + " (" + String.format("%.3f", pct) + "%)"),
                false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setNutrition(CommandSourceStack src, String group, int value) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Must be used by a player"));
            return 0;
        }
        NutritionCapability cap = player.getData(AttributeTypeRegistry.NutritionCapability);
        cap.getNutritionData().setNutrition(group, value);
        float pct = value / NUTRITION_PERCENT_DIVISOR;

        src.sendSuccess(() -> Component.literal(
                "Set " + group + " to " + value + " (" + String.format("%.3f", pct) + "%)"),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listNutrition(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Must be used by a player"));
            return 0;
        }
        NutritionCapability cap = player.getData(AttributeTypeRegistry.NutritionCapability);
        var data = cap.getNutritionData();

        // 遍历所有已配置的营养组（不是只有数据的组）
        List<com.uitstalie.nutrition.nutrition.api.data.group.NutritionGroupJson> allGroups = NutritionDataRegistry.groups();

        if (allGroups.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No nutrition groups configured"), false);
            return Command.SINGLE_SUCCESS;
        }

        src.sendSuccess(() -> Component.literal("== Nutrition Values =="), false);
        for (var group : allGroups) {
            int value = data.getNutrition(group.groupName);
            float pct = value / NUTRITION_PERCENT_DIVISOR;
            src.sendSuccess(() -> Component.literal(
                    "  " + group.groupName + ": " + value + " (" + String.format("%.3f", pct) + "%)"),
                    false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int autoGenerate(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("Starting nutrition auto generate..."), true);
        NutritionAutoGenerateService.generate(src.getServer());
        src.sendSuccess(() -> Component.literal("Nutrition auto generate completed"), true);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * /nutrition find-seeds [mod]
     *
     * <p>扫描所有注册物品，找出具有 {@link FoodProperties} 但没有配方产出的物品。
     * 这些是"种子"——必须手动在 item JSON 中声明 group 归属，autogen BFS 才能覆盖下游。
     * 可选参数 mod 过滤指定命名空间。</p>
     */
    private static int findSeeds(CommandSourceStack src, String modFilter) {
        if (src.getServer() == null) {
            src.sendFailure(Component.literal("Must be used on a server"));
            return 0;
        }

        src.sendSuccess(() -> Component.literal("Scanning food items..."), true);

        // Step 1: 建立输出→配方索引（哪些物品能通过合成产出）
        Map<ResourceLocation, List<Recipe<?>>> outputIndex =
                NutritionAutoGenerateService.buildRecipeOutputIndex(src.getServer());

        // Step 2: 遍历所有物品，收集"可食用但无配方产出"的种子
        Map<String, List<String>> seedsByMod = new LinkedHashMap<>();
        int totalFoodCount = 0;

        for (Item item : BuiltInRegistries.ITEM) {
            FoodProperties food = item.getDefaultInstance().getFoodProperties(null);
            if (food == null) continue;
            totalFoodCount++;

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) continue;

            // 如果有配方产出此物品 → 不是种子（autogen 会处理）
            if (outputIndex.containsKey(itemId)) continue;

            String namespace = itemId.getNamespace();
            // 跳过原版（vanilla seeds 已在现有的 item JSON 中）
            if (namespace.equals("minecraft")) continue;

            if (modFilter != null && !namespace.equals(modFilter)) continue;

            seedsByMod.computeIfAbsent(namespace, k -> new ArrayList<>())
                    .add(itemId.toString());
        }

        // Step 3: 输出
        final int finalFoodCount = totalFoodCount;
        final int finalTotalSeeds = seedsByMod.values().stream().mapToInt(List::size).sum();
        final int finalModCount = seedsByMod.size();
        boolean filtered = modFilter != null;

        String header = "== Nutrition Seeds " + (filtered ? "(mod: " + modFilter + ") " : "") + "==";
        src.sendSuccess(() -> Component.literal(header), false);
        Log.d("find-seeds", header);

        String summary = "  Total food items: " + finalFoodCount + ", total seeds: " + finalTotalSeeds
                + " across " + finalModCount + " mods";
        src.sendSuccess(() -> Component.literal(summary), false);
        Log.d("find-seeds", summary);

        // 按种子数量降序排列
        List<Map.Entry<String, List<String>>> sorted = new ArrayList<>(seedsByMod.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));

        for (var entry : sorted) {
            String mod = entry.getKey();
            List<String> items = entry.getValue();

            String modHeader = "  [" + mod + "] " + items.size() + " seeds";
            src.sendSuccess(() -> Component.literal(modHeader), false);
            Log.d("find-seeds", modHeader);

            // 所有种子全部输出
            for (String id : items) {
                String line = id.replaceFirst("^" + mod + ":", "  - ");
                src.sendSuccess(() -> Component.literal(line), false);
                Log.d("find-seeds", line);
            }
        }

        // 写入 JSON 文件，方便离线分析
        writeSeedsToFile(seedsByMod, finalFoodCount, finalTotalSeeds, finalModCount);

        return Command.SINGLE_SUCCESS;
    }

    /** 将种子数据写入 run/find-seeds-output.json */
    private static void writeSeedsToFile(Map<String, List<String>> seedsByMod,
                                          int foodCount, int totalSeeds, int modCount) {
        try {
            Path outFile = Path.of("find-seeds-output.json");
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"summary\": {\n");
            json.append("    \"total_food_items\": ").append(foodCount).append(",\n");
            json.append("    \"total_seeds\": ").append(totalSeeds).append(",\n");
            json.append("    \"total_mods\": ").append(modCount).append("\n");
            json.append("  },\n");
            json.append("  \"seeds_by_mod\": {\n");
            boolean firstMod = true;
            for (var entry : seedsByMod.entrySet()) {
                if (!firstMod) json.append(",\n");
                firstMod = false;
                json.append("    \"").append(entry.getKey()).append("\": [\n");
                List<String> items = entry.getValue();
                for (int i = 0; i < items.size(); i++) {
                    json.append("      \"").append(items.get(i)).append("\"");
                    if (i < items.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("    ]");
            }
            json.append("\n  }\n}");
            Files.writeString(outFile, json.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Log.d("find-seeds", "Written to " + outFile.toAbsolutePath());
        } catch (IOException e) {
            Log.e("find-seeds", "Failed to write output file: " + e.getMessage());
        }
    }

    /**
     * /nutrition export — 导出所有可食用物品的 ID、翻译名和营养组归属。
     * 输出到 run/export-items.json。
     */
    private static int exportItems(CommandSourceStack src) {
        if (src.getServer() == null) {
            src.sendFailure(Component.literal("Must be used on a server"));
            return 0;
        }

        src.sendSuccess(() -> Component.literal("Exporting item nutrition data..."), true);

        var autogen = NutritionDataRegistry.autoGeneratedItemSource();
        var manualListener = NutritionDataRegistry.itemListener();
        Map<ResourceLocation, Set<String>> manualGroups = null;
        if (manualListener != null) {
            manualGroups = new LinkedHashMap<>();
            for (var gc : manualListener.getGroupConfigs().values()) {
                String groupName = gc.groups;
                for (var ie : gc.items) {
                    ResourceLocation id = ResourceLocation.tryParse(ie.item());
                    if (id != null) {
                        manualGroups.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(groupName);
                    }
                }
            }
        }

        record ItemExport(String id, String name, List<String> groups) {}
        List<ItemExport> exports = new ArrayList<>();
        int totalFood = 0;
        int withGroups = 0;

        for (Item item : BuiltInRegistries.ITEM) {
            FoodProperties food = item.getDefaultInstance().getFoodProperties(null);
            if (food == null) continue;
            totalFood++;

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) continue;

            // 收集所有 group 归属（autogen + manual）
            Set<String> groupNames = new LinkedHashSet<>();
            if (autogen != null) {
                var autogenMap = autogen.getAllGroups();
                Set<String> ag = autogenMap.get(itemId);
                if (ag != null) groupNames.addAll(ag);
            }
            if (manualGroups != null) {
                Set<String> mg = manualGroups.get(itemId);
                if (mg != null) groupNames.addAll(mg);
            }

            if (!groupNames.isEmpty()) withGroups++;

            String displayName = item.getDescription().getString();
            exports.add(new ItemExport(itemId.toString(), displayName,
                    new ArrayList<>(groupNames)));
        }

        // 写入 JSON
        final int finalTotalFood = totalFood;
        final int finalWithGroups = withGroups;
        try {
            Path outFile = src.getServer().getServerDirectory().resolve("export-items.json");
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"summary\": {\n");
            json.append("    \"total_food_items\": ").append(finalTotalFood).append(",\n");
            json.append("    \"items_with_groups\": ").append(finalWithGroups).append(",\n");
            json.append("    \"items_without_groups\": ").append(finalTotalFood - finalWithGroups).append("\n");
            json.append("  },\n");
            json.append("  \"items\": [\n");
            for (int i = 0; i < exports.size(); i++) {
                var e = exports.get(i);
                json.append("    {");
                json.append("\"id\": \"").append(e.id).append("\", ");
                json.append("\"name\": \"").append(escapeJson(e.name)).append("\", ");
                json.append("\"groups\": [");
                for (int j = 0; j < e.groups.size(); j++) {
                    json.append("\"").append(e.groups.get(j)).append("\"");
                    if (j < e.groups.size() - 1) json.append(", ");
                }
                json.append("]}");
                if (i < exports.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n}");
            Files.writeString(outFile, json.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            src.sendSuccess(() -> Component.literal(
                    "Exported " + finalTotalFood + " food items (" + finalWithGroups + " with groups) to "
                            + outFile.toAbsolutePath()), true);
        } catch (IOException e) {
            src.sendFailure(Component.literal("Failed to write export: " + e.getMessage()));
            return 0;
        }

        return Command.SINGLE_SUCCESS;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
