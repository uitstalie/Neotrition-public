package com.uitstalie.neotrition;

import com.uitstalie.neotrition.api.data.NutritionDataRegistry;
import com.uitstalie.neotrition.api.data.config.NutritionConfigProvider;
import com.uitstalie.neotrition.api.data.effect.NutritionEffectProvider;
import com.uitstalie.neotrition.api.data.group.NutritionGroupProvider;
import com.uitstalie.neotrition.api.data.item.NutritionItemProvider;
import com.uitstalie.neotrition.capabilities.NutritionCapabilities;
import com.uitstalie.neotrition.network.NetworkPayloadHandler;
import com.uitstalie.neotrition.server.NutritionCommand;
import com.uitstalie.neotrition.registry.AttributeTypeRegistry;
import com.uitstalie.neotrition.service.NutritionRuntimeService;
import com.uitstalie.neotrition.service.NutritionAutoGenerateService;
import com.uitstalie.neotrition.service.NutritionSyncService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Neotrition.MOD_ID)
public class Neotrition {

    public static final String MOD_ID = "neotrition";

    public Neotrition(IEventBus modEventBus, ModContainer modContainer) {
        NutritionDataRegistry.initialize();

        modEventBus.addListener(this::gatherData);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(this::addReloadListener);

        modEventBus.addListener(RegisterCapabilitiesEvent.class, NutritionCapabilities::attachCapabilityPlayer);
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, NetworkPayloadHandler::register);
        AttributeTypeRegistry.register(modEventBus);
    }

    /**
     * 注册 /neotrition 命令。
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        NutritionCommand.register(event.getDispatcher());
        // NutritionTestCommand.register(event.getDispatcher()); // hidden by default (v0.0.5), uncomment for testing
    }

    /**
     * Phase 5/6: 玩家登录时全量同步 + 立即触发 effect 重算。
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        NutritionRuntimeService.onPlayerLogin(player);
    }

    /**
     * 服务端启动完成后，若 mod 列表有变化则自动运行 autogen。
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        NutritionAutoGenerateService.autoGenerateIfNeeded(event.getServer());
        NutritionSyncService.invalidateItemEntryCache();
        NutritionSyncService.prewarmItemEntryCache();
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeServer(),
                new NutritionConfigProvider(event.getGenerator().getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper())
        );
        event.getGenerator().addProvider(
                event.includeServer(),
                new NutritionGroupProvider(event.getGenerator().getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper())
        );
        event.getGenerator().addProvider(
                event.includeServer(),
                new NutritionItemProvider(event.getGenerator().getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper())
        );
        event.getGenerator().addProvider(
                event.includeServer(),
                new NutritionEffectProvider(event.getGenerator().getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper())
        );
    }

    private void addReloadListener(AddReloadListenerEvent event) {
        NutritionDataRegistry.addReloadListeners(event);
    }
}
