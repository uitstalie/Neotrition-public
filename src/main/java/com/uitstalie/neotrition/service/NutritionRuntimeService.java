package com.uitstalie.neotrition.service;

import com.uitstalie.neotrition.Neotrition;
import com.uitstalie.neotrition.api.data.NutritionDataRegistry;
import com.uitstalie.neotrition.api.data.config.NutritionConfigJson;
import com.uitstalie.neotrition.capabilities.nutrition.NutritionCapability;
import com.uitstalie.neotrition.registry.AttributeTypeRegistry;
import com.uitstalie.neotrition.util.effect.NutritionEffectApplier;
import com.uitstalie.neotrition.util.ticker.SecondTickEvent;
import com.uitstalie.neotrition.util.ticker.TimerState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 玩家营养运行时应用服务：tick、衰减、effect 刷新和登录同步。 */
@EventBusSubscriber(modid = Neotrition.MOD_ID)
public final class NutritionRuntimeService {

    /** 缓存的配置频率（秒），volatile 保证 config reload 后立即生效。默认 MEDIUM=3s。 */
    private static volatile int cachedFrequency = 3;

    /** 全局 tick 计数器，模 20 到达一秒边界时投递 {@link SecondTickEvent}。 */
    private static int tickCounter;

    private NutritionRuntimeService() {}

    /**
     * 从 config 拉取最新频率并更新缓存。
     * 在 config reload 后调用，使所有玩家的下一个秒 tick 即生效。
     */
    public static void refreshFrequency() {
        NutritionConfigJson config = NutritionDataRegistry.config();
        cachedFrequency = config != null ? config.frequency.toSeconds() : 3;
    }

    // ── 秒级计时：ServerTickEvent → SecondTickEvent ──

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;
        NeoForge.EVENT_BUS.post(new SecondTickEvent(event.getServer()));
    }

    // ── 事件处理：监听 SecondTickEvent，驱动所有在线玩家的 timer ──

    @SubscribeEvent
    public static void onSecondTick(SecondTickEvent event) {
        int freq = cachedFrequency;
        if (freq <= 0) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            NutritionCapability cap = player.getData(AttributeTypeRegistry.NutritionCapability);
            if (cap == null) continue;

            // ── 饥饿联动衰减（每秒检测，独立于定时器回调）──
            int currentFood = player.getFoodData().getFoodLevel();
            int lastFood = cap.getLastFoodLevel();
            if (lastFood >= 0 && currentFood < lastFood) {
                int hungerLost = lastFood - currentFood;
                NutritionConfigJson config = NutritionDataRegistry.config();
                if (config != null && config.hungerDecayMultiplier > 0) {
                    double multiplier = config.hungerDecayMultiplier;
                    // 睡眠时衰减减半
                    if (player.isSleeping()) multiplier *= 0.5;
                    var groups = NutritionDataRegistry.groups();
                    cap.getNutritionData().applyHungerDecay(hungerLost, multiplier, groups);
                }
            }
            cap.setLastFoodLevel(currentFood);

            TimerState timer = cap.getTimerState();
            if (timer == null) continue;

            if (timer.getFrequency() != freq) {
                timer.setFrequency(freq);
            }

            if (!timer.tick()) continue;

            // ── Second event 触发：衰减 → food record → effect 刷新 → 同步 ──
            NutritionConfigJson config = NutritionDataRegistry.config();
            if (config == null) continue;

            boolean needSync = false;

            var groups = NutritionDataRegistry.groups();
            if (!groups.isEmpty()) {
                cap.getNutritionData().tickDecay(groups);
            }

            if (config.isFoodRecordEnabled()) {
                cap.getFoodRecord().tick(freq);
            }

            if (cap.tickEffectRefresh(freq)) {
                NutritionEffectApplier.refreshAll(player, cap, NutritionDataRegistry.effectsByLocation());
                needSync = true;
            }

            if (needSync || cap.getNutritionData().isDirty()) {
                NutritionSyncService.syncToClient(player, cap);
                cap.getNutritionData().clearDirty();
            }
        }
    }

    // ── 登录 ──

    public static void onPlayerLogin(ServerPlayer player) {
        NutritionCapability cap = player.getData(AttributeTypeRegistry.NutritionCapability);
        if (cap == null) return;

        cap.setLastFoodLevel(player.getFoodData().getFoodLevel());
        NutritionEffectApplier.refreshAll(player, cap, NutritionDataRegistry.effectsByLocation());
        NutritionSyncService.syncToClient(player, cap);
    }
}