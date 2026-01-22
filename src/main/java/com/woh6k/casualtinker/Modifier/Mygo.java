package com.woh6k.casualtinker.Modifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

public class Mygo extends Modifier implements OnAttackedModifierHook, TooltipModifierHook {

    // NBT键名：上次触发时间
    private static final ResourceLocation LAST_TRIGGER = new ResourceLocation("casualtinker", "mygo_last_trigger");

    // 冷却时间：20秒 = 400 ticks
    private static final int COOLDOWN_TICKS = 400;
    // 持续时间：5秒 = 100 ticks
    private static final int DURATION_TICKS = 100;

    public Mygo() {
        super();
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    // --- 辅助方法 ---
    private long getLastTriggerTime(IToolStackView tool) {
        String timeStr = tool.getPersistentData().getString(LAST_TRIGGER);
        if (timeStr.isEmpty()) return 0L;
        try { return Long.parseLong(timeStr); } catch (NumberFormatException e) { return 0L; }
    }

    private void setLastTriggerTime(IToolStackView tool, long time) {
        tool.getPersistentData().putString(LAST_TRIGGER, String.valueOf(time));
    }

    // =================================================================================
    // 1. 受伤钩子
    // =================================================================================
    @Override
    public void onAttacked(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull EquipmentContext context, @NotNull EquipmentSlot slotType, @NotNull DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity entity = context.getEntity();
        if (entity.level.isClientSide || !(entity instanceof Player player)) return;

        if (slotType.getType() != EquipmentSlot.Type.ARMOR) return;

        // 1. 冷却检测 (20s)
        long currentTime = entity.level.getGameTime();
        long lastTrigger = getLastTriggerTime(tool);
        if (currentTime - lastTrigger < COOLDOWN_TICKS) {
            return;
        }

        // 2. 伤害阈值检测 (> 20% MaxHP)
        float maxHealth = entity.getMaxHealth();
        if (amount <= (maxHealth * 0.2f)) {
            return;
        }

        // === 触发逻辑 ===

        setLastTriggerTime(tool, currentTime);

        // 推迟执行以确保安全
        player.level.getServer().execute(() -> {
            if (!player.isAlive()) return;

            // 【核心修改】计算药水等级
            // 获取当前特性等级 (例如 1, 2, 3, 4...)
            int level = modifier.getLevel();

            // 限制最高为 3 (超过3按3算)
            int effectiveLevel = Math.min(level, 3);

            // 药水 Amplifier 从 0 开始计算：
            // 特性等级 1 -> 药水 0 (效果 I)
            // 特性等级 2 -> 药水 1 (效果 II)
            // 特性等级 3 -> 药水 2 (效果 III)
            int amplifier = effectiveLevel - 1;

            // 给予 速度、抗性、生命恢复 (对应等级)
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, amplifier));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, amplifier));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, amplifier));

            // 音效
            player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.5f, 1.2f);
        });
    }

    // =================================================================================
    // 2. Tooltip 钩子：显示冷却
    // =================================================================================
    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (player == null) return;

        long currentTime = player.level.getGameTime();
        long lastTrigger = getLastTriggerTime(tool);
        long timePassed = currentTime - lastTrigger;

        if (timePassed < COOLDOWN_TICKS) {
            long remainingTicks = COOLDOWN_TICKS - timePassed;
            long remainingSeconds = remainingTicks / 20;

            Component text = Component.translatable(this.getTranslationKey())
                    .append(": ")
                    .append(Component.literal("冷却中 (" + remainingSeconds + "s)").withStyle(ChatFormatting.RED));

            tooltip.add(text);
        } else {
            Component text = Component.translatable(this.getTranslationKey())
                    .append(": ")
                    .append(Component.literal("就绪").withStyle(ChatFormatting.GREEN));
            tooltip.add(text);
        }
    }
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName(level).copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x89CFF0)));
    }
}