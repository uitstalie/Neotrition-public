package com.uitstalie.neotrition.api.data.config;

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
 * DataGen Provider：生成默认 config.json。
 * 输出到 {@code data/neotrition/config/config.json}。
 */
public class NutritionConfigProvider extends JsonCodecProvider<NutritionConfigJson> {

    public NutritionConfigProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, PackOutput.Target.DATA_PACK, "config", PackType.SERVER_DATA, NutritionConfigJson.CODEC, lookupProvider, Neotrition.MOD_ID, existingFileHelper);
    }

    @Override
    protected void gather() {
        var marginalEffect = new NutritionConfigJson.MarginalEffect(
                true, 5,
                List.of(
                        new NutritionConfigJson.MarginalEffect.Rule(new ValueRange(1, 1), 1.0),
                        new NutritionConfigJson.MarginalEffect.Rule(new ValueRange(2, 5), 0.8),
                        new NutritionConfigJson.MarginalEffect.Rule(new ValueRange(6, 10), 0.5)
                )
        );

        NutritionConfigJson defaultConfig = new NutritionConfigJson(
                NutritionConfigJson.Frequency.HIGH,
                true, false, true, true,
                "healing * 100 + saturation * 50",
                1.0,
                true,
                marginalEffect
        );

        this.unconditional(
                ResourceLocation.fromNamespaceAndPath(Neotrition.MOD_ID, "config"),
                defaultConfig
        );
    }
}
