package com.uitstalie.neotrition.api.data.effect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.uitstalie.neotrition.util.log.Log;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Effect 配置加载器。
 * 监听 {@code data/neotrition/effects/} 目录，解析 effect/attribute 规则。
 * 跳过非法条目，不阻塞合法条目加载。
 */
public class NutritionEffectDataListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Map<ResourceLocation, NutritionEffectJson> effects = new LinkedHashMap<>();

    public NutritionEffectDataListener() {
        super(GSON, "effects");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        effects.clear();

        map.forEach((key, json) -> {
            try {
                var parsed = NutritionEffectJson.CODEC.parse(JsonOps.INSTANCE, json);
                var result = parsed.getOrThrow(error -> {
                    Log.e("NutritionEffect", "Failed parsing effect json: " + key + " — " + error);
                    return new RuntimeException(error);
                });

                // 过滤非法 effect/attribute 子条目（保留合法条目）
                var validEntries = result.entries.stream()
                        .map(ce -> {
                            var validEffects = ce.effects().stream()
                                    .filter(NutritionEffectJson.EffectEntry::isValid).toList();
                            var validAttrs = ce.attributes().stream()
                                    .filter(NutritionEffectJson.AttributeEntry::isValid).toList();
                            if (validEffects.isEmpty() && validAttrs.isEmpty()
                                    && !(ce.effects().isEmpty() && ce.attributes().isEmpty())) {
                                return null; // 有内容但全非法 → 跳过整条
                            }
                            return new NutritionEffectJson.CombinedEntry(ce.match(), validEffects, validAttrs);
                        })
                        .filter(ce -> ce != null)
                        .toList();
                NutritionEffectJson sanitized = new NutritionEffectJson(validEntries);

                Log.d("NutritionEffect", "Loaded effect file: " + key
                        + " entries=" + sanitized.entries.size());
                effects.put(key, sanitized);
            } catch (Exception e) {
                Log.e("NutritionEffect", "Failed parsing effect json: " + key + " — " + e.getMessage());
            }
        });
    }

    public Map<ResourceLocation, NutritionEffectJson> getEffectsWithResourceLocation() {
        return effects;
    }

    public List<NutritionEffectJson> getEffects() {
        return effects.values().stream().toList();
    }
}
