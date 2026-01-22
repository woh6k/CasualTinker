package com.woh6k.casualtinker.Modifier;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

public class Phoenix_blessing extends Modifier implements InventoryTickModifierHook {
    public Phoenix_blessing() {
        super();
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull Level world, @NotNull LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, @NotNull ItemStack stack) {
        if (world.isClientSide || !(holder instanceof Player player)) {
            return;
        }

        // 1. 严格限制：只有穿在身上才生效
        if (!isCorrectSlot) {
            return;
        }

        // 2. 核心判定：玩家处于燃烧状态 (isOnFire) 或者 在岩浆里 (isInLava)
        // 只要满足其中一个，就触发赐福
        if (player.isOnFire() || player.isInLava()) {

            // A. 防火逻辑 (持续给 Buff，防止受伤)
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, false, false, true));

            // B. 回血逻辑
            int level = modifier.getLevel();
            // 基础间隔 4秒(80 tick)，每级减少 1秒(20 tick)，最快 0.5秒(10 tick)
            int interval = Math.max(10, 80 - (level * 20));

            if (world.getGameTime() % interval == 0) {
                if (player.getHealth() < player.getMaxHealth()) {
                    player.heal(1.0f);
                }
            }
        }
    }
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName(level).copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8B0000)));
    }
}