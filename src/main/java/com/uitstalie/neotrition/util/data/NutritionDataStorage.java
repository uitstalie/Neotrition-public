package com.uitstalie.neotrition.util.data;

import com.uitstalie.neotrition.api.data.group.NutritionGroupJson;
import com.uitstalie.neotrition.util.log.Log;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个营养组的运行时状态。
 *
 * <h3>衰减语义</h3>
 * <ol>
 *   <li>每次 second event 回调时，decayCountdown 递减 1。</li>
 *   <li>归零时扣除 decayValue，重置 countdown = decayFrequency。</li>
 * </ol>
 */
class NutritionData {

    static final int MIN_VALUE = 0;
    static final int MAX_VALUE = 100_000;

    /** 当前营养值（0–100 000）。 */
    int value;
    /** 距下一次衰减还剩余几个 second event 回调。 */
    int decayCountdown;

    NutritionData() {
        this.value = 0;
        this.decayCountdown = 0;
    }

    /**
     * 增加营养值，上限 100 000。
     */
    int addNutrition(int perValue) {
        if (perValue <= 0) return 0;
        int before = value;
        value = Math.min(MAX_VALUE, value + perValue);
        dirty = true;
        return value - before;
    }

    /** 标记数据已脏，下次 sync 必须推送。 */
    boolean dirty;

    /**
     * 执行一次衰减推进。
     *
     * @param decayValue     每次扣减值
     * @param decayFrequency 衰减间隔（second event 回调次数）
     * @param decayPressure  压力指数：实际衰减 = decayValue × (1 + (value/max)^pressure)
     * @return true 表示本次发生了衰减扣减
     */
    boolean tickDecay(int decayValue, int decayFrequency, double decayPressure) {
        if (decayValue <= 0 || decayFrequency <= 0) return false;
        if (value <= 0) return false;

        // 首次初始化倒计时
        if (decayCountdown <= 0) {
            decayCountdown = decayFrequency;
            return false;
        }

        decayCountdown--;
        if (decayCountdown > 0) return false;

        // 压力系数：值越接近满值，衰减越重
        int effectiveDecay = decayValue;
        if (decayPressure > 0 && value > 0) {
            double ratio = (double) value / MAX_VALUE;
            double multiplier = 1.0 + Math.pow(ratio, decayPressure);
            effectiveDecay = (int) Math.round(decayValue * multiplier);
        }

        // 倒计时归零：扣除并重置
        value = Math.max(MIN_VALUE, value - effectiveDecay);
        decayCountdown = decayFrequency;
        return true;
    }

    // ───── NBT ─────

    void writeNBT(CompoundTag tag) {
        tag.putInt("value", value);
        tag.putInt("decayCountdown", decayCountdown);
    }

    void readNBT(CompoundTag tag) {
        value = Math.clamp(tag.getInt("value"), MIN_VALUE, MAX_VALUE);
        decayCountdown = tag.getInt("decayCountdown");
    }
}

/**
 * 玩家所有营养组的聚合存储，负责营养值的存取和批量衰减。
 *
 * <h3>线程模型</h3>
 * 仅在服务端 per-player second event 回调中单线程访问，无需额外同步。
 */
public class NutritionDataStorage {

    private static final String KEY_VERSION = "mod_version";

    /**
     * 生成当前数据包的版本指纹（来自 group 名称列表 + 各组 item 数量）。
     * 数据包变化后自动改变，无需手动维护。
     */
    public static String computeCurrentVersion() {
        try {
            var groups = com.uitstalie.neotrition.api.data.NutritionDataRegistry.groups();
            StringBuilder sb = new StringBuilder();
            for (var g : groups) {
                sb.append(g.groupName).append("|");
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private final Map<String, NutritionData> groups = new LinkedHashMap<>();

    // ────────── 食用 ──────────

    /**
     * 向指定营养组增加营养值。
     *
     * @param groupName 营养组名
     * @param perValue  增量
     * @return 实际增加值
     */
    public int addNutrition(String groupName, int perValue) {
        if (perValue <= 0) return 0;
        NutritionData data = groups.computeIfAbsent(groupName, k -> new NutritionData());
        return data.addNutrition(perValue);
    }

    /**
     * 直接设置指定营养组的营养值（用于命令/测试）。
     * 设置后清空衰减倒计时，避免设完立刻衰减。
     *
     * @param groupName 营养组名
     * @param value     目标值，自动截断到 [0, 100000]
     */
    public void setNutrition(String groupName, int value) {
        NutritionData data = groups.computeIfAbsent(groupName, k -> new NutritionData());
        data.value = Math.clamp(value, NutritionData.MIN_VALUE, NutritionData.MAX_VALUE);
        data.decayCountdown = 0;
        dirty = true;
    }

    // ────────── Dirty flag (P1) ──────────

    private boolean dirty;

    /** 营养数据是否自上次同步以来发生了变化。 */
    public boolean isDirty() { return dirty; }

    /** 清除脏标记（sync 后调用）。 */
    public void clearDirty() { dirty = false; }

    /**
     * 对所有营养组执行一次衰减。
     * 由 second event 回调直接调用。
     *
     * @param allGroups 所有已注册的营养组配置
     */
    public void tickDecay(List<NutritionGroupJson> allGroups) {
        if (allGroups == null || allGroups.isEmpty()) return;

        for (NutritionGroupJson group : allGroups) {
            if (group.groupName == null || group.groupName.isBlank()) continue;
            NutritionData data = groups.get(group.groupName);
            if (data == null) continue;
            if (data.tickDecay(group.decayValue, group.decayFrequency, group.decayPressure)) {
                dirty = true;
            }
        }
    }

    // ────────── 查询 ──────────

    public int getNutrition(String groupName) {
        NutritionData data = groups.get(groupName);
        return data != null ? data.value : 0;
    }

    /** 所有已有记录的营养组名（用于同步/GUI）。 */
    public List<String> getGroupNames() {
        return new ArrayList<>(groups.keySet());
    }

    /** 清空所有营养值（reload 时使用）。 */
    public void clear() {
        groups.clear();
    }

    // ────────── NBT ──────────

    public CompoundTag serializeNBT() {
        CompoundTag root = new CompoundTag();
        root.putString(KEY_VERSION, computeCurrentVersion());
        CompoundTag groupsTag = new CompoundTag();
        for (Map.Entry<String, NutritionData> entry : groups.entrySet()) {
            CompoundTag dataTag = new CompoundTag();
            entry.getValue().writeNBT(dataTag);
            groupsTag.put(entry.getKey(), dataTag);
        }
        root.put("groups", groupsTag);
        return root;
    }

    public void deserializeNBT(CompoundTag root) {
        String savedVersion = root.getString(KEY_VERSION);
        String currentVersion = computeCurrentVersion();
        if (!savedVersion.isEmpty() && !savedVersion.equals(currentVersion)) {
            Log.w("NutritionData", "Data pack changed since last save! "
                    + "Saved groups: [" + savedVersion + "] "
                    + "Current groups: [" + currentVersion + "] "
                    + "Nutrition data for removed groups will be orphaned.");
        }
        groups.clear();
        if (!root.contains("groups")) return;
        CompoundTag groupsTag = root.getCompound("groups");
        for (String key : groupsTag.getAllKeys()) {
            CompoundTag dataTag = groupsTag.getCompound(key);
            NutritionData data = new NutritionData();
            data.readNBT(dataTag);
            groups.put(key, data);
        }
    }
}
