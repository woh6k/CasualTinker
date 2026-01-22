package com.woh6k.casualtinker.Modifier;

import net.minecraft.ChatFormatting; // 用于设置文本颜色
import net.minecraft.core.particles.ParticleTypes; // 用于引用粒子类型
import net.minecraft.nbt.Tag; // 用于NBT标签类型判断
import net.minecraft.network.chat.Component; // 聊天组件
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation; // 资源路径
import net.minecraft.sounds.SoundEvents; // 原版音效
import net.minecraft.sounds.SoundSource; // 音效来源分类
import net.minecraft.world.effect.MobEffectInstance; // 药水效果实例
import net.minecraft.world.effect.MobEffects; // 原版药水效果
import net.minecraft.world.entity.LivingEntity; // 活体生物基类
import net.minecraft.world.entity.player.Player; // 玩家类
import net.minecraft.world.item.TooltipFlag; // 工具提示Flag
import slimeknights.mantle.client.TooltipKey; // 匠魂的按键提示
import slimeknights.tconstruct.common.TinkerTags; // 匠魂的标签库
import slimeknights.tconstruct.library.modifiers.Modifier; // 修饰符基类
import slimeknights.tconstruct.library.modifiers.ModifierEntry; // 修饰符条目(含等级)
import slimeknights.tconstruct.library.modifiers.ModifierHooks; // 钩子注册表
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook; // 近战伤害钩子
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook; // 近战命中钩子
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook; // 工具提示钩子
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook; // 背包Tick钩子
import slimeknights.tconstruct.library.module.ModuleHookMap; // 钩子注册Builder
import slimeknights.tconstruct.library.tools.context.ToolAttackContext; // 攻击上下文
import slimeknights.tconstruct.library.tools.nbt.IToolStackView; // 工具数据视图
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 特性：春日影 (Haruhikage)
 * 效果：
 * 1. 5秒内未攻击，武器进入【蓄势】状态。
 * 2. 蓄势状态下攻击，伤害大幅提升 (1.5x ~ 2.0x)，并给予目标发光效果。
 * 3. 记录并显示该特性的触发次数。
 * 4. 蓄势完成时，在持有者脚底生成粒子特效。
 */
public class Haruhikage extends Modifier implements MeleeDamageModifierHook, MeleeHitModifierHook, InventoryTickModifierHook, TooltipModifierHook {

    // NBT 键名：记录上一次攻击的时间戳 (GameTime)
    private static final ResourceLocation LAST_ATTACK_TIME = new ResourceLocation("casualtinker", "last_attack_time");
    // NBT 键名：记录成功触发蓄势攻击的次数
    private static final ResourceLocation TRIGGER_COUNT = new ResourceLocation("casualtinker", "haruhikage_trigger_count");

    // 蓄力阈值：5秒 = 100 ticks (Minecraft逻辑是 20 ticks = 1秒)
    private static final int CHARGE_TIME = 100;

    public Haruhikage() {
        super();
    }

    /**
     * 注册钩子方法。
     * 必须在这里声明实现了哪些接口，否则对应的方法不会被游戏调用。
     */
    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.MELEE_DAMAGE);   // 计算伤害倍率
        builder.addHook(this, ModifierHooks.MELEE_HIT);      // 攻击后特效与重置时间
        builder.addHook(this, ModifierHooks.INVENTORY_TICK); // 粒子特效与初始化
        builder.addHook(this, ModifierHooks.TOOLTIP);        // 显示触发次数
    }

    // =================================================================================
    // 辅助方法区域：用于安全的读写 NBT 数据
    // =================================================================================

    /**
     * 读取上一次攻击的时间。
     * 注意：由于 1.19.2 ModDataNBT 暂无 getLong 方法，这里使用 String 转换作为变通方案。
     */
    private long getLastAttackTime(IToolStackView tool) {
        String timeStr = tool.getPersistentData().getString(LAST_ATTACK_TIME);
        // 如果字符串为空，说明从未记录过，返回 0
        if (timeStr.isEmpty()) return 0L;
        try {
            return Long.parseLong(timeStr);
        } catch (NumberFormatException e) {
            // 防止数据损坏导致报错，如果解析失败也返回 0
            return 0L;
        }
    }

    /**
     * 保存当前攻击时间。
     * 将 Long 类型的时间戳转换为 String 存入 NBT。
     */
    private void setLastAttackTime(IToolStackView tool, long time) {
        tool.getPersistentData().putString(LAST_ATTACK_TIME, String.valueOf(time));
    }

    /**
     * 读取触发次数 (Int 类型可以直接读写)。
     */
    private int getTriggerCount(IToolStackView tool) {
        return tool.getPersistentData().getInt(TRIGGER_COUNT);
    }

    /**
     * 增加一次触发计数。
     */
    private void addTriggerCount(IToolStackView tool) {
        int current = getTriggerCount(tool);
        tool.getPersistentData().putInt(TRIGGER_COUNT, current + 1);
    }

    // =================================================================================
    // 1. Tooltip 钩子：在工具信息栏显示数据
    // =================================================================================
    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        // 获取当前的累计次数
        int count = getTriggerCount(tool);

        // 添加一行文本： "春日影触发次数: X" (数字显示为金色)
        // translatable 对应 zh_cn.json 里的 key
        tooltip.add(Component.translatable("stat.casualtinker.haruhikage_count")
                .append(": ")
                .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.GOLD)));
    }

    // =================================================================================
    // 2. 伤害钩子：计算蓄势后的额外伤害
    // =================================================================================
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        // 只有【近战武器】才生效，防止用铲子/空手套属性
        if (!tool.hasTag(TinkerTags.Items.MELEE)) return damage;

        // 获取当前游戏时间
        long currentTime = context.getAttacker().level.getGameTime();
        // 获取上次攻击时间
        long lastAttack = getLastAttackTime(tool);

        // 判断：时间间隔是否超过 5 秒
        if (currentTime - lastAttack >= CHARGE_TIME) {
            // 获取特性等级，最高按 4 级计算
            int level = Math.min(modifier.getLevel(), 4);
            float multiplier;

            // 根据等级设定伤害倍率
            switch (level) {
                case 1: multiplier = 1.5f;  break; // 1级 1.5倍
                case 2: multiplier = 1.65f; break; // 2级 1.65倍
                case 3: multiplier = 1.8f;  break; // 3级 1.8倍
                default: multiplier = 2.0f; break; // 4级 2.0倍
            }
            // 返回翻倍后的伤害
            return damage * multiplier;
        }

        // 如果没蓄满，返回原伤害
        return damage;
    }

    // =================================================================================
    // 3. 攻击后钩子：处理特效、计数、重置时间
    // =================================================================================
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!tool.hasTag(TinkerTags.Items.MELEE)) return;

        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        long currentTime = attacker.level.getGameTime();
        long lastAttack = getLastAttackTime(tool);

        // 如果这是一次【蓄势攻击】(时间差满足条件)
        if (currentTime - lastAttack >= CHARGE_TIME) {
            if (target != null) {
                // 1. 给被打的目标发光效果 (1秒)
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20, 0));

                // 2. 播放清脆的音效 (紫水晶声)
                attacker.level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);

                // 3. 粒子特效 (仅在客户端执行)
                // 在目标身上炸开一圈淡金色光尘
                if (attacker.level.isClientSide) {
                    for (int i = 0; i < 8; i++) {
                        attacker.level.addParticle(ParticleTypes.END_ROD,
                                target.getX() + (attacker.getRandom().nextDouble() - 0.5),
                                target.getY() + target.getEyeHeight() / 2,
                                target.getZ() + (attacker.getRandom().nextDouble() - 0.5),
                                0, 0.05, 0);
                    }
                }

                // 4. 【关键逻辑】计数器 +1
                addTriggerCount(tool);
            }
        }

        // 无论是否蓄满，只要攻击了，就更新时间戳，强制打断/重置蓄力
        setLastAttackTime(tool, currentTime);
    }

    // =================================================================================
    // 4. 背包钩子：处理待机时的粒子特效 (此处修改了位置)
    // =================================================================================
    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, net.minecraft.world.level.Level level, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, net.minecraft.world.item.ItemStack stack) {
        // 只有当【手持】且是【武器】时才生效
        if (isSelected && tool.hasTag(TinkerTags.Items.MELEE)) {

            long currentTime = level.getGameTime();

            // 【初始化逻辑】
            // 如果是一把新做的刀(没有NBT记录)，强制设为当前时间。
            // 防止玩家刚做出来第一刀就暴击，必须先冷却5秒。
            if (!tool.getPersistentData().contains(LAST_ATTACK_TIME, Tag.TAG_STRING)) {
                setLastAttackTime(tool, currentTime);
                return;
            }

            long lastAttack = getLastAttackTime(tool);

            // 如果蓄势完成 (时间差 >= 5秒)
            if (currentTime - lastAttack >= CHARGE_TIME) {
                // 粒子只在客户端生成
                if (level.isClientSide) {
                    // 频率：每 20 tick (1秒) 生成一次，避免太晃眼
                    if (currentTime % 20 == 0) {

                        // 【修改核心】：将粒子移到脚底下
                        // holder.getX() / getZ() 是身体中心
                        // holder.getY() 是脚底板的Y坐标

                        // X/Z轴：在身体周围 0.5 格范围内随机
                        double spawnX = holder.getX() + (holder.getRandom().nextDouble() - 0.5) * 0.5;
                        double spawnZ = holder.getZ() + (holder.getRandom().nextDouble() - 0.5) * 0.5;

                        // Y轴：holder.getY() 是地面，+0.1 稍微悬浮一点点，防止穿模到地里
                        double spawnY = holder.getY() + 0.1;

                        // 生成 END_ROD 粒子 (淡金色光尘)
                        // 速度(0, 0.01, 0) 让它微微向上飘一点点
                        level.addParticle(ParticleTypes.END_ROD, spawnX, spawnY, spawnZ, 0, 0.01, 0);
                    }
                }
            }
        }
    }
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName(level).copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x89CFF0)));
    }
}



