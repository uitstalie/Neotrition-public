package com.uitstalie.neotrition.api.data.group;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.uitstalie.neotrition.library.color.Color;
import com.uitstalie.neotrition.library.color.ColorUtils;
import com.uitstalie.neotrition.library.color.DefaultColor;
import org.jetbrains.annotations.Nullable;

/**
 * 营养组配置。
 *
 * <p>每个营养组定义一个独立的营养类别（如"蛋白质""维生素"）。</p>
 *
 * <h3>JSON 结构</h3>
 * <pre>{@code
 * {
 *   "group_name": "protein",
 *   "group_icon": "minecraft:beef",
 *   "gui_text_color": "#FFFFFFFF",
 *   "gui_png_color": "#FF0000FF",
 *   "decay_value": 5,
 *   "decay_frequency": 3,
 *   "decay_pressure": 2.0,
 *   "value_formula": "healing * 50"
 * }
 * }</pre>
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li><b>group_name</b> — 全局唯一标识，同时作为 tag 名称和翻译 key。空字符串代表无效。</li>
 *   <li><b>group_icon</b> — GUI 展示用的物品 ID（如 {@code minecraft:apple}）。</li>
 *   <li><b>gui_text_color</b> — GUI 文字颜色，十六进制 {@code #AARRGGBB}，默认白色。</li>
 *   <li><b>gui_png_color</b> — GUI 贴图颜色，默认白色。</li>
 *   <li><b>decay_value</b> — 每次衰减扣减值，默认 0（不衰减）。</li>
 *   <li><b>decay_frequency</b> — 衰减间隔（second event 回调次数）。1=每次扣，3=每 3 次扣。默认 1。</li>
 *   <li><b>decay_pressure</b> — 衰减压力指数。越接近满值衰减越重，公式: 有效衰减 = decay_value × (1 + (value/100k)^pressure)。0=无压力。默认 0。</li>
 *   <li><b>value_formula</b> — 该组独立公式，null 时走全局配置。</li>
 * </ul>
 */
public class NutritionGroupJson {

    /** 营养组全局唯一名称。 */
    public final String groupName;
    /** GUI 展示用的物品 ID。 */
    public final String groupIcon;
    /** GUI 文字颜色。 */
    public final Color guiTextColor;
    /** GUI 贴图颜色。 */
    public final Color guiPngColor;
    /** 每次衰减扣减值。 */
    public final int decayValue;
    /** 衰减间隔（second event 回调次数）。 */
    public final int decayFrequency;
    /** 衰减压力指数：值越接近满值，实际衰减越重。0=无压力。 */
    public final double decayPressure;
    /** 该组独立公式，null 时走全局配置。 */
    @Nullable
    public final String valueFormula;

    public NutritionGroupJson(String groupName,
                              String groupIcon,
                              Color guiTextColor,
                              Color guiPngColor,
                              int decayValue,
                              int decayFrequency,
                              double decayPressure,
                              @Nullable String valueFormula) {
        this.groupName = groupName;
        this.groupIcon = groupIcon;
        this.guiTextColor = guiTextColor;
        this.guiPngColor = guiPngColor;
        this.decayValue = decayValue;
        this.decayFrequency = decayFrequency;
        this.decayPressure = decayPressure;
        this.valueFormula = valueFormula;
    }

    public static final Codec<NutritionGroupJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group_name", "").forGetter(c -> c.groupName),
            Codec.STRING.optionalFieldOf("group_icon", "").forGetter(c -> c.groupIcon),
            ColorUtils.COLOR_CODEC.optionalFieldOf("gui_text_color", DefaultColor.WHITE).forGetter(c -> c.guiTextColor),
            ColorUtils.COLOR_CODEC.optionalFieldOf("gui_png_color", DefaultColor.WHITE).forGetter(c -> c.guiPngColor),
            Codec.INT.optionalFieldOf("decay_value", 0).forGetter(c -> c.decayValue),
            Codec.INT.optionalFieldOf("decay_frequency", 1).forGetter(c -> c.decayFrequency),
            Codec.DOUBLE.optionalFieldOf("decay_pressure", 0.0).forGetter(c -> c.decayPressure),
            Codec.STRING.optionalFieldOf("value_formula", "").forGetter(c -> c.valueFormula != null ? c.valueFormula : "")
    ).apply(instance, (groupName, groupIcon, guiTextColor, guiPngColor, decayValue, decayFrequency, decayPressure, valueFormula) ->
            new NutritionGroupJson(groupName, groupIcon, guiTextColor, guiPngColor,
                    decayValue, decayFrequency, decayPressure,
                    valueFormula.isEmpty() ? null : valueFormula)));
}
