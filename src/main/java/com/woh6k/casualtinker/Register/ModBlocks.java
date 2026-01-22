package com.woh6k.casualtinker.Register;

import com.woh6k.casualtinker.Casualtinker;
import com.woh6k.casualtinker.Register.ModItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    // 1. 创建方块注册器
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Casualtinker.MODID);

    // ========================================================================
    // 在这里添加你的方块
    // ========================================================================

    // 注册：原初之石矿
    // 使用 registerBlock 辅助方法，它会自动帮你注册对应的物品
    public static final RegistryObject<Block> START_STONE_ORE = registerBlock("start_stone_ore",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(4.5f, 3.0f) // 硬度
                    .requiresCorrectToolForDrops() // 需要工具
                    .sound(SoundType.DEEPSLATE))); // 声音


    // ========================================================================
    // 核心辅助方法 (请不要删除这两段代码)
    // ========================================================================

    // 方法1：注册方块，并自动调用注册物品的方法
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn); // 自动注册物品
        return toReturn;
    }

    // 方法2：注册对应的 BlockItem
    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        // 这里引用了 ModItems.ITEMS，确保你的 ModItems 类存在且没有报错
        ModItem.ITEMS.register(name, () -> new BlockItem(block.get(),
                new Item.Properties().tab(ModTab.CASUAL_TINKER_TAB))); // 使用你的创造模式栏
    }

    // 提供给主类调用的注册方法
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}