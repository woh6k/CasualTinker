package com.woh6k.casualtinker.Modifier;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
// 【关键新增】导入 ModifierNBT
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.List;
import java.util.Random;

import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;

/**
 * 茶能明目 (Teabright) 强化模组
 * 攻击时（近战或远程）有50%概率使周围10格内实体发光2秒
 */
public class Teabright extends Modifier implements MeleeDamageModifierHook, ProjectileHitModifierHook {
    private static final Random random = new Random();
    private static final double TRIGGER_CHANCE = 0.5; // 50%触发概率
    private static final double RADIUS = 10.0;
    private static final int MAX_ENTITIES = 100;
    private static final int GLOW_DURATION = 2 * 20;

    public Teabright() {
        super();
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.PROJECTILE_HIT);
    }

    // ==========================================================
    // 1. 近战触发逻辑
    // ==========================================================
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        tryTriggerEffect(context.getAttacker());
        return damage;
    }

    // ==========================================================
    // 2. 远程触发逻辑 (最终修正版)
    // ==========================================================
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        // 这里填入我们的逻辑
        if (attacker != null) {
            tryTriggerEffect(attacker);
        }
        // 返回 false 表示不拦截/不取消这次攻击
        return false;
    }

    // ==========================================================
    // 3. 核心逻辑 (保持不变)
    // ==========================================================
    private void tryTriggerEffect(LivingEntity centerEntity) {
        if (random.nextDouble() > TRIGGER_CHANCE) {
            return;
        }

        if (centerEntity.level instanceof ServerLevel serverLevel) {
            AABB bounds = centerEntity.getBoundingBox().inflate(RADIUS);
            List<Entity> entities = serverLevel.getEntities(null, bounds);

            if (entities.size() <= MAX_ENTITIES) {
                for (Entity entity : entities) {
                    if (entity instanceof LivingEntity livingEntity && !(livingEntity instanceof Player)) {
                        MobEffectInstance glowEffect = new MobEffectInstance(
                                MobEffects.GLOWING,
                                GLOW_DURATION,
                                0,
                                false,
                                true,
                                true
                        );
                        livingEntity.addEffect(glowEffect);
                    }
                }
            }
        }
    }

    @Override
    public Component getDisplayName(int level) {
        return Component.translatable(getTranslationKey())
                .withStyle(style -> style.withColor((TextColor.fromRgb(0x008000))));
    }
}