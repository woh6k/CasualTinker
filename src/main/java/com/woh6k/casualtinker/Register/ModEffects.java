package com.woh6k.casualtinker.Register;

import com.woh6k.casualtinker.Casualtinker;
import com.woh6k.casualtinker.Effect.CoercionEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {

    // 创建注册器
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Casualtinker.MODID);

    // 1. 注册 "威压" 效果
    public static final RegistryObject<MobEffect> COERCION = MOB_EFFECTS.register("coercion", CoercionEffect::new);
    //注册 ”碎龙“效果
    public static final RegistryObject<MobEffect> DRAGON_PIECES = MOB_EFFECTS.register("dragon_pieces", CoercionEffect::new);




    // 注册方法
    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }
}