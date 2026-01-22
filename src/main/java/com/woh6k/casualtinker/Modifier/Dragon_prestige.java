package com.woh6k.casualtinker.Modifier;

import com.woh6k.casualtinker.Casualtinker;
import com.woh6k.casualtinker.Register.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
// 【关键修复】正确的 TooltipKey 导入路径 (来自 Mantle)
import slimeknights.mantle.client.TooltipKey;

import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;

public class Dragon_prestige extends Modifier implements MeleeHitModifierHook, TooltipModifierHook {

    private static final ResourceLocation COOLDOWN_KEY = new ResourceLocation(Casualtinker.MODID, "dragon_prestige_cooldown");

    public Dragon_prestige() {
        super();
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    // ==========================================================
    // 逻辑 A：攻击触发
    // ==========================================================
    @Override
    public void afterMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @NotNull ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        Level level = attacker.level;

        if (level.isClientSide || !(attacker instanceof Player player)) {
            return;
        }

        // 1. 读取 NBT
        ModDataNBT persistentData = tool.getPersistentData();
        long gameTime = level.getGameTime();

        // 读取冷却 (String -> Long)
        String timeStr = persistentData.getString(COOLDOWN_KEY);
        long readyTime = 0;
        if (!timeStr.isEmpty()) {
            try {
                readyTime = Long.parseLong(timeStr);
            } catch (NumberFormatException e) {
                readyTime = 0;
            }
        }

        // 检查冷却
        if (gameTime < readyTime) {
            return;
        }

        // 2. 概率判定 (40%)
        if (level.random.nextFloat() >= 0.4f) {
            return;
        }

        // 3. 触发效果 (11秒)
        player.addEffect(new MobEffectInstance(ModEffects.COERCION.get(), 220, 0, false, false, true));

        // 4. 写入冷却 (60秒)
        persistentData.putString(COOLDOWN_KEY, String.valueOf(gameTime + 1200));
    }

    // ==========================================================
    // 逻辑 B：显示冷却时间 (Tooltip)
    // ==========================================================
    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @Nullable Player player, @NotNull List<Component> tooltip, @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        if (player == null) return;

        long gameTime = player.level.getGameTime();

        // 读取冷却
        ModDataNBT persistentData = tool.getPersistentData();
        String timeStr = persistentData.getString(COOLDOWN_KEY);
        long readyTime = 0;
        if (!timeStr.isEmpty()) {
            try {
                readyTime = Long.parseLong(timeStr);
            } catch (NumberFormatException e) {
                readyTime = 0;
            }
        }

        long remainingTicks = readyTime - gameTime;

        if (remainingTicks > 0) {
            int seconds = (int) (remainingTicks / 20);
            // 添加红色提示
            tooltip.add(Component.literal("威压冷却: " + seconds + "s").withStyle(ChatFormatting.RED));
        }
    }
    @Override
    public Component getDisplayName(int level) {
        return Component.translatable(getTranslationKey())
                .withStyle(style -> style.withColor(TextColor.fromRgb(0x696969)));
    }
}