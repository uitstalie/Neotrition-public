package com.uitstalie.neotrition.capabilities.nutrition;

import com.uitstalie.neotrition.util.data.FoodRecordWindow;
import com.uitstalie.neotrition.util.data.NutritionDataStorage;
import com.uitstalie.neotrition.util.effect.ActiveAttributeState;
import com.uitstalie.neotrition.util.effect.ActiveEffectState;
import com.uitstalie.neotrition.util.effect.NutritionEffectApplier;
import com.uitstalie.neotrition.util.ticker.TimerState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * 单一玩家营养能力，聚合 nutrition data、food record、timer 等全部运行时数据。
 *
 * <h3>Phase 2</h3>
 * 新增 {@link TimerState} 用于统一 second event 定时器。
 *
 * <h3>Phase 3</h3>
 * 新增 {@link NutritionDataStorage}（营养值 + 衰减）和 {@link FoodRecordWindow}（滑动窗口食物记录）。
 *
 * <h3>Phase 4</h3>
 * 新增 {@link #effectRefreshCountdown} 用于驱动 effect/attribute 周期刷新。
 */
public class NutritionCapability implements INBTSerializable<CompoundTag> {
    private final TimerState timerState;
    private final NutritionDataStorage nutritionData;
    private final FoodRecordWindow foodRecord;

    /** Phase 4: effect/attribute 刷新倒计时（秒）。<= 0 时触发刷新。 */
    private int effectRefreshCountdown;

    /** 当前活跃的 effect 列表。在 refreshAll 中更新，由同步层转换为网络 DTO。 */
    private final List<ActiveEffectState> activeEffects = new ArrayList<>();

    /** 当前活跃的 attribute 列表。在 refreshAll 中更新，由同步层转换为网络 DTO。 */
    private final List<ActiveAttributeState> activeAttributes = new ArrayList<>();

    /**
     * 临时捕获：当前被食用的物品栈。
     * 由事件监听器设置，由 {@code FoodData.eat()} Mixin 消费后清空。
     * 不参与序列化（跨 tick 即可覆盖）。
     */
    private ItemStack capturedFood = ItemStack.EMPTY;

    /** 上次记录的玩家饥饿值，用于计算饥饿联动衰减。不持久化。 */
    private int lastFoodLevel = -1;

    public int getLastFoodLevel() { return lastFoodLevel; }
    public void setLastFoodLevel(int level) { this.lastFoodLevel = level; }

    public NutritionCapability() {
        this.timerState = new TimerState();
        this.nutritionData = new NutritionDataStorage();
        this.foodRecord = new FoodRecordWindow();
        this.effectRefreshCountdown = NutritionEffectApplier.REFRESH_INTERVAL_SECONDS;
    }

    public TimerState getTimerState() {
        return timerState;
    }

    public NutritionDataStorage getNutritionData() {
        return nutritionData;
    }

    public FoodRecordWindow getFoodRecord() {
        return foodRecord;
    }

    // ────────── Phase 4: Effect 刷新 ──────────

    /**
     * 每秒推进 effect 刷新倒计时。
     *
     * @param frequencySeconds 本次推进的秒数
     * @return true 表示应在此帧执行 effect/attribute 重算
     */
    public boolean tickEffectRefresh(int frequencySeconds) {
        if (frequencySeconds <= 0) return false;
        effectRefreshCountdown -= frequencySeconds;
        if (effectRefreshCountdown <= 0) {
            effectRefreshCountdown = NutritionEffectApplier.REFRESH_INTERVAL_SECONDS;
            return true;
        }
        return false;
    }

    public int getEffectRefreshCountdown() {
        return effectRefreshCountdown;
    }

    public List<ActiveEffectState> getActiveEffects() {
        return activeEffects;
    }

    public List<ActiveAttributeState> getActiveAttributes() {
        return activeAttributes;
    }

    // ────────── Captured Food (方块食物兼容) ──────────

    public void setCapturedFood(ItemStack stack) {
        this.capturedFood = stack.copy();
    }

    public ItemStack getCapturedFood() {
        return this.capturedFood;
    }

    public void clearCapturedFood() {
        this.capturedFood = ItemStack.EMPTY;
    }

    // ────────── NBT ──────────

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("timer", timerState.serializeNBT());
        tag.put("neotrition", nutritionData.serializeNBT());
        tag.put("foodRecord", foodRecord.serializeNBT());
        tag.putInt("effectRefresh", effectRefreshCountdown);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag compoundTag) {
        if (compoundTag.contains("timer")) {
            timerState.deserializeNBT(compoundTag.getCompound("timer"));
        }
        if (compoundTag.contains("neotrition")) {
            nutritionData.deserializeNBT(compoundTag.getCompound("neotrition"));
        }
        if (compoundTag.contains("foodRecord")) {
            foodRecord.deserializeNBT(compoundTag.getCompound("foodRecord"));
        }
        if (compoundTag.contains("effectRefresh")) {
            effectRefreshCountdown = compoundTag.getInt("effectRefresh");
        }
    }
}
