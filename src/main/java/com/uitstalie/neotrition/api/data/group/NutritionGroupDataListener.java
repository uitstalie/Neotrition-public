package com.uitstalie.neotrition.api.data.group;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.uitstalie.neotrition.util.data.ValueFormulaEvaluator;
import com.uitstalie.neotrition.util.log.Log;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 营养组加载器。
 * 监听 {@code data/neotrition/groups/} 目录，解析所有 {@code *.json} 文件。
 * 校验 group_name/group_icon 非空，decay 规则合法性。
 */
public class NutritionGroupDataListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Map<ResourceLocation, NutritionGroupJson> nutritionGroups = new LinkedHashMap<>();

    public NutritionGroupDataListener() {
        super(GSON, "groups");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        nutritionGroups.clear();

        map.forEach((key, json) -> {
            try {
                var parsed = NutritionGroupJson.CODEC.parse(JsonOps.INSTANCE, json);
                var result = parsed.getOrThrow(error -> {
                    Log.e("NutritionGroup", "Failed parsing group json: " + key + " — " + error);
                    return new RuntimeException(error);
                });

                if (!ModList.get().isLoaded(key.getNamespace())) {
                    return;
                }

                // Validate required fields
                if (result.groupName == null || result.groupName.isBlank()) {
                    Log.w("NutritionGroup", "Skipping group with empty group_name: " + key);
                    return;
                }
                if (result.groupIcon == null || result.groupIcon.isBlank()) {
                    Log.w("NutritionGroup", "Skipping group with empty group_icon: " + key);
                    return;
                }

                // Validate and fix decay config
                if (result.decayFrequency <= 0) {
                    Log.w("NutritionGroup", "Invalid decay_frequency in group " + key
                            + " (must be >= 1), defaulting to 1");
                    result = new NutritionGroupJson(result.groupName, result.groupIcon,
                            result.guiTextColor, result.guiPngColor,
                            result.decayValue, 1, result.decayPressure, result.valueFormula);
                }

                // Dry-run formula validation: catch syntax errors at load time
                if (result.valueFormula != null && !result.valueFormula.isBlank()) {
                    ValueFormulaEvaluator.evaluate(result.valueFormula, 1, 0.5f);
                }

                Log.d("NutritionGroup", "Loaded group: " + key + " name=" + result.groupName);
                nutritionGroups.put(key, result);
            } catch (Exception e) {
                Log.e("NutritionGroup", "Failed parsing group json: " + key + " — " + e.getMessage());
            }
        });
    }

    public Map<ResourceLocation, NutritionGroupJson> getNutritionGroupsWithResourceLocation() {
        return nutritionGroups;
    }

    public List<NutritionGroupJson> getNutritionGroups() {
        return nutritionGroups.values().stream().toList();
    }
}
