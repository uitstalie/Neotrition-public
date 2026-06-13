package com.uitstalie.neotrition.api.data.item;

import java.util.List;
import java.util.Set;

/**
 * 物品营养绑定来源。
 *
 * <p>手写 datapack 与自动生成结果都通过该接口暴露，
 * 再由合并层决定优先级。</p>
 */
public interface ItemNutritionSource {

    /** 获取所有 group 级配置（用于统计/列举）。 */
    List<NutritionItemJson> getItems();

    /** 获取指定物品所属的营养组集合。未归属返回空集。 */
    Set<String> getGroupsForItem(String itemId);
}
