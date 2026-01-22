package com.woh6k.casualtinker.Register;

import com.woh6k.casualtinker.Casualtinker;
import com.woh6k.casualtinker.Item.GuideBookItem;
import com.woh6k.casualtinker.Item.HolidayCoralItem;
import com.woh6k.casualtinker.Item.StartStoneItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Casualtinker.MODID);
    //注册引导书
    public static final RegistryObject<Item> GUIDE_BOOK = ITEMS.register("guide_book", () -> new GuideBookItem(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB).stacksTo(1)));
    //注册原初之石
    public static final RegistryObject<Item> START_STONE = ITEMS.register("start_stone", () -> new StartStoneItem(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB)));

    // 注册坚持仙蛊物品
    public static final RegistryObject<Item> PERSIST_GU = ITEMS.register("persist_gu", () -> new Item(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB).stacksTo(1)));
    // 注册慧剑仙蛊物品
    public static final RegistryObject<Item> WISDOM_SWORD_GU = ITEMS.register("wisdom_sword_gu", () -> new Item(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB).stacksTo(1)));
    // 注册态度仙蛊物品
    public static final RegistryObject<Item> ATTITUDE_GU = ITEMS.register("attitude_gu", () -> new Item(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB).stacksTo(1)));
    // 注册春秋蝉仙蛊物品
    public static final RegistryObject<Item> SPRING_AUTUMN_CICADA = ITEMS.register("spring_autumn_cicada", () -> new Item(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB).stacksTo(1)));


    // 注册茶叶物品
    public static final RegistryObject<Item> TEA_LEAF = ITEMS.register("tea_leaf", () -> new Item(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB)));
    // 注册凤血赤金锭物品
    public static final RegistryObject<Item> PHOENIX_GOLD_INGOT = ITEMS.register("phoenix_gold_ingot", () -> new Item(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB)));
    //注册龙纹黑金锭物品
    public static final RegistryObject<Item> DRAGON_GOLD_INGOT = ITEMS.register("dragon_gold_ingot", () -> new Item(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB)));
    //注册高松灯锭物品
    public static final RegistryObject<Item> TOMORI__INGOT = ITEMS.register("tomori_ingot", () -> new Item(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB)));

    //注册源石碎片
    public static final RegistryObject<Item> ORIGINIUM_SHARD = ITEMS.register("originium_shard", () -> new Item(new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB)));
    //注册假日珊瑚物品
    public static final RegistryObject<Item> HOLIDAY_CORAL = ITEMS.register("holiday_coral", () -> new HolidayCoralItem());

}