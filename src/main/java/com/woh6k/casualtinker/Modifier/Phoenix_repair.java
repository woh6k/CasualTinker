package com.woh6k.casualtinker.Modifier;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Phoenix_repair extends Modifier implements InventoryTickModifierHook {
    public Phoenix_repair() {
        super();
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull Level world, @NotNull LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, @NotNull ItemStack stack) {
        // 1. 基础检查
        if (world.isClientSide || !(holder instanceof Player player)) {
            return;
        }

        // 2. 核心判定：玩家处于燃烧状态 (isOnFire) 或者 在岩浆里 (isInLava)
        // 只要满足其中一个，就视为“浴火”
        if (!player.isOnFire() && !player.isInLava()) {
            return;
        }

        // 3. 生效位置判定
        boolean shouldRepair = false;

        // 情况 A: 护甲 - 必须穿在身上
        if (isCorrectSlot) {
            shouldRepair = true;
        }
        // 情况 B: 工具 - 必须拿在主手
        else if (player.getMainHandItem() == stack) {
            shouldRepair = true;
        }

        // 4. 执行修复
        if (shouldRepair) {
            // 概率计算：每级 1.5%
            float chance = modifier.getLevel() * 0.015f;

            if (tool.getDamage() > 0 && world.random.nextFloat() < chance) {
                tool.setDamage(tool.getDamage() - 1);
            }
        }
    }
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName(level).copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8B0000)));
    }
}