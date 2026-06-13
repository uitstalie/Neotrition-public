package com.uitstalie.neotrition.api.data.effect;

import com.uitstalie.neotrition.Neotrition;
import com.uitstalie.neotrition.util.data.ValueRange;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * DataGen Provider：生成默认 effect/attribute JSON。
 * 输出到 {@code data/neotrition/effects/}。
 */
public class NutritionEffectProvider extends JsonCodecProvider<NutritionEffectJson> {

    public NutritionEffectProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, PackOutput.Target.DATA_PACK, "effects", PackType.SERVER_DATA, NutritionEffectJson.CODEC, lookupProvider, Neotrition.MOD_ID, existingFileHelper);
    }

    @Override
    protected void gather() {
        /*
         * 14 组 Diet effects/attributes — 仅保留合并条目（全组统一阈值 25k/50k）
         * effect：每 6s 刷 8s duration
         * attribute：同周期移除旧 modifier 重建新 modifier
         */

        // === 血量合并：proteins/eggs/fishs/grains 四组任意达标 → +5 ===
        var healthOr25k = multiMatch4(NutritionEffectJson.Predict.OR,
                "proteins", 25000, "eggs", 25000, "fishs", 25000, "grains", 25000);
        // === 血量合并：四组全部达标 → +10 ===
        var healthAnd50k = multiMatch4(NutritionEffectJson.Predict.AND,
                "proteins", 50000, "eggs", 50000, "fishs", 50000, "grains", 50000);
        // === 护甲合并：wines/proteins/grains/vegetables/fruits 五组全部达标 → +5 ===
        var armorAnd50k = multiMatch5(NutritionEffectJson.Predict.AND,
                "wines", 50000, "proteins", 50000, "grains", 50000,
                "vegetables", 50000, "fruits", 50000);
        // === 重甲 + 坚韧：proteins/grains/vegetables/fruits/trace 五组全部达标 → +10 armor + 击退抗性 ===
        var heavyArmorAnd50k = multiMatch5(NutritionEffectJson.Predict.AND,
                "proteins", 50000, "grains", 50000, "vegetables", 50000,
                "fruits", 50000, "trace_elements", 50000);

        // === 独立 potion effects ===
        var fish25k = match("fishs", 25000);
        var vege50k = match("vegetables", 50000);
        // === 幸运儿：sugars/honeys/wines 任意达标 → +1 luck ===
        var luckOr20k = multiMatch3(NutritionEffectJson.Predict.OR,
                "sugars", 20000, "honeys", 20000, "wines", 20000);
        // === 矿工觉醒：nuts/salt/coffee 任意达标 → +1 haste ===
        var hasteOr25k = multiMatch3(NutritionEffectJson.Predict.OR,
                "nuts", 25000, "salt", 25000, "coffee", 25000);
        // === 咖啡因爆发：coffee → +1 speed ===
        var coffee25k = match("coffee", 25000);
        // === 自然自愈：mushrooms/eggs/milks 任意达标 → regeneration ===
        var regenOr20k = multiMatch3(NutritionEffectJson.Predict.OR,
                "mushrooms", 20000, "eggs", 20000, "milks", 20000);

        // === AND 组合：混合营养 → speed II ===
        var speedAnd50k = multiMatch4(NutritionEffectJson.Predict.AND,
                "grains", 50000, "fruits", 50000, "proteins", 50000, "vegetables", 50000);
        // === AND 组合：农家早餐 → saturation ===
        var satAnd50k = multiMatch3(NutritionEffectJson.Predict.AND,
                "eggs", 50000, "grains", 50000, "milks", 50000);
        // === AND 组合：幸运之神 → luck II ===
        var luck2And50k = multiMatch4(NutritionEffectJson.Predict.AND,
                "honeys", 50000, "sugars", 50000, "wines", 50000, "trace_elements", 50000);
        // === NOT 示例：核心营养全缺 → 虚弱 + 缓慢 ===
        var deficientNot25k = new NutritionEffectJson.Match(NutritionEffectJson.Predict.NOT, List.of(
                new NutritionEffectJson.Prediction("proteins", new ValueRange(25000, 100000)),
                new NutritionEffectJson.Prediction("fishs", new ValueRange(25000, 100000)),
                new NutritionEffectJson.Prediction("mushrooms", new ValueRange(25000, 100000))
        ));

        NutritionEffectJson def = new NutritionEffectJson(List.of(
                entryAttr(healthOr25k,     att("max_health", 5, ADD)),
                entryAttr(healthAnd50k,    att("max_health", 10, ADD)),
                entryAttr(armorAnd50k,     att("armor", 5, ADD)),
                entryAttrs(heavyArmorAnd50k,
                        att("armor", 10, ADD),
                        att("knockback_resistance", 0.5, ADD)),
                entry(fish25k, ef("water_breathing", 0)),
                entry(vege50k, ef("night_vision", 0)),
                entry(luckOr20k, ef("luck", 0)),
                entry(hasteOr25k, ef("haste", 0)),
                entry(coffee25k, ef("speed", 0)),
                entry(regenOr20k, ef("regeneration", 0)),
                entry(speedAnd50k, ef("speed", 1)),
                entry(satAnd50k, ef("saturation", 0)),
                entry(luck2And50k, ef("luck", 1)),
                // NOT 示例：核心营养全缺 → 虚弱 + 缓慢
                new NutritionEffectJson.CombinedEntry(deficientNot25k,
                        List.of(ef("weakness", 0), ef("slowness", 0)),
                        List.of())
        ));

        this.unconditional(
                ResourceLocation.fromNamespaceAndPath(Neotrition.MOD_ID, "default"),
                def
        );
    }

    /** Multi-group match: OR 或 AND 三组 */
    private static NutritionEffectJson.Match multiMatch3(NutritionEffectJson.Predict predict,
                                                          String g1, int b1, String g2, int b2,
                                                          String g3, int b3) {
        return new NutritionEffectJson.Match(predict, List.of(
                new NutritionEffectJson.Prediction(g1, new ValueRange(b1, 100000)),
                new NutritionEffectJson.Prediction(g2, new ValueRange(b2, 100000)),
                new NutritionEffectJson.Prediction(g3, new ValueRange(b3, 100000))
        ));
    }

    /** Multi-group match: OR 或 AND 四组 */
    private static NutritionEffectJson.Match multiMatch4(NutritionEffectJson.Predict predict,
                                                          String g1, int b1, String g2, int b2,
                                                          String g3, int b3, String g4, int b4) {
        return new NutritionEffectJson.Match(predict, List.of(
                new NutritionEffectJson.Prediction(g1, new ValueRange(b1, 100000)),
                new NutritionEffectJson.Prediction(g2, new ValueRange(b2, 100000)),
                new NutritionEffectJson.Prediction(g3, new ValueRange(b3, 100000)),
                new NutritionEffectJson.Prediction(g4, new ValueRange(b4, 100000))
        ));
    }

    /** Multi-group match: OR 或 AND 五组 */
    private static NutritionEffectJson.Match multiMatch5(NutritionEffectJson.Predict predict,
                                                          String g1, int b1, String g2, int b2,
                                                          String g3, int b3, String g4, int b4,
                                                          String g5, int b5) {
        return new NutritionEffectJson.Match(predict, List.of(
                new NutritionEffectJson.Prediction(g1, new ValueRange(b1, 100000)),
                new NutritionEffectJson.Prediction(g2, new ValueRange(b2, 100000)),
                new NutritionEffectJson.Prediction(g3, new ValueRange(b3, 100000)),
                new NutritionEffectJson.Prediction(g4, new ValueRange(b4, 100000)),
                new NutritionEffectJson.Prediction(g5, new ValueRange(b5, 100000))
        ));
    }

    /** Single-group match（单组阈值条件） */
    private static NutritionEffectJson.Match match(String group, int below) {
        return new NutritionEffectJson.Match(
                NutritionEffectJson.Predict.AND,
                List.of(new NutritionEffectJson.Prediction(group, new ValueRange(below, 100000)))
        );
    }

    private static NutritionEffectJson.EffectEntry ef(String name, int power) {
        return new NutritionEffectJson.EffectEntry("minecraft:" + name, power);
    }

    private static NutritionEffectJson.AttributeEntry att(String name, double amount,
                                                           NutritionEffectJson.Operation op) {
        return new NutritionEffectJson.AttributeEntry("minecraft:generic." + name, amount, op);
    }

    private static NutritionEffectJson.CombinedEntry entryAttr(
            NutritionEffectJson.Match m,
            NutritionEffectJson.AttributeEntry a) {
        return new NutritionEffectJson.CombinedEntry(m, List.of(), List.of(a));
    }

    private static NutritionEffectJson.CombinedEntry entryAttrs(
            NutritionEffectJson.Match m,
            NutritionEffectJson.AttributeEntry a1,
            NutritionEffectJson.AttributeEntry a2) {
        return new NutritionEffectJson.CombinedEntry(m, List.of(), List.of(a1, a2));
    }

    /** Shorthand: single effect, no attributes */
    private static NutritionEffectJson.CombinedEntry entry(
            NutritionEffectJson.Match m,
            NutritionEffectJson.EffectEntry e) {
        return new NutritionEffectJson.CombinedEntry(m, List.of(e), List.of());
    }

    private static final NutritionEffectJson.Operation ADD = NutritionEffectJson.Operation.ADD_VALUE;
}
