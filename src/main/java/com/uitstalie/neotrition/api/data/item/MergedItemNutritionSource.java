package com.uitstalie.neotrition.api.data.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 按来源优先级合并物品营养绑定。先加入的 source 优先级更高。 */
public class MergedItemNutritionSource implements ItemNutritionSource {

    private final List<ItemNutritionSource> sources = new ArrayList<>();

    public void setSources(List<ItemNutritionSource> sources) {
        this.sources.clear();
        if (sources != null) {
            this.sources.addAll(sources.stream().filter(source -> source != null).toList());
        }
    }

    public void addSource(ItemNutritionSource source) {
        if (source != null) {
            sources.add(source);
        }
    }

    @Override
    public List<NutritionItemJson> getItems() {
        // groupName → merged NutritionItemJson
        Map<String, NutritionItemJson> merged = new LinkedHashMap<>();
        // 追踪每组中已存在的 itemId，避免被低优先级 source 覆盖
        Map<String, Set<String>> existingItems = new LinkedHashMap<>();

        for (ItemNutritionSource source : sources) {
            for (NutritionItemJson group : source.getItems()) {
                if (!group.isValid()) continue;
                String groupName = group.group;
                Set<String> seen = existingItems.computeIfAbsent(groupName, k -> new HashSet<>());

                if (!merged.containsKey(groupName)) {
                    // 首次遇到该组：保留全部 items
                    merged.put(groupName, new NutritionItemJson(groupName, new ArrayList<>(group.items)));
                    group.items.forEach(e -> seen.add(e.item()));
                } else {
                    // 后续 source：只追加未见过的 items
                    NutritionItemJson existing = merged.get(groupName);
                    for (NutritionItemJson.ItemEntry entry : group.items) {
                        if (!seen.contains(entry.item())) {
                            existing.items.add(entry);
                            seen.add(entry.item());
                        }
                    }
                }
            }
        }
        return new ArrayList<>(merged.values());
    }

    @Override
    public Set<String> getGroupsForItem(String itemId) {
        // 按优先级查询：第一个返回非空结果的 source 直接返回
        for (ItemNutritionSource source : sources) {
            Set<String> groups = source.getGroupsForItem(itemId);
            if (groups != null && !groups.isEmpty()) return groups;
        }
        return Set.of();
    }
}
