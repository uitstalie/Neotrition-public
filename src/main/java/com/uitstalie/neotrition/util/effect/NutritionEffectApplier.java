package com.uitstalie.neotrition.util.effect;

import com.uitstalie.neotrition.api.data.effect.NutritionEffectJson;
import com.uitstalie.neotrition.capabilities.nutrition.NutritionCapability;
import com.uitstalie.neotrition.util.data.NutritionDataStorage;
import com.uitstalie.neotrition.util.log.Log;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Effect / Attribute 运行时管理器。
 *
 * <h3>生命周期</h3>
 * <ul>
 *   <li>{@link #REFRESH_INTERVAL_SECONDS} 秒一次全量重算（由 per-player tick 驱动）</li>
 *   <li>effect：duration {@link #EFFECT_DURATION_TICKS} tick，条件命中时刷新，
 *       条件不命中时不主动移除，由原版自然过期</li>
 *   <li>attribute：条件命中时施加 modifier，不命中时主动移除，
 *       通过 {@link ResourceLocation} 实现来源隔离</li>
 * </ul>
 *
 * <h3>来源隔离</h3>
 * <p>attribute modifier 使用 {@code neotrition:attr/<filePath>/<index>} 格式的唯一 ID，
 * 移除时只移除自己施加的 modifier，不影响外部来源。</p>
 */
public final class NutritionEffectApplier {

    /** Effect 刷新间隔（秒）。 */
    public static final int REFRESH_INTERVAL_SECONDS = 6;
    /** Effect 持续时间（tick），略长于刷新间隔以保证不中断。 */
    private static final int EFFECT_DURATION_TICKS = 8 * 20; // 8s

    private NutritionEffectApplier() {
    }

    /**
     * 执行一次全量 effect/attribute 重算。
     * 由 {@code Nutrition.onSecondEvent} 在刷新周期触发时调用。
     *
     * @param player       目标玩家
     * @param cap          玩家营养能力
     * @param allEffects   所有已加载的 effect 配置文件（key = ResourceLocation）
     */
    public static void refreshAll(ServerPlayer player,
                                  NutritionCapability cap,
                                  Map<ResourceLocation, NutritionEffectJson> allEffects) {
        if (player == null || cap == null || allEffects == null || allEffects.isEmpty()) return;

        NutritionDataStorage nutritionData = cap.getNutritionData();
        List<ActiveEffectState> activeEffects = new ArrayList<>();
        List<ActiveAttributeState> activeAttributes = new ArrayList<>();

        for (var entry : allEffects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            NutritionEffectJson effectFile = entry.getValue();

            if (effectFile.entries == null || effectFile.entries.isEmpty()) continue;

            for (int entryIdx = 0; entryIdx < effectFile.entries.size(); entryIdx++) {
                NutritionEffectJson.CombinedEntry ce = effectFile.entries.get(entryIdx);
                if (ce.match() == null || !ce.match().isValid()) continue;

                if (!ce.match().evaluate(nutritionData)) {
                    // 条件不命中：移除该 entry 的所有 attribute（effect 不做处理）
                    if (ce.hasAttributes()) {
                        for (int attrIdx = 0; attrIdx < ce.attributes().size(); attrIdx++) {
                            NutritionEffectJson.AttributeEntry a = ce.attributes().get(attrIdx);
                            if (!a.isValid()) continue;
                            removeAttribute(player, a, attributeModifierId(fileId, entryIdx, attrIdx));
                        }
                    }
                    continue;
                }

                // 条件命中：施加 effects + attributes
                if (ce.hasEffects()) {
                    for (NutritionEffectJson.EffectEntry e : ce.effects()) {
                        if (!e.isValid()) continue;
                        applyEffect(player, e, fileId);
                        ResourceLocation effectId = ResourceLocation.tryParse(e.name());
                        if (effectId != null) {
                            activeEffects.add(new ActiveEffectState(effectId, e.power()));
                        }
                    }
                }

                if (ce.hasAttributes()) {
                    for (int attrIdx = 0; attrIdx < ce.attributes().size(); attrIdx++) {
                        NutritionEffectJson.AttributeEntry a = ce.attributes().get(attrIdx);
                        if (!a.isValid()) continue;
                        ResourceLocation modifierId = attributeModifierId(fileId, entryIdx, attrIdx);
                        applyAttribute(player, a, modifierId);
                        activeAttributes.add(new ActiveAttributeState(
                                ResourceLocation.tryParse(a.name()),
                                a.amount(),
                                a.operation().name()));
                    }
                }
            }
        }

        // 更新 capability 中活跃 effect 列表（用于网络同步）
        cap.getActiveEffects().clear();
        cap.getActiveEffects().addAll(activeEffects);

        // 更新 capability 中活跃 attribute 列表（用于网络同步）
        cap.getActiveAttributes().clear();
        cap.getActiveAttributes().addAll(activeAttributes);
    }

    // ────────── Effect ──────────

    private static void applyEffect(ServerPlayer player, NutritionEffectJson.EffectEntry e,
                                    ResourceLocation fileId) {
        ResourceLocation effectId = ResourceLocation.tryParse(e.name());
        if (effectId == null) {
            Log.w("NutritionEffect", "Invalid effect name: " + e.name() + " in " + fileId);
            return;
        }

        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null);
        if (effect == null) {
            Log.w("NutritionEffect", "Unknown effect: " + e.name() + " in " + fileId);
            return;
        }

        // 如果已有同 effect 且更高 amplifier，不覆盖
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null && existing.getAmplifier() > e.power()) return;

        player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS, e.power(),
                false,  // ambient
                true,   // visible
                true    // showIcon
        ));
    }

    // ────────── Attribute ──────────

    private static void applyAttribute(ServerPlayer player, NutritionEffectJson.AttributeEntry a,
                                       ResourceLocation modifierId) {
        ResourceLocation attrId = ResourceLocation.tryParse(a.name());
        if (attrId == null) {
            Log.w("NutritionEffect", "Invalid attribute name: " + a.name());
            return;
        }

        Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            Log.w("NutritionEffect", "Player missing attribute: " + a.name());
            return;
        }

        // 移除旧的同 ID modifier（本系统之前施加的）
        instance.removeModifier(modifierId);

        AttributeModifier.Operation op = switch (a.operation()) {
            case ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };

        instance.addPermanentModifier(new AttributeModifier(modifierId, a.amount(), op));
    }

    private static void removeAttribute(ServerPlayer player, NutritionEffectJson.AttributeEntry a,
                                        ResourceLocation modifierId) {
        ResourceLocation attrId = ResourceLocation.tryParse(a.name());
        if (attrId == null) return;

        Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
        if (attribute == null) return;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(modifierId);
        }
    }

    /**
     * 为 attribute modifier 生成唯一 ID，用于来源隔离。
     */
    private static ResourceLocation attributeModifierId(ResourceLocation fileId, int entryIndex, int attrIndex) {
        String path = "attr/" + fileId.getNamespace() + "/" + fileId.getPath() + "/" + entryIndex + "/" + attrIndex;
        return ResourceLocation.fromNamespaceAndPath("neotrition", path);
    }
}
