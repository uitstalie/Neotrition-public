package com.uitstalie.neotrition.util.ticker;

import net.minecraft.nbt.CompoundTag;

import java.util.Random;

/**
 * 单个玩家的定时器状态，由 per-player tick 驱动。
 *
 * <h3>核心语义</h3>
 * <ul>
 *   <li>在线时间 — 仅玩家在线时推进，退出冻结，登录恢复。</li>
 *   <li>随机偏移 — 首次激活时分配随机 second 偏移，避免全服同时回调。</li>
 *   <li>频率驱动 — 按 {@code frequencySeconds} 间隔触发 second event 回调。</li>
 * </ul>
 *
 * <h3>NBT 持久化</h3>
 * 保存 {@code remainingSeconds} 和 {@code offsetAssigned}，确保登录后恢复定时器位置。
 */
public class TimerState {

    private static final Random RANDOM = new Random();

    /** 距下一次回调还剩余多少秒。零或负值表示本轮应触发。 */
    private int remainingSeconds;

    /** 每两次回调之间的秒数间隔，等于当前生效的 frequency。 */
    private int frequencySeconds;

    /** 是否已完成首次随机偏移分配。持久化后登录恢复时不重新随机。 */
    private boolean offsetAssigned;

    public TimerState() {
        this.remainingSeconds = 0;
        this.frequencySeconds = 3; // MEDIUM default
        this.offsetAssigned = false;
    }

    // ────────── 频率配置 ──────────

    /** 设置回调间隔。若首次设置，自动分配随机偏移。 */
    public void setFrequency(int seconds) {
        if (seconds <= 0) return;
        this.frequencySeconds = seconds;
        if (!offsetAssigned) {
            remainingSeconds = RANDOM.nextInt(seconds);
            offsetAssigned = true;
        }
    }

    public int getFrequency() {
        return frequencySeconds;
    }

    // ────────── 每 second 推进 ──────────

    /**
     * 推进 1 秒。
     *
     * @return true 表示本轮应触发 second event callback
     */
    public boolean tick() {
        if (!offsetAssigned) {
            // 防御：初始化时未调用 setFrequency，用默认频率补初始化
            remainingSeconds = RANDOM.nextInt(Math.max(1, frequencySeconds));
            offsetAssigned = true;
        }

        remainingSeconds--;

        if (remainingSeconds <= 0) {
            remainingSeconds = frequencySeconds;
            return true;
        }
        return false;
    }

    // ────────── NBT 序列化 ──────────

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("remainingSeconds", remainingSeconds);
        tag.putInt("frequencySeconds", frequencySeconds);
        tag.putBoolean("offsetAssigned", offsetAssigned);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.remainingSeconds = tag.getInt("remainingSeconds");
        this.frequencySeconds = tag.getInt("frequencySeconds");
        this.offsetAssigned = tag.getBoolean("offsetAssigned");
        if (this.frequencySeconds <= 0) {
            this.frequencySeconds = 3;
        }
    }
}
