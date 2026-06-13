package com.uitstalie.neotrition.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.JsonOps;
import com.uitstalie.neotrition.api.data.NutritionDataRegistry;
import com.uitstalie.neotrition.api.data.config.NutritionConfigJson;
import com.uitstalie.neotrition.api.data.effect.NutritionEffectJson;
import com.uitstalie.neotrition.api.data.group.NutritionGroupJson;
import com.uitstalie.neotrition.api.data.item.NutritionItemJson;
import com.uitstalie.neotrition.capabilities.nutrition.NutritionCapability;
import com.uitstalie.neotrition.registry.AttributeTypeRegistry;
import com.uitstalie.neotrition.service.NutritionAutoGenerateService;
import com.uitstalie.neotrition.util.data.NutritionDataStorage;
import com.uitstalie.neotrition.util.data.ValueFormulaEvaluator;
import com.uitstalie.neotrition.util.data.ValueRange;
import com.uitstalie.neotrition.util.log.Log;
import com.google.gson.JsonParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * 营养系统集成测试命令：/neotrition test。
 *
 * <p>命令式集成测试，参考 NeoForge oldtest 模式。
 * 每项测试独立运行，通过/失败结果汇总输出。</p>
 */
public final class NutritionTestCommand {

    private NutritionTestCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("neotrition")
                        .then(Commands.literal("test")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> runAllTests(ctx.getSource()))));
    }

    // ── 测试入口 ────────────────────────────────────────────────────────

    private static int runAllTests(CommandSourceStack source) {
        List<TestResult> results = new ArrayList<>();

        results.add(testConfigLoaded());
        results.add(testGroupsLoaded());
        results.add(testItemsLoaded());
        results.add(testEffectsLoaded());
        results.add(testFormulaEvaluator());
        results.add(testValueRange());
        results.add(testNutritionApi(source));
        results.add(testAutogenOnServer(source));
        results.add(testEffectEvaluation());
        results.add(testDecayMechanics());
        results.add(testItemEntryCodec());
        results.add(testFormulaPower());
        results.add(testFormulaParentheses());
        results.add(testFormulaFloor());

        // 汇总
        int passed = 0;
        int failed = 0;
        StringBuilder sb = new StringBuilder("§6=== Nutrition Test Results ===\n");
        for (TestResult r : results) {
            if (r.passed) {
                passed++;
                sb.append("§a✔ ").append(r.name).append("\n");
            } else {
                failed++;
                sb.append("§c✘ ").append(r.name).append(" — ").append(r.error).append("\n");
            }
        }
        sb.append("§6=============================\n");
        sb.append(String.format("§e%d passed, §c%d failed, §f%d total",
                passed, failed, results.size()));

        source.sendSystemMessage(Component.literal(sb.toString()));
        Log.w("Test", String.format("Nutrition tests: %d passed, %d failed", passed, failed));
        return failed == 0 ? 1 : 0;
    }

    // ── 测试 1: 配置加载 ───────────────────────────────────────────────

    private static TestResult testConfigLoaded() {
        NutritionConfigJson config = NutritionDataRegistry.config();
        if (config == null) {
            return TestResult.fail("config", "NutritionDataRegistry.config() is null");
        }
        if (config.frequency == null) {
            return TestResult.fail("config", "frequency is null");
        }
        return TestResult.pass("config");
    }

    // ── 测试 2: 营养组加载 ─────────────────────────────────────────────

    private static TestResult testGroupsLoaded() {
        List<NutritionGroupJson> groups = NutritionDataRegistry.groups();
        if (groups == null || groups.isEmpty()) {
            return TestResult.fail("groups", "No groups loaded (0 groups)");
        }
        for (NutritionGroupJson g : groups) {
            if (g.groupName == null || g.groupName.isEmpty())
                return TestResult.fail("groups", "Group has empty name");
            if (g.decayValue <= 0)
                return TestResult.fail("groups", "Group '" + g.groupName + "' has non-positive decay");
        }
        return TestResult.pass("groups (" + groups.size() + " loaded)");
    }

    // ── 测试 3: 物品绑定加载 ───────────────────────────────────────────

    private static TestResult testItemsLoaded() {
        var items = NutritionDataRegistry.items();
        if (items == null || items.isEmpty()) {
            return TestResult.fail("items", "No item bindings loaded");
        }
        return TestResult.pass("items (" + items.size() + " items)");
    }

    // ── 测试 4: 效果规则加载 ───────────────────────────────────────────

    private static TestResult testEffectsLoaded() {
        List<NutritionEffectJson> effects = NutritionDataRegistry.effects();
        if (effects == null || effects.isEmpty()) {
            return TestResult.fail("effects", "No effect rules loaded");
        }
        for (NutritionEffectJson e : effects) {
            if (e.entries == null || e.entries.isEmpty())
                return TestResult.fail("effects", "An effect config has no entries");
        }
        return TestResult.pass("effects (" + effects.size() + " rules)");
    }

    // ── 测试 5: 公式求值 ───────────────────────────────────────────────

    private static TestResult testFormulaEvaluator() {
        int result = ValueFormulaEvaluator.evaluate("healing*100 + saturation*50", 4, 2.4f);
        if (result != 520) {
            return TestResult.fail("formula", "Expected 520, got " + result);
        }
        return TestResult.pass("formula (healing*100 + saturation*50 = " + result + ")");
    }

    // ── 测试 6: 区间规则 ───────────────────────────────────────────────

    private static TestResult testValueRange() {
        ValueRange range = new ValueRange(10, 50);
        if (range.contains(5)) return TestResult.fail("range", "5 should NOT match [10,50]");
        if (!range.contains(30)) return TestResult.fail("range", "30 should match [10,50]");
        if (range.contains(60)) return TestResult.fail("range", "60 should NOT match [10,50]");
        if (!range.contains(10)) return TestResult.fail("range", "10 should match [10,50]");
        return TestResult.pass("range [10,50] matching");
    }

    // ── 测试 7: 营养值 API ─────────────────────────────────────────────

    private static TestResult testNutritionApi(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayer();
            if (player == null) return TestResult.fail("nutrition-api", "No player available");

            NutritionCapability cap = player.getData(AttributeTypeRegistry.NutritionCapability.get());
            if (cap == null) return TestResult.fail("nutrition-api", "NutritionCapability is null");

            NutritionDataStorage data = cap.getNutritionData();
            if (data == null) return TestResult.fail("nutrition-api", "NutritionDataStorage is null");

            data.setNutrition("fruit", 50000);
            int val = data.getNutrition("fruit");
            if (val != 50000) return TestResult.fail("nutrition-api", "set/get mismatch: " + val);

            int unknown = data.getNutrition("nonexistent_group");
            if (unknown != 0) return TestResult.fail("nutrition-api", "Unknown group should be 0, got " + unknown);

            data.setNutrition("fruit", 0);
            return TestResult.pass("nutrition-api (set/get/unknown)");
        } catch (Exception e) {
            return TestResult.fail("nutrition-api", e.getMessage());
        }
    }

    // ── 测试 8: 自动生成 ───────────────────────────────────────────────

    private static TestResult testAutogenOnServer(CommandSourceStack source) {
        try {
            MinecraftServer server = source.getServer();
            if (server == null) return TestResult.fail("autogen", "No server available");

            var config = NutritionDataRegistry.config();
            if (config == null || !config.autoGenerateOnLoad)
                return TestResult.pass("autogen (disabled in config — skip)");

            long start = System.currentTimeMillis();
            NutritionAutoGenerateService.generate(server);
            long elapsed = System.currentTimeMillis() - start;

            int newItems = NutritionDataRegistry.autoGeneratedItemSource().getAllGroups().size();
            return TestResult.pass("autogen (" + newItems + " items in " + elapsed + "ms)");
        } catch (Exception e) {
            return TestResult.fail("autogen", e.getMessage());
        }
    }

    // ── 测试 9: 效果判定 ───────────────────────────────────────────────

    private static TestResult testEffectEvaluation() {
        try {
            NutritionDataStorage mockData = new NutritionDataStorage();
            mockData.setNutrition("fruit", 60);
            mockData.setNutrition("protein", 30);

            var andMatch = new NutritionEffectJson.Match(
                    NutritionEffectJson.Predict.AND,
                    List.of(
                            new NutritionEffectJson.Prediction("fruit",
                                    new ValueRange(50, 99999)),
                            new NutritionEffectJson.Prediction("protein",
                                    new ValueRange(20, 99999))
                    ));

            if (!andMatch.evaluate(mockData))
                return TestResult.fail("effect-and", "AND match should be true for fruit>=50, protein>=20");

            var orMatch = new NutritionEffectJson.Match(
                    NutritionEffectJson.Predict.OR,
                    List.of(
                            new NutritionEffectJson.Prediction("fruit",
                                    new ValueRange(90, 99999)),
                            new NutritionEffectJson.Prediction("protein",
                                    new ValueRange(20, 99999))
                    ));

            if (!orMatch.evaluate(mockData))
                return TestResult.fail("effect-or", "OR match should be true (protein>=20)");

            var notMatch = new NutritionEffectJson.Match(
                    NutritionEffectJson.Predict.NOT,
                    List.of(
                            new NutritionEffectJson.Prediction("fruit",
                                    new ValueRange(90, 99999))
                    ));

            if (!notMatch.evaluate(mockData))
                return TestResult.fail("effect-not", "NOT match should be true (fruit<90)");

            return TestResult.pass("effect-match (AND/OR/NOT)");
        } catch (Exception e) {
            return TestResult.fail("effect-eval", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ── 测试 10: 衰减机制 ──────────────────────────────────────────────

    private static TestResult testDecayMechanics() {
        try {
            List<NutritionGroupJson> groups = NutritionDataRegistry.groups();
            if (groups == null || groups.isEmpty()) return TestResult.fail("decay", "No groups");

            for (NutritionGroupJson g : groups) {
                if (g.decayValue < 0)
                    return TestResult.fail("decay", "Group '" + g.groupName + "' has negative decayValue: " + g.decayValue);
                if (g.decayFrequency <= 0)
                    return TestResult.fail("decay", "Group '" + g.groupName + "' has non-positive decayFrequency: " + g.decayFrequency);
                if (g.decayPressure < 0)
                    return TestResult.fail("decay", "Group '" + g.groupName + "' has negative decayPressure: " + g.decayPressure);
            }

            double value0 = 0;
            double pressure = 2.0;
            double decayAt0 = 1.0 * (1 + Math.pow(value0 / 100000.0, pressure));
            if (Math.abs(decayAt0 - 1.0) > 0.001)
                return TestResult.fail("decay", "Decay at value=0 should equal decayValue (1.0), got " + decayAt0);

            double value100k = 100000;
            double decayAt100k = 1.0 * (1 + Math.pow(value100k / 100000.0, pressure));
            if (Math.abs(decayAt100k - 2.0) > 0.001)
                return TestResult.fail("decay", "Decay at value=100k with pressure=2 should be 2x, got " + decayAt100k);

            return TestResult.pass("decay (" + groups.size() + " groups validated)");
        } catch (Exception e) {
            return TestResult.fail("decay", e.getMessage());
        }
    }

    // ── 测试 11: ItemEntry Codec ────────────────────────────────────────

    private static TestResult testItemEntryCodec() {
        try {
            // Parse string shorthand
            var json1 = JsonParser.parseString("\"minecraft:apple\"");
            var r1 = NutritionItemJson.ItemEntry.CODEC.parse(JsonOps.INSTANCE, json1);
            var entry1 = r1.getOrThrow(e -> new RuntimeException(e));
            if (!entry1.item().equals("minecraft:apple") || entry1.value() != null)
                return TestResult.fail("item-codec", "String parse failed: " + entry1);

            // Parse object without value
            var json2 = JsonParser.parseString("{\"item\":\"minecraft:apple\"}");
            var r2 = NutritionItemJson.ItemEntry.CODEC.parse(JsonOps.INSTANCE, json2);
            var entry2 = r2.getOrThrow(e -> new RuntimeException(e));
            if (!entry2.item().equals("minecraft:apple") || entry2.value() != null)
                return TestResult.fail("item-codec", "Object-no-value parse failed: " + entry2);

            // Parse object with value
            var json3 = JsonParser.parseString("{\"item\":\"minecraft:apple\",\"value\":3000}");
            var r3 = NutritionItemJson.ItemEntry.CODEC.parse(JsonOps.INSTANCE, json3);
            var entry3 = r3.getOrThrow(e -> new RuntimeException(e));
            if (!entry3.item().equals("minecraft:apple") || entry3.value() == null || entry3.value() != 3000)
                return TestResult.fail("item-codec", "Object-with-value parse failed: " + entry3);

            // Encode: no value → string
            var enc1 = NutritionItemJson.ItemEntry.CODEC.encodeStart(JsonOps.INSTANCE, entry1);
            if (!enc1.getOrThrow().getAsString().equals("minecraft:apple"))
                return TestResult.fail("item-codec", "Encode no-value should be string, got: " + enc1);

            // Encode: has value → object
            var enc3 = NutritionItemJson.ItemEntry.CODEC.encodeStart(JsonOps.INSTANCE, entry3);
            var obj3 = enc3.getOrThrow().getAsJsonObject();
            if (!obj3.get("item").getAsString().equals("minecraft:apple") || obj3.get("value").getAsInt() != 3000)
                return TestResult.fail("item-codec", "Encode with-value failed: " + obj3);

            return TestResult.pass("item-codec (3 formats parse + encode)");
        } catch (Exception e) {
            return TestResult.fail("item-codec", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ── 测试 12: 公式幂运算 ────────────────────────────────────────────

    private static TestResult testFormulaPower() {
        try {
            // Basic: 2^3 = 8
            int r1 = ValueFormulaEvaluator.evaluate("2 ^ 3", 0, 0);
            if (r1 != 8) return TestResult.fail("formula-power", "2^3 expected 8, got " + r1);

            // Right-associative: 2^3^2 = 2^9 = 512
            int r2 = ValueFormulaEvaluator.evaluate("2 ^ 3 ^ 2", 0, 0);
            if (r2 != 512) return TestResult.fail("formula-power", "2^3^2 expected 512, got " + r2);

            // Precedence: 2*3^2 = 2*9 = 18 (^ higher than *)
            int r3 = ValueFormulaEvaluator.evaluate("2 * 3 ^ 2", 0, 0);
            if (r3 != 18) return TestResult.fail("formula-power", "2*3^2 expected 18, got " + r3);

            // With variables
            int r4 = ValueFormulaEvaluator.evaluate("healing ^ 2", 5, 0);
            if (r4 != 25) return TestResult.fail("formula-power", "healing^2 expected 25, got " + r4);

            return TestResult.pass("formula-power (^ right-assoc + precedence)");
        } catch (Exception e) {
            return TestResult.fail("formula-power", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ── 测试 13: 公式括号 ──────────────────────────────────────────────

    private static TestResult testFormulaParentheses() {
        try {
            // (2+3)*4 = 20
            int r1 = ValueFormulaEvaluator.evaluate("(2+3)*4", 0, 0);
            if (r1 != 20) return TestResult.fail("formula-paren", "(2+3)*4 expected 20, got " + r1);

            // 2*(3+4) = 14
            int r2 = ValueFormulaEvaluator.evaluate("2*(3+4)", 0, 0);
            if (r2 != 14) return TestResult.fail("formula-paren", "2*(3+4) expected 14, got " + r2);

            // Nested: ((healing+1)^2) * saturation
            int r3 = ValueFormulaEvaluator.evaluate("((healing+1)^2) * saturation", 3, 2.0f);
            if (r3 != 32) return TestResult.fail("formula-paren", "((3+1)^2)*2 expected 32, got " + r3);

            return TestResult.pass("formula-paren (grouping + nesting)");
        } catch (Exception e) {
            return TestResult.fail("formula-paren", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ── 测试 14: 公式 floor 语义 ───────────────────────────────────────

    private static TestResult testFormulaFloor() {
        try {
            // Positive: 7 * 0.3 = 2.1 → floor = 2
            int r1 = ValueFormulaEvaluator.evaluate("healing * 0.3", 7, 0);
            if (r1 != 2) return TestResult.fail("formula-floor", "7*0.3 floor expected 2, got " + r1);

            // Negative fraction: 2 * 0.4 - 1 = -0.4 → floor = -1 (NOT truncation to 0)
            int r2 = ValueFormulaEvaluator.evaluate("healing * 0.4 - 1", 2, 0);
            if (r2 != -1) return TestResult.fail("formula-floor", "2*0.4-1=-0.4 floor expected -1, got " + r2);

            // Exact negative: -3.0 → floor = -3
            int r3 = ValueFormulaEvaluator.evaluate("healing - 10", 7, 0);
            if (r3 != -3) return TestResult.fail("formula-floor", "7-10=-3 floor expected -3, got " + r3);

            return TestResult.pass("formula-floor (positive/negative/exact)");
        } catch (Exception e) {
            return TestResult.fail("formula-floor", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private record TestResult(String name, boolean passed, String error) {
        static TestResult pass(String name) { return new TestResult(name, true, ""); }
        static TestResult fail(String name, String error) { return new TestResult(name, false, error); }
    }
}
