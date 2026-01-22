package com.woh6k.casualtinker.Register;

import com.woh6k.casualtinker.Casualtinker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Casualtinker.MODID);

    // 注册声音事件，名字 "holiday_voice" 必须和 sounds.json 里的键名一致
    public static final RegistryObject<SoundEvent> HOLIDAY_VOICE = SOUNDS.register("holiday_voice",
            () -> new SoundEvent(new ResourceLocation(Casualtinker.MODID, "holiday_voice")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}