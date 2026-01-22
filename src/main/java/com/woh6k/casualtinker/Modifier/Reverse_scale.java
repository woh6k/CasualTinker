package com.woh6k.casualtinker.Modifier;

import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Reverse_scale extends Modifier implements OnAttackedModifierHook {

    // NBT 键名：记录上一次触发逆鳞的时间
    private static final ResourceLocation LAST_TRIGGER_TIME = new ResourceLocation("casualtinker", "reverse_scale_last_trigger");

    // 内置冷却时间：20 ticks = 1秒
    // 这意味着哪怕你在岩浆里或者被加特林打，1秒内也只会回一次血
    private static final int COOLDOWN = 20;

    public Reverse_scale() {
        super();
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }

    // --- 辅助方法：安全读写时间 ---
    private long getLastTriggerTime(IToolStackView tool) {
        String timeStr = tool.getPersistentData().getString(LAST_TRIGGER_TIME);
        if (timeStr.isEmpty()) return 0L;
        try { return Long.parseLong(timeStr); } catch (NumberFormatException e) { return 0L; }
    }

    private void setLastTriggerTime(IToolStackView tool, long time) {
        tool.getPersistentData().putString(LAST_TRIGGER_TIME, String.valueOf(time));
    }

    @Override
    public void onAttacked(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull EquipmentContext context, @NotNull EquipmentSlot slotType, @NotNull DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity entity = context.getEntity();
        if (entity.level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        if (slotType.getType() != EquipmentSlot.Type.ARMOR) {
            return;
        }

        // 1. 伤害阈值判定 (>10.0f)
        if (amount <= 10.0f) {
            return;
        }

        // 2. 冷却时间判定
        long currentTime = entity.level.getGameTime();
        long lastTrigger = getLastTriggerTime(tool);

        // 如果距离上次触发还不到 20 ticks (1秒)，直接忽略这次触发
        if (currentTime - lastTrigger < COOLDOWN) {
            return;
        }

        // =================================================================

        // 3. 先更新冷却时间！
        // 放在这里是为了防止极短时间内的多次伤害判定（比如同一 tick 内受到两次伤害）
        // 这样可以确保立刻锁住冷却
        setLastTriggerTime(tool, currentTime);

        // 4. 推迟执行回血逻辑
        player.level.getServer().execute(() -> {
            if (!player.isAlive()) {
                return;
            }

            int level = modifier.getLevel();

            // 回血
            player.heal(level * 2.0f);

            // 给 Buff
            int duration = 40 + (level * 10);
            // 这里稍微优化了逻辑：如果玩家已经有更强的效果，通常 addEffect 会自动处理，不用手动判定
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, false, true));
        });
    }

    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName(level).copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080)));
    }
}