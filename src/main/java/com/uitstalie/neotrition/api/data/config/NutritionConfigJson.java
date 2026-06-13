package com.uitstalie.neotrition.api.data.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.uitstalie.neotrition.util.data.ValueRange;

import java.util.List;

/**
 * 全局配置文件（data/neotrition/config/config.json）。
 *
 * <h3>JSON 结构</h3>
 * <pre>{@code
 * {
 *   "frequency": "MEDIUM",
 *   "enable_food_record": true,
 *   "enable_chat_log": true,
 *   "debug": true,
 *   "value_formula": "healing * 2 + saturation * 0.5",
 *   "hunger_decay_multiplier": 1.0,
 *   "marginal_effect": {
 *     "enable": true,
 *     "window_minutes": 5,
 *     "rules": [
 *       { "below": 1, "above": 1, "multiplier": 1.0 }
 *     ]
 *   }
 * }
 * }</pre>
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li><b>frequency</b> — 统一计时器回调间隔：LOW(5s) / MEDIUM(3s) / HIGH(2s)，默认 MEDIUM。</li>
 *   <li><b>enable_food_record</b> — 是否启用食物历史记录，默认 true。</li>
 *   <li><b>enable_chat_log</b> — 是否向玩家发送聊天通知。需 debug=true 才生效，默认 false。</li>
 *   <li><b>enable_always_eat</b> — 饱食时是否仍可进食，默认 true。启用后即使饥饿值满也能继续吃食物。</li>
 *   <li><b>debug</b> — 调试模式主开关：控制 ChatLog + BFS 追溯记录。默认 true。</li>
 *   <li><b>value_formula</b> — 营养值折算公式。保留变量 {@code healing} (int) 和 {@code saturation} (float)。
 *       支持 {@code + - * / ( ) ^}。缺配启动崩溃。</li>
 *   <li><b>hunger_decay_multiplier</b> — 饥饿联动衰减倍率。玩家饥饿值每下降 1 点，额外扣除 {@code hunger_decay_multiplier × 衰减值} 的营养。
 *       设为 0 可禁用饥饿联动。默认 1.0。</li>
 *   <li><b>auto_generate_on_load</b> — 是否在服务器启动时自动运行 BFS 配方传播推断，默认 true。</li>
 *   <li><b>marginal_effect</b> — 边际效应配置。依赖 food_record，后者关闭时自动失效。</li>
 * </ul>
 *
 * @see NutritionConfigJson.MarginalEffect
 * @see NutritionConfigJson.Frequency
 */
public class NutritionConfigJson {

    /**
     * 统一计时器回调频率。
     * 定义全局 second event 的触发间隔。
     */
    public enum Frequency {
        LOW, MEDIUM, HIGH;

        /**
         * @return 对应的秒数间隔
         */
        public int toSeconds() {
            return switch (this) {
                case LOW -> 5;
                case MEDIUM -> 3;
                case HIGH -> 2;
            };
        }
    }

    public final Frequency frequency;
    public final boolean enableFoodRecord;
    public final boolean enableChatLog;
    public final boolean enableAlwaysEat;
    public final boolean debug;
    public final String valueFormula;
    public final double hungerDecayMultiplier;
    public final boolean autoGenerateOnLoad;
    public final MarginalEffect marginalEffect;

    public NutritionConfigJson(Frequency frequency,
                               boolean enableFoodRecord,
                               boolean enableChatLog,
                               boolean enableAlwaysEat,
                               boolean debug,
                               String valueFormula,
                               double hungerDecayMultiplier,
                               boolean autoGenerateOnLoad,
                               MarginalEffect marginalEffect) {
        this.frequency = frequency;
        this.enableFoodRecord = enableFoodRecord;
        this.enableChatLog = enableChatLog;
        this.enableAlwaysEat = enableAlwaysEat;
        this.debug = debug;
        this.valueFormula = valueFormula;
        this.hungerDecayMultiplier = hungerDecayMultiplier;
        this.autoGenerateOnLoad = autoGenerateOnLoad;
        this.marginalEffect = marginalEffect;
    }

    public static final Codec<NutritionConfigJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(Frequency::valueOf, Frequency::name)
                    .optionalFieldOf("frequency", Frequency.MEDIUM)
                    .forGetter(c -> c.frequency),
            Codec.BOOL.optionalFieldOf("enable_food_record", true)
                    .forGetter(c -> c.enableFoodRecord),
            Codec.BOOL.optionalFieldOf("enable_chat_log", false)
                    .forGetter(c -> c.enableChatLog),
            Codec.BOOL.optionalFieldOf("enable_always_eat", true)
                    .forGetter(c -> c.enableAlwaysEat),
            Codec.BOOL.optionalFieldOf("debug", true)
                    .forGetter(c -> c.debug),
            Codec.STRING.fieldOf("value_formula")
                    .forGetter(c -> c.valueFormula),
            Codec.DOUBLE.optionalFieldOf("hunger_decay_multiplier", 1.0)
                    .forGetter(c -> c.hungerDecayMultiplier),
            Codec.BOOL.optionalFieldOf("auto_generate_on_load", true)
                    .forGetter(c -> c.autoGenerateOnLoad),
            MarginalEffect.CODEC.optionalFieldOf("marginal_effect", MarginalEffect.DEFAULT)
                    .forGetter(c -> c.marginalEffect)
    ).apply(instance, NutritionConfigJson::new));

    /**
     * 边际效应配置。
     * 当同一食物在滑动窗口内被重复食用时，营养增量乘以对应 multiplier。
     * 依赖 food_record 开启。
     */
    public record MarginalEffect(boolean enable, int windowMinutes, List<Rule> rules) {

        /** 默认值：启用，5 分钟窗口，空规则表。 */
        public static final MarginalEffect DEFAULT = new MarginalEffect(true, 5, List.of());

        public static final Codec<MarginalEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("enable", true).forGetter(MarginalEffect::enable),
                Codec.INT.optionalFieldOf("window_minutes", 5).forGetter(MarginalEffect::windowMinutes),
                Codec.list(Rule.CODEC).optionalFieldOf("rules", List.of()).forGetter(MarginalEffect::rules)
        ).apply(instance, MarginalEffect::new));

        /**
         * 边际效应规则：count 落在 [below, above] 时，营养增量乘以 multiplier。
         * 区间按闭区间理解，重叠时 warn + 保留书写顺序第一个命中。
         */
        public record Rule(ValueRange range, double multiplier) {

            public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("below").forGetter(r -> r.range.below()),
                    Codec.INT.fieldOf("above").forGetter(r -> r.range.above()),
                    Codec.DOUBLE.optionalFieldOf("multiplier", 1.0).forGetter(Rule::multiplier)
            ).apply(instance, (below, above, multiplier) -> new Rule(new ValueRange(below, above), multiplier)));
        }

    }

    public boolean isFoodRecordEnabled() {
        return enableFoodRecord;
    }
}
