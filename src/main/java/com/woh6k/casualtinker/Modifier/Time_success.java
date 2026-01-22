package com.woh6k.casualtinker.Modifier;

import com.woh6k.casualtinker.Register.CasualtinkerModifier;

// --- Minecraft 原版类 ---

/*
 * 春秋必成 (Time_success) 强化模组
 * 为匠魂胸甲添加复活能力
 * 玩家死亡时有机会消耗经验复活，并获得短暂保护效果
 */
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

// --- Forge API ---
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// --- 匠魂 3 核心类 (参考你的模板) ---
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks; // 【重点】新的钩子注册表
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook; // 【重点】Tooltip 专用接口
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.List;

// 继承 Modifier 并实现 TooltipModifierHook 接口
public class Time_success extends Modifier implements TooltipModifierHook {

    // NBT 键名
    private static final ResourceLocation COOLDOWN_KEY = new ResourceLocation("casualtinker", "time_success_last_use");
    // 冷却时间：10分钟 (12000 ticks)
    private static final int COOLDOWN_TICKS = 12000;
    // 霉运时间：5分钟
    private static final int BAD_LUCK_TICKS = 6000;

    public Time_success() {
        super();
        // 注册 Forge 事件总线 (用于监听玩家死亡)
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ==========================================================
    // 1. 注册 Hook (参考你的模板)
    // ==========================================================
    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        // 【关键修复】使用 ModifierHooks.TOOLTIP 而不是 TinkerHooks
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    // ==========================================================
    // 2. 死亡事件逻辑 (这部分走 Forge 事件，不走 Hook)
    // ==========================================================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        // 必须是服务端玩家
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 必须穿在胸甲上
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.isEmpty()) {
            return;
        }

        ToolStack tool = ToolStack.from(chestStack);

        // 必须有此强化
        if (tool.getModifierLevel(CasualtinkerModifier.time_success.get()) <= 0) {
            return;
        }

        // --- NBT 读取 (String 兼容模式) ---
        long lastUseTime = 0;
        String timeStr = tool.getPersistentData().getString(COOLDOWN_KEY);
        if (!timeStr.isEmpty()) {
            try {
                lastUseTime = Long.parseLong(timeStr);
            } catch (NumberFormatException e) {
                lastUseTime = 0;
            }
        }

        long currentTime = player.level.getGameTime();
        // 检查冷却
        boolean isCooldownReady = (currentTime >= lastUseTime + COOLDOWN_TICKS);

        if (!isCooldownReady) {
            return; // 冷却中，不触发
        }

        // 检查损坏
        if (tool.isBroken()) {
            player.sendSystemMessage(Component.literal("§c[春秋必成] 宝甲已碎，无法护主！"));
            return;
        }

        // 检查等级代价
        if (player.experienceLevel < 100) {
            player.sendSystemMessage(Component.literal("§c[春秋必成] 修为不足百级，无法逆天改命！"));
            return;
        }

        // === 触发复活 ===
        event.setCanceled(true);

        // 恢复状态
        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

        // 支付代价
        player.giveExperienceLevels(-100);
        player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, BAD_LUCK_TICKS, 0));

        // NBT 写入 (记录时间)
        tool.getPersistentData().putString(COOLDOWN_KEY, String.valueOf(currentTime));

        // 播放特效
        player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        // 【新增代码】手动增加“使用不死图腾”的统计数据
        // 这样我们就能在成就里通过检测这个数据来判断次数了
        player.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));

        if (player.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    60, 0.5, 0.5, 0.5, 0.5);
        }

        player.sendSystemMessage(Component.literal("§6[春秋必成] 霉运: 5分钟"));
    }

    // ==========================================================
    // 3. Tooltip 实现 (来自 TooltipModifierHook 接口)
    // ==========================================================
    @Override
    public void addTooltip(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier, @Nullable Player player, @NotNull List<Component> tooltip, @NotNull TooltipKey tooltipKey, @NotNull TooltipFlag tooltipFlag) {
        // 读取 NBT
        long lastUseTime = 0;
        String timeStr = tool.getPersistentData().getString(COOLDOWN_KEY);
        if (!timeStr.isEmpty()) {
            try {
                lastUseTime = Long.parseLong(timeStr);
            } catch (NumberFormatException e) {
                lastUseTime = 0;
            }
        }

        long currentTime = (player != null) ? player.level.getGameTime() : 0;
        long timeLeft = (lastUseTime + COOLDOWN_TICKS) - currentTime;

        // 显示逻辑
        if (timeLeft > 0) {
            long totalSeconds = timeLeft / 20;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            tooltip.add(Component.literal("§c春秋必成:冷却中: " + minutes + "分" + seconds + "秒"));
        } else {
            if (tool.isBroken()) {
                tooltip.add(Component.literal("§4春秋必成:失效 (装备损坏)"));
            } else if (player != null && player.experienceLevel < 100) {
                tooltip.add(Component.literal("§e春秋必成:就绪 (经验不足)"));
            } else {
                tooltip.add(Component.literal("§a春秋必成:就绪 (需100级)"));
            }
        }
    }

    // ==========================================================
    // 4. 显示名称颜色
    // ==========================================================
    @Override
    public Component getDisplayName(int level) {
        // 金色 0xFFD700
        return Component.translatable(getTranslationKey())
                .withStyle(style -> style.withColor(TextColor.fromRgb(0xFFD700)));
    }
}