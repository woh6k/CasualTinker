package com.woh6k.casualtinker.Effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class DragonPiecesEffect extends MobEffect {

    public DragonPiecesEffect() {
        // HARMFUL = 有害 (红色名字)
        // 0x8A0303 = 深龙血色
        super(MobEffectCategory.HARMFUL, 0x8A0303);

        // 注册属性修饰符
        // 注意：这里的 -0.0 (数值) 是占位符，不起实际作用，
        // 实际数值完全由下面的 getAttributeModifierValue 方法决定。
        this.addAttributeModifier(
                Attributes.ARMOR,
                "d8a9e6b2-1c4f-4b8a-9e7d-3f2a1b5c8d0e", // 固定UUID
                -0.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    /**
     * 重写此方法以实现非线性的数值成长：
     * 等级 1 (Amp 0): -10%
     * 等级 2 (Amp 1): -20%
     * 等级 3 (Amp 2): -30%
     * 等级 4+ (Amp 3+): -50% (封顶)
     */
    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        // amplifier 从 0 开始 (0 就是药水等级1)

        if (amplifier == 0) return -0.10; // 1级 10%
        if (amplifier == 1) return -0.20; // 2级 20%
        if (amplifier == 2) return -0.30; // 3级 30%

        // 4级及以上 (amplifier >= 3)，固定为 50%
        return -0.50;
    }
}