package com.woh6k.casualtinker.Modifier;

import com.woh6k.casualtinker.Register.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

// 导入 Hook 相关类
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.mantle.client.TooltipKey;

import java.util.List;

// 1. 同时实现 "近战钩子" 和 "提示钩子"
public class Dragon_tooth extends Modifier implements MeleeHitModifierHook, TooltipModifierHook {

    public Dragon_tooth() {
        super();
    }

    @Override
    protected void registerHooks(Builder builder) {
        // 2. 注册两个钩子
        builder.addHook(this, ModifierHooks.MELEE_HIT);
        builder.addHook(this, ModifierHooks.TOOLTIP);
    }

    // --- 攻击逻辑 (保持不变) ---
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        int level = modifier.getLevel();
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();

        if (target != null && !target.level.isClientSide && !target.isDeadOrDying() && attacker != null) {
            if (attacker.getRandom().nextFloat() < 0.5f) {
                int effectiveLevel = Math.min(level, 4);
                int amplifier = effectiveLevel - 1;
                if (amplifier < 0) amplifier = 0;

                if (target.hasEffect(ModEffects.DRAGON_PIECES.get())) {
                    target.removeEffect(ModEffects.DRAGON_PIECES.get());
                }

                target.addEffect(new MobEffectInstance(ModEffects.DRAGON_PIECES.get(), 60, amplifier));
            }
        }
    }

    // --- 提示逻辑 (addInformation -> addTooltip) ---
    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        int level = modifier.getLevel();

        int cappedLevel = Math.min(level, 4);
        int percent;

        if (cappedLevel == 1) percent = 10;
        else if (cappedLevel == 2) percent = 20;
        else if (cappedLevel == 3) percent = 30;
        else percent = 50;
    }

    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName(level).copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080)));
    }
}