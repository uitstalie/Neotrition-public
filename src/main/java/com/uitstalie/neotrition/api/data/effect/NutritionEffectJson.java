package com.uitstalie.neotrition.api.data.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.uitstalie.neotrition.util.data.NutritionDataStorage;
import com.uitstalie.neotrition.util.data.ValueRange;

import java.util.List;

/**
 * Effect 数据包配置（data/neotrition/effects/*.json）。
 *
 * <p>定义营养状态满足条件时，系统应给予的原版 effect 或 attribute 结果。
 * 采用条件维持型语义：条件命中时持续刷新，条件不命中时自然移除。</p>
 *
 * <h3>JSON 结构</h3>
 * <pre>{@code
 * {
 *   "entries": [
 *     {
 *       "match": {
 *         "predict": "AND",
 *         "prediction": [
 *           { "neotrition": "proteins", "below": 25000, "above": 100000 }
 *         ]
 *       },
 *       "effects": [
 *         { "name": "minecraft:strength", "power": 0 }
 *       ],
 *       "attributes": [
 *         { "name": "minecraft:generic.max_health", "amount": 2.0, "operation": "ADD_VALUE" }
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p>effects 和 attributes 均为可选（空数组或省略均可），match 命中时二者同时触发。</p>
 *
 * <h3>调度</h3>
 * <ul>
 *   <li>effect: 每 6s 刷新，duration 固定 8s。由统一计时器驱动。</li>
 *   <li>attribute: 同 refresh 周期，重建时先移除旧 modifier 再添加新 modifier。</li>
 * </ul>
 *
 * <h3>容错</h3>
 * <p>非法条目 warn + 跳过，不阻塞其他合法条目加载。</p>
 */
public class NutritionEffectJson {

    public final List<CombinedEntry> entries;

    public NutritionEffectJson(List<CombinedEntry> entries) {
        this.entries = entries;
    }

    public static final Codec<NutritionEffectJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(CombinedEntry.CODEC).fieldOf("entries").forGetter(c -> c.entries)
    ).apply(instance, NutritionEffectJson::new));

    /**
     * 合并条目：一个 match 条件 + 可选 effects + 可选 attributes。
     * 条件命中时，同时触发所有 effect 和 attribute。
     */
    public record CombinedEntry(Match match, List<EffectEntry> effects, List<AttributeEntry> attributes) {

        public static final Codec<CombinedEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Match.CODEC.fieldOf("match").forGetter(CombinedEntry::match),
                Codec.list(EffectEntry.CODEC).optionalFieldOf("effects", List.of()).forGetter(CombinedEntry::effects),
                Codec.list(AttributeEntry.CODEC).optionalFieldOf("attributes", List.of()).forGetter(CombinedEntry::attributes)
        ).apply(instance, CombinedEntry::new));

        public boolean hasEffects() { return effects != null && !effects.isEmpty(); }
        public boolean hasAttributes() { return attributes != null && !attributes.isEmpty(); }
    }

    /** 条件匹配结构。predict 定义 prediction 数组的匹配方式（AND/OR/NOT）。 */
    public record Match(Predict predict, List<Prediction> predictions) {

        public static final Codec<Match> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Predict.CODEC.fieldOf("predict").forGetter(Match::predict),
                Codec.list(Prediction.CODEC).fieldOf("prediction").forGetter(Match::predictions)
        ).apply(instance, Match::new));

        /**
         * 评估此 match 条件是否命中。
         *
         * @param data 当前玩家营养数据
         * @return true 表示条件命中
         */
        public boolean evaluate(NutritionDataStorage data) {
            if (!isValid() || data == null) return false;
            return switch (predict) {
                case AND -> predictions.stream().allMatch(p -> p.evaluate(data));
                case OR  -> predictions.stream().anyMatch(p -> p.evaluate(data));
                case NOT -> predictions.stream().noneMatch(p -> p.evaluate(data));
            };
        }

        public boolean isValid() {
            return predictions != null && !predictions.isEmpty();
        }
    }

    /** 条件组合方式：AND（全部命中）、OR（任一命中）、NOT（全不命中）。 */
    public enum Predict {
        AND, OR, NOT;

        public static final Codec<Predict> CODEC = Codec.STRING.xmap(
                s -> {
                    try {
                        return Predict.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid predict value: " + s + ". Must be AND, OR, or NOT.");
                    }
                },
                Predict::name
        );
    }

    /**
     * 叶子条件：当前 nutrition value 是否落在 [below, above] 闭区间内。
     */
    public record Prediction(String nutrition, ValueRange range) {

        public static final Codec<Prediction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("neotrition").forGetter(Prediction::nutrition),
                Codec.INT.fieldOf("below").forGetter(p -> p.range.below()),
                Codec.INT.fieldOf("above").forGetter(p -> p.range.above())
        ).apply(instance, (nutrition, below, above) -> new Prediction(nutrition, new ValueRange(below, above))));

        /**
         * 评估此叶子条件是否对给定营养数据命中。
         *
         * @param data 当前玩家营养数据
         * @return true 表示当前 nutrition 值落在 range 内
         */
        public boolean evaluate(NutritionDataStorage data) {
            if (!isValid() || data == null) return false;
            return range.contains(data.getNutrition(nutrition));
        }

        public boolean isValid() {
            return nutrition != null && !nutrition.isBlank()
                    && range.isValid();
        }
    }

    /**
     * Effect 条目：指定 effect id 和 amplifier。
     * 条件由外层 {@link CombinedEntry#match()} 统一管理。
     */
    public record EffectEntry(String name, int power) {

        public static final Codec<EffectEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(EffectEntry::name),
                Codec.INT.optionalFieldOf("power", 0).forGetter(EffectEntry::power)
        ).apply(instance, EffectEntry::new));

        public boolean isValid() {
            return name != null && !name.isBlank() && power >= 0;
        }
    }

    /** Attribute modifier 运算方式，对应原版 AttributeModifier.Operation。 */
    public enum Operation {
        ADD_VALUE,
        ADD_MULTIPLIED_BASE,
        ADD_MULTIPLIED_TOTAL;

        public static final Codec<Operation> CODEC = Codec.STRING.xmap(
                s -> Operation.valueOf(s.toUpperCase()),
                Operation::name
        );
    }

    /**
     * Attribute 条目：指定 attribute id、数值和运算方式。
     * 条件由外层 {@link CombinedEntry#match()} 统一管理。
     */
    public record AttributeEntry(String name, double amount, Operation operation) {

        public static final Codec<AttributeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(AttributeEntry::name),
                Codec.DOUBLE.fieldOf("amount").forGetter(AttributeEntry::amount),
                Operation.CODEC.fieldOf("operation").forGetter(AttributeEntry::operation)
        ).apply(instance, AttributeEntry::new));

        public boolean isValid() {
            return name != null && !name.isBlank() && operation != null;
        }
    }
}
