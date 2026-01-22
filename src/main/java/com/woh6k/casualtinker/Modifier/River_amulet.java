package com.woh6k.casualtinker.Modifier;

import com.woh6k.casualtinker.Register.CasualtinkerModifier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;

/**
 * 逆流护身印 (River_amulet) 强化模组
 * 为匠魂胸甲添加弹射物反弹和伤害免疫功能
 * 穿戴者可以反弹所有弹射物并免疫弹射物伤害
 */
public class River_amulet extends Modifier {

    public River_amulet() {
        super();
        // 注册到 Forge 事件总线，这样才能监听下面的 projectile 事件
        MinecraftForge.EVENT_BUS.register(this);
    }

    // =========================================================
    // 事件 1：物理反弹 (ProjectileImpactEvent)
    // =========================================================

    // 【注意】这里没有 @Override，因为它监听的是外部事件
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onProjectileImpact(ProjectileImpactEvent event) {
        // 1. 判定是否击中了实体
        if (event.getRayTraceResult().getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) event.getRayTraceResult();

            // 2. 判定被击中的是不是生物
            if (entityHit.getEntity() instanceof LivingEntity target) {
                // 3. 判定该生物是否拥有 river_amulet 强化
                if (hasRiverAmulet(target)) {
                    Projectile projectile = event.getProjectile();

                    // --- 反弹逻辑 ---
                    Vec3 motion = projectile.getDeltaMovement();
                    // 速度取反，产生反弹效果
                    projectile.setDeltaMovement(motion.scale(-1.0));

                    // 视觉旋转 (箭头调头)
                    projectile.setYRot(projectile.getYRot() + 180.0F);
                    projectile.xRotO += 180.0F;

                    // 更改归属权（反弹杀怪算玩家的）
                    projectile.setOwner(target);

                    // --- 【新增特效】生成护体水环 ---
                    spawnWaterRingEffect(target);

                    // 4. 【核心】取消这次碰撞事件，让箭飞走而不是插在身上
                    event.setCanceled(true);
                }
            }
        }
    }

    // =========================================================
    // 事件 2：伤害免疫 (LivingHurtEvent)
    // =========================================================

    // 【注意】这里也没有 @Override
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHurt(LivingHurtEvent event) {
        // 如果伤害来源是弹射物，且受害者有强化
        if (event.getSource().isProjectile() && hasRiverAmulet(event.getEntity())) {
            // 【核心】直接取消伤害，实现“护身”效果
            event.setCanceled(true);
            event.setAmount(0f);
        }
    }

    // =========================================================
    // 辅助方法：检查强化
    // =========================================================
    private boolean hasRiverAmulet(LivingEntity entity) {
        // 1. 简单检查：胸甲槽位有没有东西
        if (entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            return false;
        }

        // 2. 解析匠魂数据
        ToolStack chestArmor = ToolStack.from(entity.getItemBySlot(EquipmentSlot.CHEST));

        // 3. 检查是否有我们注册的 river_amulet 强化
        // 【关键确认】: 这里的 river_amulet 必须对应你在注册类里的变量名
        // 我这里假设你的注册类是 ModModifiers，且变量名是 river_amulet
        return !chestArmor.isBroken() &&
                chestArmor.getModifierLevel(CasualtinkerModifier.RIVER_AMULET.get()) > 0;
    }

    /**
     * 【新增辅助方法】生成轻量化的水粒子护盾特效
     */
    private void spawnWaterRingEffect(LivingEntity entity) {
        // 必须在服务端生成粒子，客户端才能看见
        if (entity.level instanceof ServerLevel serverLevel) {

            // 粒子数量：12个足够形成一个圈，不多不少，不卡顿
            int particleCount = 12;
            // 半径：1.2格，刚好包裹住玩家
            double radius = 1.2;
            // 高度：玩家身体中间 (胸甲的位置)
            double height = entity.getY() + entity.getBbHeight() / 2.0;

            for (int i = 0; i < particleCount; i++) {
                // 计算角度 (0 到 360 度均匀分布)
                double angle = (2 * Math.PI / particleCount) * i;

                // 计算 x 和 z 的偏移量
                double xOffset = Math.cos(angle) * radius;
                double zOffset = Math.sin(angle) * radius;

                // 发送粒子
                // ParticleTypes.SPLASH : 溅水粒子，效果像水花炸开，消失快，不遮挡视线
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        entity.getX() + xOffset, // X坐标
                        height,                  // Y坐标
                        entity.getZ() + zOffset, // Z坐标
                        1,      // 每个位置生成的数量
                        0, 0, 0, // 粒子的随机偏移量 (设为0让形状更规则)
                        0.0      // 粒子速度 (0表示原地炸开)
                );
            }
        }
    }

    @Override
    public Component getDisplayName(int level) {
        // 1. Component.translatable(...) : 只获取名字，不带等级数字
        // 2. .withStyle(...) : 使用 Lambda 表达式，只修改颜色，保留原有的粗体/斜体
        // 3. TextColor.fromRgb(...) : 在这里填入你的 16进制颜色代码 (0x开头)

        return Component.translatable(getTranslationKey())
                .withStyle(style -> style.withColor((TextColor.fromRgb(0x00AAAA))));
    }
}