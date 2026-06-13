package com.uitstalie.neotrition.api.data.item;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 营养组物品绑定配置（data/neotrition/items/{group_name}.json）。
 *
 * <p>一文件管一组：声明组名，列出该组下所有物品，每项可附带手动营养值（覆盖公式）。
 * 不写 value 的物品走公式折算。Group 成员关系由此文件完全确定，不再依赖 item tag。</p>
 *
 * <h3>JSON 结构</h3>
 * <pre>{@code
 * {
 *   "groups": "fruit",
 *   "items": [
 *     { "item": "minecraft:apple", "value": 3000 },
 *     { "item": "minecraft:melon_slice" }
 *   ]
 * }
 * }</pre>
 */
public class NutritionItemJson {

    /** 营养组名（对应 groups/ 下的 group_name，JSON 键为 "groups"）。 */
    public final String group;
    /** 该组下的物品列表。 */
    public final List<ItemEntry> items;

    public NutritionItemJson(String group, List<ItemEntry> items) {
        this.group = group;
        this.items = items;
    }

    public static final Codec<NutritionItemJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("groups").forGetter(c -> c.group),
            Codec.list(ItemEntry.CODEC).fieldOf("items").forGetter(c -> c.items)
    ).apply(instance, NutritionItemJson::new));

    /**
     * 单个物品项。
     *
     * <p>支持三种 JSON 格式：</p>
     * <ul>
     *   <li>{@code "minecraft:apple"} — 字符串简写，无手动值，走公式</li>
     *   <li>{@code {"item": "minecraft:apple"}} — 对象，无手动值，走公式</li>
     *   <li>{@code {"item": "minecraft:apple", "value": 3000}} — 对象，有手动值</li>
     * </ul>
     *
     * @param item  物品 ID（如 minecraft:apple）
     * @param value 手动营养值，null 表示走公式
     */
    public record ItemEntry(String item, @Nullable Integer value) {

        /** 对象格式 Codec（item + 可选 value）。 */
        private static final Codec<ItemEntry> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("item").forGetter(ItemEntry::item),
                Codec.INT.optionalFieldOf("value").forGetter(e -> Optional.ofNullable(e.value))
        ).apply(instance, (item, valueOpt) -> new ItemEntry(item, valueOpt.orElse(null))));

        /** 字符串格式 Codec："minecraft:apple" → ItemEntry("minecraft:apple", null) */
        private static final Codec<ItemEntry> STRING_CODEC = Codec.STRING.xmap(
                s -> new ItemEntry(s, null), ItemEntry::item);

        /**
         * 统一 Codec：字符串简写或对象格式。
         * <p>解码时优先尝试字符串，失败回退对象。编码时无 value 输出字符串，有 value 输出对象。</p>
         */
        public static final Codec<ItemEntry> CODEC = Codec.either(STRING_CODEC, OBJECT_CODEC)
                .xmap(
                        either -> either.map(Function.identity(), Function.identity()),
                        entry -> entry.hasManualValue()
                                ? Either.right(entry)
                                : Either.left(entry)
                );

        public boolean hasManualValue() {
            return value != null;
        }
    }

    public boolean isValid() {
        return group != null && !group.isBlank()
                && items != null && !items.isEmpty();
    }
}
