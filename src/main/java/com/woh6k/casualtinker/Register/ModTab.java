package com.woh6k.casualtinker.Register;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModTab {
    public static final CreativeModeTab CASUAL_TINKER_TAB = new CreativeModeTab("casualtinker_tab") {
        @Override
        public ItemStack makeIcon() {
            // 使用茶叶作为创造模式物品栏的图标
            return new ItemStack(ModItem.TEA_LEAF.get());
        }
    };
}