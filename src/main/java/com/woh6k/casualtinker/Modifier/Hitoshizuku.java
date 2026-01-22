package com.woh6k.casualtinker.Modifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 特性：壱雫空 (Hitoshizuku)
 * 效果：
 * 根据【春日影】特性的触发次数增加伤害。
 * 每触发 2 次，伤害增加 1%。
 * 上限增加 400%。
 */
public class Hitoshizuku extends Modifier implements MeleeDamageModifierHook, TooltipModifierHook {

    private static final ResourceLocation TRIGGER_COUNT = new ResourceLocation("casualtinker", "haruhikage_trigger_count");

    public Hitoshizuku() {
        super();
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.MELEE_DAMAGE);
        builder.addHook(this, ModifierHooks.TOOLTIP);
    }

    private int getHaruhikageCount(IToolStackView tool) {
        return tool.getPersistentData().getInt(TRIGGER_COUNT);
    }

    // --- 【关键修复】直接计算整数百分比，避免浮点误差 ---
    private int getBonusPercentage(IToolStackView tool) {
        int count = getHaruhikageCount(tool);
        // 直接做整数除法，绝对精确
        // 30 / 2 = 15
        // 31 / 2 = 15
        // 32 / 2 = 16
        int percent = count / 2;

        // 锁死上限 400%
        return Math.min(percent, 400);
    }

    // =================================================================================
    // 1. 伤害钩子
    // =================================================================================
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (!tool.hasTag(TinkerTags.Items.MELEE)) {
            return damage;
        }

        int percent = getBonusPercentage(tool);

        if (percent > 0) {
            // 将整数百分比转为倍率 (16 -> 0.16)
            float bonusMultiplier = percent / 100.0f;
            return damage * (1.0f + bonusMultiplier);
        }

        return damage;
    }

    // =================================================================================
    // 2. Tooltip 钩子
    // =================================================================================
    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        int percent = getBonusPercentage(tool);

        if (percent > 0) {
            float baseDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE); // 获取武器攻击力（匠魂面板攻击力-拳头攻击力1点）

            float extraDamage = baseDamage * percent / 100.0f;

            // %.2f 保留两位小数，显示更精确
            String extraStr = String.format("%.2f", extraDamage);

            Component text = Component.translatable("stat.casualtinker.hitoshizuku_bonus")
                    .append(": ")
                    .append(Component.literal("+" + percent + "%").withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal(" (+" + extraStr + ")").withStyle(ChatFormatting.GRAY));

            tooltip.add(text);
        }
    }
    // --- 隐藏等级显示 ---
    @Override
    public Component getDisplayName(int level) {
        // 1. 获取纯名字 (去掉等级)
        // 2. 链式调用 .withStyle 设置颜色
        return Component.translatable(this.getTranslationKey())
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x89CFF0)));
    }
}

