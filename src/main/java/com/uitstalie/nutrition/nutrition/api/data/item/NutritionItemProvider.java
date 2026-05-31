package com.uitstalie.nutrition.nutrition.api.data.item;

import com.uitstalie.nutrition.nutrition.Nutrition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * DataGen Provider：生成 group-centric item JSON。
 * 输出到 {@code data/nutrition/items/}。
 */
public class NutritionItemProvider extends JsonCodecProvider<NutritionItemJson> {

    public NutritionItemProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, PackOutput.Target.DATA_PACK, "items", PackType.SERVER_DATA, NutritionItemJson.CODEC, lookupProvider, Nutrition.MOD_ID, existingFileHelper);
    }

    @Override
    protected void gather() {
        // ============================================================
        // proteins — 蛋白质（肉类）
        // ============================================================
        add("proteins", List.of(
                // vanilla
                e("minecraft:cooked_beef"), e("minecraft:cooked_chicken"),
                e("minecraft:cooked_porkchop"), e("minecraft:cooked_mutton"),
                e("minecraft:cooked_rabbit"), e("minecraft:beef"), e("minecraft:chicken"),
                e("minecraft:porkchop"), e("minecraft:mutton"), e("minecraft:rabbit"),
                e("minecraft:rotten_flesh"),
                // mod seeds
                e("croptopia:frog_legs"), e("croptopia:raw_ravager_meat"),
                e("farmersdelight:ham"),
                // alexsmobs
                e("alexsmobs:maggot"), e("alexsmobs:moose_ribs"),
                e("alexsmobs:kangaroo_meat"),                 e("alexsmobs:shrimp_fried_rice"),
                // croptopia
                e("croptopia:fried_frog_legs"),
                // exdeorum
                e("exdeorum:cooked_silkworm"),
                // earthmobsmod
                e("earthmobsmod:harder_flesh"),
                // crabbersdelight
                e("crabbersdelight:raw_frog_leg"), e("crabbersdelight:jar_of_pickles"),
                // spawn
                e("spawn:dodo"),
                // tsuki (processed, no FoodProperties)
                e("tsuki:minced_meat"), e("tsuki:hydra_ramen"),
                // minecraft (双开 proteins + trace_elements)
                e("minecraft:spider_eye")
        ));

        // ============================================================
        // eggs — 蛋类
        // ============================================================
        add("eggs", List.of(
                e("minecraft:egg"),
                e("farmersdelight:fried_egg"),
                e("alexsmobs:boiled_emu_egg"),
                // spawn (双开 proteins + eggs)
                e("spawn:dodo")
        ));

        // ============================================================
        // fishs — 鱼类 (100 tide + 28 aquaculture + 10 nether + 3 create + 9 croptopia + 6 vanilla)
        // ============================================================
        add("fishs", List.of(
                // vanilla
                e("minecraft:cod"), e("minecraft:cooked_cod"),
                e("minecraft:salmon"), e("minecraft:cooked_salmon"),
                e("minecraft:tropical_fish"), e("minecraft:pufferfish"),
                // tides (100)
                e("tide:rainbow_trout"), e("tide:brook_trout"), e("tide:largemouth_bass"),
                e("tide:smallmouth_bass"), e("tide:white_crappie"), e("tide:black_crappie"),
                e("tide:yellow_perch"), e("tide:carp"), e("tide:pike"), e("tide:guppy"),
                e("tide:bluegill"), e("tide:catfish"), e("tide:walleye"), e("tide:arapaima"),
                e("tide:mirage_catfish"), e("tide:sand_tiger_shark"), e("tide:slimy_salmon"),
                e("tide:frostbite_flounder"), e("tide:sturgeon"), e("tide:blossom_bass"),
                e("tide:spore_stalker"), e("tide:mooneye"), e("tide:bull_shark"),
                e("tide:ocean_perch"), e("tide:red_snapper"), e("tide:flounder"),
                e("tide:anchovy"), e("tide:tuna"), e("tide:mackerel"), e("tide:snook"),
                e("tide:angelfish"), e("tide:mahi_mahi"), e("tide:sailfish"),
                e("tide:swordfish"), e("tide:manta_ray"), e("tide:neptune_koi"),
                e("tide:pluto_snail"), e("tide:sun_emblem"), e("tide:saturn_cuttlefish"),
                e("tide:marstilus"), e("tide:uranias_pisces"), e("tide:great_white_shark"),
                e("tide:coelacanth"), e("tide:cave_eel"), e("tide:cave_crawler"),
                e("tide:deep_grouper"), e("tide:shadow_snapper"), e("tide:glowfish"),
                e("tide:anglerfish"), e("tide:abyss_angler"), e("tide:iron_tetra"),
                e("tide:lapis_lanternfish"), e("tide:crystal_shrimp"),
                e("tide:dripstone_darter"), e("tide:luminescent_jellyfish"),
                e("tide:crystalline_carp"), e("tide:gilded_minnow"), e("tide:bedrock_tetra"),
                e("tide:chasm_eel"), e("tide:echo_snapper"), e("tide:windbass"),
                e("tide:devils_hole_pupfish"), e("tide:midas_fish"),
                e("tide:incandescent_larva"), e("tide:bedrock_bug"), e("tide:sleepy_carp"),
                e("tide:blue_neonfish"), e("tide:judgment_fish"), e("tide:deep_blue"),
                e("tide:nephrosilu"), e("tide:vengeance"), e("tide:pentapus"),
                e("tide:darkness_eater"), e("tide:shadow_shark"), e("tide:alpha_fish"),
                e("tide:magma_mackerel"), e("tide:ember_koi"), e("tide:ash_perch"),
                e("tide:obsidian_pike"), e("tide:volcano_tuna"), e("tide:inferno_guppy"),
                e("tide:warped_guppy"), e("tide:crimson_fangjaw"), e("tide:soulscale"),
                e("tide:witherfin"), e("tide:blazing_swordfish"), e("tide:pale_clubfish"),
                e("tide:amber_rockfish"), e("tide:enderfin"), e("tide:chorus_cod"),
                e("tide:ender_glider"), e("tide:endergazer"), e("tide:violet_carp"),
                e("tide:red_40"), e("tide:dutchman_sock"), e("tide:elytrout"),
                e("tide:mantyvern"), e("tide:snatcher_squid"), e("tide:voidseeker"),
                e("tide:dragon_fish"),
                // aquaculture (28)
                e("aquaculture:algae"), e("aquaculture:atlantic_cod"), e("aquaculture:blackfish"),
                e("aquaculture:pacific_halibut"), e("aquaculture:atlantic_halibut"),
                e("aquaculture:atlantic_herring"), e("aquaculture:pink_salmon"),
                e("aquaculture:pollock"), e("aquaculture:rainbow_trout"),
                e("aquaculture:bayad"), e("aquaculture:boulti"), e("aquaculture:capitaine"),
                e("aquaculture:synodontis"), e("aquaculture:smallmouth_bass"),
                e("aquaculture:bluegill"), e("aquaculture:brown_trout"), e("aquaculture:carp"),
                e("aquaculture:catfish"), e("aquaculture:gar"), e("aquaculture:muskellunge"),
                e("aquaculture:perch"), e("aquaculture:arapaima"), e("aquaculture:piranha"),
                e("aquaculture:tambaqui"), e("aquaculture:brown_shrooma"),
                e("aquaculture:red_shrooma"), e("aquaculture:red_grouper"), e("aquaculture:tuna"),
                // netherdepthsupgrade (10)
                e("netherdepthsupgrade:lava_pufferfish"), e("netherdepthsupgrade:obsidianfish"),
                e("netherdepthsupgrade:searing_cod"), e("netherdepthsupgrade:bonefish"),
                e("netherdepthsupgrade:wither_bonefish"), e("netherdepthsupgrade:blazefish"),
                e("netherdepthsupgrade:magmacubefish"), e("netherdepthsupgrade:glowdine"),
                e("netherdepthsupgrade:fortress_grouper"), e("netherdepthsupgrade:eyeball_fish"),
                // createfisheryindustry (3)
                e("createfisheryindustry:fish_skin"), e("createfisheryindustry:raw_blue_mussel"),
                e("createfisheryindustry:raw_mediterranean_mussel"),
                // croptopia fish/seafood (9)
                e("croptopia:anchovy"), e("croptopia:calamari"), e("croptopia:clam"),
                e("croptopia:crab"), e("croptopia:glowing_calamari"), e("croptopia:oyster"),
                e("croptopia:roe"), e("croptopia:shrimp"), e("croptopia:tuna"),
                // crabbersdelight raw seafood mob-drops (7)
                e("crabbersdelight:crab"), e("crabbersdelight:clawster"),
                e("crabbersdelight:shrimp"), e("crabbersdelight:raw_clam_meat"),
                e("crabbersdelight:raw_squid_tentacles"), e("crabbersdelight:raw_glow_squid_tentacles"),
                e("crabbersdelight:crab_legs"),
                // alexsmobs fish mob-drops (4)
                e("alexsmobs:lobster_tail"), e("alexsmobs:raw_catfish"),
                e("alexsmobs:blobfish"), e("alexsmobs:flying_fish"),
                e("alexsmobs:cosmic_cod"), e("alexsmobs:rainbow_jelly"),
                // aquaculturedelight turtle
                e("aquaculturedelight:small_turtle_meat"),
                // tsuki fish/seafood
                e("tsuki:shrimp"), e("tsuki:bonito"),
                // spawn fish/seafood
                e("spawn:angler_fish"), e("spawn:herring"), e("spawn:bluefish"),
                e("spawn:tuna_chunk"), e("spawn:tuna_slice"), e("spawn:cooked_clam")
        ));

        // ============================================================
        // fruits — 水果
        // ============================================================
        add("fruits", List.of(
                // vanilla
                e("minecraft:apple"), e("minecraft:melon_slice"),
                e("minecraft:sweet_berries"), e("minecraft:glow_berries"),
                e("minecraft:chorus_fruit"), e("minecraft:golden_apple"),
                e("minecraft:enchanted_golden_apple"),
                // croptopia fruits (34)
                e("croptopia:apricot"), e("croptopia:avocado"), e("croptopia:banana"),
                e("croptopia:blackberry"), e("croptopia:blueberry"), e("croptopia:cantaloupe"),
                e("croptopia:cherry"), e("croptopia:coconut"), e("croptopia:cranberry"),
                e("croptopia:currant"), e("croptopia:date"), e("croptopia:dragonfruit"),
                e("croptopia:elderberry"), e("croptopia:fig"), e("croptopia:grape"),
                e("croptopia:grapefruit"), e("croptopia:honeydew"), e("croptopia:kiwi"),
                e("croptopia:kumquat"), e("croptopia:lemon"), e("croptopia:lime"),
                e("croptopia:mango"), e("croptopia:nectarine"), e("croptopia:orange"),
                e("croptopia:peach"), e("croptopia:pear"), e("croptopia:persimmon"),
                e("croptopia:pineapple"), e("croptopia:plum"), e("croptopia:raspberry"),
                e("croptopia:rhubarb"), e("croptopia:saguaro"), e("croptopia:starfruit"),
                e("croptopia:strawberry"),
                // alexsmobs
                e("alexsmobs:banana"),
                // silentgear
                e("silentgear:nether_banana"),
                // regions_unexplored
                e("regions_unexplored:salmonberry"), e("regions_unexplored:duskmelon_slice"),
                e("regions_unexplored:hanging_earlight_fruit"),
                // tsuki
                e("tsuki:grape"), e("tsuki:grape_green"), e("tsuki:ume"),
                e("tsuki:lemon"), e("tsuki:lime"),
                // tsuki drinks (no FoodProperties, derived from fruits)
                e("tsuki:lemon_juice"), e("tsuki:lime_juice"), e("tsuki:cola"),
                e("tsuki:blackcurrant_juice"), e("tsuki:orange_juice"),
                // monstergirl
                e("monstergirl:life_fruit"), e("monstergirl:soul_tether_berry"),
                e("monstergirl:dream_weave_berry"), e("monstergirl:halved_coconut"),
                // yeastnfeast
                e("yeastnfeast:elderberries"), e("yeastnfeast:rose_hips"),
                e("yeastnfeast:lemon"), e("yeastnfeast:hawthorn_berries"),
                // spawn
                e("spawn:dates"),
                // crabbersdelight
                e("crabbersdelight:coconut_halve")
        ));

        // ============================================================
        // grains — 谷物
        // ============================================================
        add("grains", List.of(
                // vanilla
                e("minecraft:bread"), e("minecraft:wheat"), e("minecraft:cake"),
                e("minecraft:cookie"), e("minecraft:potato"), e("minecraft:baked_potato"),
                e("minecraft:pumpkin_pie"),
                // croptopia grains
                e("croptopia:barley"), e("croptopia:corn"), e("croptopia:oat"),
                e("croptopia:rice"), e("croptopia:sweetpotato"),
                // tsuki (processed, no FoodProperties)
                e("tsuki:buckwheat_bread"), e("tsuki:raw_taiyaki"),
                e("tsuki:taiyaki"), e("tsuki:taiyaki_mocha"),
                // croptopia (processed, no FoodProperties)
                e("croptopia:tuna_sandwich"), e("croptopia:cheese_cake")
        ));

        // ============================================================
        // honeys — 蜂蜜
        // ============================================================
        add("honeys", List.of(
                e("minecraft:honey_bottle"), e("minecraft:honeycomb")
        ));

        // ============================================================
        // milks — 乳制品
        // ============================================================
        add("milks", List.of(
                // vanilla
                e("minecraft:milk_bucket"),
                // yeastnfeast cheeses
                e("yeastnfeast:cheese_slice"), e("yeastnfeast:duskwheel_slice"),
                e("yeastnfeast:freshwheel_slice"), e("yeastnfeast:sharpwheel_slice"),
                // monstergirl
                e("monstergirl:milk_bottle"),
                // croptopia (processed, no FoodProperties)
                e("croptopia:cheese_cake")
        ));

        // ============================================================
        // mushrooms — 菌类
        // ============================================================
        add("mushrooms", List.of(
                e("minecraft:red_mushroom"), e("minecraft:brown_mushroom"),
                e("minecraft:mushroom_stew"), e("minecraft:beetroot_soup"),
                e("minecraft:rabbit_stew"), e("minecraft:suspicious_stew"),
                // alexsmobs
                e("alexsmobs:gongylidia"),
                // tsuki
                e("tsuki:matsutake"), e("tsuki:edodes"), e("tsuki:shimeji")
        ));

        // ============================================================
        // nuts — 坚果
        // ============================================================
        add("nuts", List.of(
                // vanilla
                e("minecraft:cocoa_beans"),
                // croptopia nuts
                e("croptopia:almond"), e("croptopia:cashew"), e("croptopia:nutmeg"),
                e("croptopia:peanut"), e("croptopia:pecan"), e("croptopia:walnut"),
                // tsuki
                e("tsuki:almond"),
                e("tsuki:chestnut_toasted"),
                // spawn
                e("spawn:roasted_sunflower_seeds")
        ));

        // ============================================================
        // salt — 盐类
        // ============================================================
        add("salt", List.of(
                e("croptopia:sea_lettuce")
        ));

        // ============================================================
        // sugars — 糖类
        // ============================================================
        add("sugars", List.of(
                e("minecraft:sugar"), e("minecraft:sugar_cane"),
                e("yeastnfeast:maple_syrup"),
                e("create:bar_of_chocolate")
        ));

        // ============================================================
        // vegetables — 蔬菜
        // ============================================================
        add("vegetables", List.of(
                // vanilla
                e("minecraft:carrot"), e("minecraft:golden_carrot"),
                e("minecraft:potato"), e("minecraft:baked_potato"),
                e("minecraft:beetroot"), e("minecraft:dried_kelp"),
                e("minecraft:poisonous_potato"),
                // farmersdelight base veggies
                e("farmersdelight:cabbage"), e("farmersdelight:tomato"),
                e("farmersdelight:onion"), e("farmersdelight:pumpkin_slice"),
                // regions_unexplored
                e("regions_unexplored:meadow_sage"),
                // tsuki veggies
                e("tsuki:taro"), e("tsuki:tomato"), e("tsuki:radish"),
                e("tsuki:eggplant"), e("tsuki:cabbage"), e("tsuki:onion"),
                // yeastnfeast
                e("yeastnfeast:ginger"), e("yeastnfeast:garlic"),
                // croptopia vegetables (35)
                e("croptopia:artichoke"), e("croptopia:asparagus"), e("croptopia:basil"),
                e("croptopia:bellpepper"), e("croptopia:blackbean"), e("croptopia:broccoli"),
                e("croptopia:cabbage"), e("croptopia:cauliflower"), e("croptopia:celery"),
                e("croptopia:chile_pepper"), e("croptopia:corn"), e("croptopia:cucumber"),
                e("croptopia:eggplant"), e("croptopia:garlic"), e("croptopia:greenbean"),
                e("croptopia:greenonion"), e("croptopia:kale"), e("croptopia:leek"),
                e("croptopia:lettuce"), e("croptopia:olive"), e("croptopia:onion"),
                e("croptopia:peanut"), e("croptopia:radish"), e("croptopia:rhubarb"),
                e("croptopia:rutabaga"), e("croptopia:sea_lettuce"),
                e("croptopia:soybean"), e("croptopia:spinach"), e("croptopia:squash"),
                e("croptopia:sweetpotato"), e("croptopia:tomatillo"), e("croptopia:tomato"),
                e("croptopia:turnip"), e("croptopia:yam"), e("croptopia:zucchini")
        ));

        // ============================================================
        // coffee — 咖啡
        // ============================================================
        add("coffee", List.of(
                e("croptopia:coffee_beans"),
                e("silentgems:cup_of_coffee")
        ));

        // ============================================================
        // wines — 酒类
        // ============================================================
        add("wines", List.of(
                // no seeds found
        ));

        // ============================================================
        // trace_elements — 微量元素（矿物/金属类物品）
        // ============================================================
        add("trace_elements", List.of(
                // vanilla minerals & metals
                e("minecraft:iron_ingot"), e("minecraft:gold_ingot"),
                e("minecraft:copper_ingot"), e("minecraft:netherite_ingot"),
                e("minecraft:diamond"), e("minecraft:emerald"),
                e("minecraft:amethyst_shard"), e("minecraft:quartz"),
                e("minecraft:lapis_lazuli"), e("minecraft:redstone"),
                e("minecraft:coal"), e("minecraft:raw_iron"),
                e("minecraft:raw_gold"), e("minecraft:raw_copper"),
                // create — zinc & brass
                e("create:zinc_ingot"), e("create:raw_zinc"),
                e("create:brass_ingot"), e("create:andesite_alloy"),
                // silent gear — alloys
                e("silentgear:crimson_steel_ingot"), e("silentgear:crimson_iron_ingot"),
                e("silentgear:blaze_gold_ingot"), e("silentgear:azure_silver_ingot"),
                e("silentgear:azure_electrum_ingot"),
                // silent gems — gemstones
                e("silentgems:ruby"), e("silentgems:sapphire"),
                e("silentgems:topaz"), e("silentgems:opal"),
                e("silentgems:amber"), e("silentgems:carnelian"),
                // minecraft (双开 proteins + trace_elements)
                e("minecraft:spider_eye")
        ));
    }

    /** Shorthand for creating an item entry with no manual value (uses formula). */
    private static NutritionItemJson.ItemEntry e(String id) {
        return new NutritionItemJson.ItemEntry(id, null);
    }

    private void add(String group, List<NutritionItemJson.ItemEntry> items) {
        this.unconditional(
                ResourceLocation.fromNamespaceAndPath(Nutrition.MOD_ID, group),
                new NutritionItemJson(group, items)
        );
    }
}
