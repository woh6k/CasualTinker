package com.woh6k.casualtinker.Modifier;

import com.woh6k.casualtinker.Register.CasualtinkerModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;

/**
 * 五指拳心剑 (Finger_sword) 强化模组
 * 玩家潜行右键可发射剑气
 * 冷却机制：8秒 (160 ticks)，使用 NBT 记录 + Tooltip 显示
 */
public class Finger_sword extends Modifier implements TooltipModifierHook {

    // 攻击距离
    private static final double RANGE = 20.0;

    // NBT键：记录上次使用时间
    private static final ResourceLocation LAST_USE_TIME = new ResourceLocation("casualtinker", "finger_sword_last_use");

    // 冷却时间：8秒 = 160 ticks
    private static final int COOLDOWN_TICKS = 160;

    public Finger_sword() {
        super();
        // 注册 Forge 事件监听 (用于右键判定)
        MinecraftForge.EVENT_BUS.register(this);
    }

    // 注册匠魂钩子 (用于 Tooltip)
    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ModifierHooks.TOOLTIP);
    }

    // --- 辅助方法：NBT 时间读写 ---
    private long getLastUseTime(IToolStackView tool) {
        String timeStr = tool.getPersistentData().getString(LAST_USE_TIME);
        if (timeStr.isEmpty()) return 0L;
        try { return Long.parseLong(timeStr); } catch (NumberFormatException e) { return 0L; }
    }

    private void setLastUseTime(IToolStackView tool, long time) {
        tool.getPersistentData().putString(LAST_USE_TIME, String.valueOf(time));
    }

    // =================================================================================
    // 1. 右键事件 (主要逻辑)
    // =================================================================================
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide || !event.getEntity().isShiftKeyDown() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        ToolStack tool = getHeldToolWithModifier(player);
        if (tool == null || tool.isBroken()) return;

        // 【修改点 A】检查冷却 (NBT 方式)
        long currentTime = player.level.getGameTime();
        long lastUse = getLastUseTime(tool);
        // 如果还没到冷却时间，直接退出
        if (currentTime - lastUse < COOLDOWN_TICKS) {
            return;
        }

        // 寻找目标
        LivingEntity target = getTargetEntity(player, RANGE);

        if (target != null) {
            // --- 消耗与伤害 ---
            int maxDurability = tool.getStats().getInt(ToolStats.DURABILITY);
            int durabilityCost = (int) Math.ceil(maxDurability * 0.20);
            if (durabilityCost < 1) durabilityCost = 1;

            float targetMaxHealth = target.getMaxHealth();
            float damage = (float) Math.ceil(targetMaxHealth * 0.20);
            if (damage < 1.0f) damage = 1.0f;

            DamageSource trueDamage = DamageSource.playerAttack(player).bypassArmor().bypassMagic().setProjectile();
            boolean hitSuccess = target.hurt(trueDamage, damage);

            if (hitSuccess) {
                ToolDamageUtil.damage(tool, durabilityCost, player, player.getMainHandItem());

                // 【修改点 B】设置冷却时间 (NBT 方式)
                setLastUseTime(tool, currentTime);

                // 音效与特效
                player.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.2f);
                player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.0f);

                if (player.level instanceof ServerLevel serverLevel) {
                    spawnSwordQiEffects(serverLevel, player, target);
                }

                player.swing(InteractionHand.MAIN_HAND);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    // =================================================================================
    // 2. Tooltip 显示 (实时倒计时)
    // =================================================================================
    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (player == null) return;

        long currentTime = player.level.getGameTime();
        long lastUse = getLastUseTime(tool);
        long timePassed = currentTime - lastUse;

        // 如果处于冷却中
        if (timePassed < COOLDOWN_TICKS) {
            long remainingTicks = COOLDOWN_TICKS - timePassed;
            long remainingSeconds = remainingTicks / 20; // 转换为秒

            // 显示格式： 五指拳心剑: 剑气充能中 (4s) - 红色
            Component text = Component.translatable(this.getTranslationKey())
                    .append(": ")
                    .append(Component.literal("剑气充能中 (" + remainingSeconds + "s)").withStyle(ChatFormatting.RED));

            tooltip.add(text);
        } else {
            // 冷却完毕 (可选显示) - 绿色
            Component text = Component.translatable(this.getTranslationKey())
                    .append(": ")
                    .append(Component.literal("剑气就绪").withStyle(ChatFormatting.GREEN));

            tooltip.add(text);
        }
    }

    // =================================================================================
    // 3. 特效与工具方法 (保持不变)
    // =================================================================================

    private void spawnSwordQiEffects(ServerLevel level, Player shooter, LivingEntity target) {
        Vec3 startPos = shooter.getEyePosition().add(0, -0.2, 0);
        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);

        Vec3 direction = targetPos.subtract(startPos).normalize();
        double distance = startPos.distanceTo(targetPos);

        // 剑气轨迹
        for (double d = 0; d < distance; d += 0.5) {
            Vec3 currentPos = startPos.add(direction.scale(d));
            level.sendParticles(ParticleTypes.END_ROD,
                    currentPos.x, currentPos.y, currentPos.z,
                    1, 0, 0, 0, 0);
        }

        // 命中爆发
        level.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z,
                50, 0.5, 0.5, 0.5, 0.5);

        level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                targetPos.x, targetPos.y, targetPos.z,
                20, 0.5, 0.5, 0.5, 0.1);

        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                targetPos.x, targetPos.y, targetPos.z,
                1, 0.1, 0.1, 0.1, 0.0);
    }

    private LivingEntity getTargetEntity(Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endVec = eyePos.add(lookVec.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D);

        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player, eyePos, endVec, searchBox,
                (entity) -> !entity.isSpectator() && entity instanceof LivingEntity && !(entity instanceof ArmorStand),
                range * range
        );

        if (hitResult != null && hitResult.getEntity() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return null;
    }

    private ToolStack getHeldToolWithModifier(Player player) {
        if (player.getMainHandItem().is(TinkerTags.Items.MODIFIABLE)) {
            ToolStack tool = ToolStack.from(player.getMainHandItem());
            if (tool.getModifierLevel(CasualtinkerModifier.finger_sword.get()) > 0) {
                return tool;
            }
        }
        return null;
    }

    @Override
    public Component getDisplayName(int level) {
        return Component.translatable(getTranslationKey())
                .withStyle(style -> style.withColor(TextColor.fromRgb(0x00FA9A)));
    }
}