package com.uitstalie.neotrition.api.data.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.uitstalie.neotrition.util.data.ValueFormulaEvaluator;
import com.uitstalie.neotrition.util.log.Log;
import com.uitstalie.neotrition.service.NutritionRuntimeService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 全局配置加载器。
 * 监听 {@code data/neotrition/config/} 目录。
 *
 * <p>设计上只期望一个 {@code config.json}，因此 {@code apply()} 解析成功第一个文件后即返回，
 * 多余文件若发生同名冲突会被静默忽略。</p>
 */
public class NutritionConfigDataListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private NutritionConfigJson config;

    public NutritionConfigDataListener() {
        super(GSON, "config");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        for (var entry : map.entrySet()) {
            try {
                var parsed = NutritionConfigJson.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
                config = parsed.getOrThrow(error -> {
                    Log.e("NutritionConfig", "Failed parsing config: " + entry.getKey() + " — " + error);
                    return new RuntimeException(error);
                });

                // Dry-run formula validation: catch syntax errors at load time, not at first food consumption
                if (config.valueFormula != null && !config.valueFormula.isBlank()) {
                    ValueFormulaEvaluator.evaluate(config.valueFormula, 1, 0.5f);
                }

                Log.d("NutritionConfig", "Loaded config: " + entry.getKey());
                NutritionRuntimeService.refreshFrequency();
                return;
            } catch (Exception e) {
                Log.e("NutritionConfig", "Failed parsing config: " + entry.getKey() + " — " + e.getMessage());
            }
        }
    }

    public NutritionConfigJson getConfig() {
        return config;
    }
}
