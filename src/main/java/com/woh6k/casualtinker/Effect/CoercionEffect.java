package com.woh6k.casualtinker.Effect;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class CoercionEffect extends MobEffect {

    public CoercionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 5秒 = 100 ticks 执行一次
        return duration % 100 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 仅服务端执行，且只有玩家能触发
        if (entity.level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        // 10格范围扫描 (和之前保持一致)
        AABB area = player.getBoundingBox().inflate(10.0);
        List<LivingEntity> targets = player.level.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            // 1. 排除自己和队友
            if (target == player || target.isAlliedTo(player)) {
                continue;
            }

            // 2. 【关键筛选】只针对 "Enemy" (敌对生物类)
            if (!(target instanceof Enemy)) {
                continue;
            }

            // 3. 【血量筛选】血量上限必须 < 20 (不包含20)
            if (target.getMaxHealth() >= 20.0f) {
                continue;
            }

            // 4. 【执行伤害】造成 40 点玩家物理伤害
            // 使用 playerAttack 确保被算作玩家击杀（会有掉落物）
            target.hurt(DamageSource.playerAttack(player), 40.0f);

        }
    }
}