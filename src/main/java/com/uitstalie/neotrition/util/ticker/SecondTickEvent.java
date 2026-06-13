package com.uitstalie.neotrition.util.ticker;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/**
 * 每秒触发一次的 tick 事件，携带当前 {@link MinecraftServer}。
 * 由 {@code NutritionRuntimeService} 的 tick 计数器每 20 个 server tick 投递一次。
 */
public class SecondTickEvent extends Event {

    private final MinecraftServer server;

    public SecondTickEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}