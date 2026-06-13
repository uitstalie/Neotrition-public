package com.uitstalie.neotrition.util.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedList;
import java.util.List;

/**
 * 食物进食记录滑动窗口（在线时间）。
 *
 * <h3>核心语义</h3>
 * <ul>
 *   <li>记录过去 {@code windowSeconds} 秒内的进食事件（按 item 存储）。</li>
 *   <li>时间推进使用在线时间：<b>不在本模块自行定义时间</b>，由外部 tick 驱动。</li>
 *   <li>同一 item 多次食用各自独立记录，不合并。</li>
 *   <li>过期方式：到期就删，不平滑衰减。</li>
 * </ul>
 *
 * <h3>线程模型</h3>
 * 仅在服务端 per-player second event 回调中单线程访问，无需额外同步。
 */
public class FoodRecordWindow {

    /** 每条进食记录：(itemId, timestamp seconds)。 */
    private final List<Record> records = new LinkedList<>();

    /** 玩家累计在线秒数。 */
    private int elapsedSeconds;

    /** 当前滑动窗口长度（秒），由 marginal_effect.window_minutes 决定。 */
    private int windowSeconds;

    public FoodRecordWindow() {
        this.elapsedSeconds = 0;
        this.windowSeconds = 300; // 5 minutes default
    }

    // ────────── 时间推进 ──────────

    /**
     * 推进在线时间并清理过期记录。
     *
     * @param seconds 自上次调用以来经过的秒数
     */
    public void tick(int seconds) {
        if (seconds <= 0) return;
        elapsedSeconds += seconds;
        removeExpired();
    }

    /**
     * 设置滑动窗口长度（秒）。
     */
    public void setWindowMinutes(int minutes) {
        this.windowSeconds = Math.max(1, minutes) * 60;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    // ────────── 记录 ──────────

    /**
     * 记录一次成功进食。
     *
     * @param itemId 物品资源定位符（如 minecraft:apple）
     */
    public void addRecord(String itemId) {
        if (itemId == null || itemId.isBlank()) return;
        records.add(new Record(itemId, elapsedSeconds));
        removeExpired();
    }

    /**
     * 查询某物品在窗口内的出现次数。
     */
    public int countItem(String itemId) {
        int count = 0;
        for (Record r : records) {
            if (r.itemId.equals(itemId)) count++;
        }
        return count;
    }

    /**
     * 清空所有记录（reload 或 disable 时使用）。
     */
    public void clear() {
        records.clear();
        elapsedSeconds = 0;
    }

    // ────────── 内部 ──────────

    /** 移除过期记录（时间戳早于 elapsedSeconds - windowSeconds）。 */
    private void removeExpired() {
        int cutoff = elapsedSeconds - windowSeconds;
        records.removeIf(r -> r.timestamp < cutoff);
    }

    // ────────── NBT ──────────

    public CompoundTag serializeNBT() {
        CompoundTag root = new CompoundTag();
        root.putInt("elapsedSeconds", elapsedSeconds);
        ListTag list = new ListTag();
        for (Record r : records) {
            CompoundTag entry = new CompoundTag();
            entry.putString("item", r.itemId);
            entry.putInt("t", r.timestamp);
            list.add(entry);
        }
        root.put("records", list);
        return root;
    }

    public void deserializeNBT(CompoundTag root) {
        records.clear();
        elapsedSeconds = root.getInt("elapsedSeconds");
        if (root.contains("records")) {
            ListTag list = root.getList("records", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String itemId = entry.getString("item");
                int ts = entry.getInt("t");
                records.add(new Record(itemId, ts));
            }
        }
    }

    // ────────── 内部类型 ──────────

    private record Record(String itemId, int timestamp) {}
}
