package com.woh6k.casualtinker.Register;


import com.woh6k.casualtinker.Casualtinker;
import com.woh6k.casualtinker.Modifier.*;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

import com.woh6k.casualtinker.Modifier.Immortal.ImmortalBase;

public class CasualtinkerModifier {
    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Casualtinker.MODID);

    public static final StaticModifier<Teabright> TEABRIGHT = MODIFIERS.register("teabright", Teabright::new);
    public static final StaticModifier<River_amulet> RIVER_AMULET = MODIFIERS.register("river_amulet", River_amulet::new);
    public static final StaticModifier<All_out> all_out = MODIFIERS.register("all_out", All_out::new);
    public static final StaticModifier<Thrifty> thrifty = MODIFIERS.register("thrifty", Thrifty::new);
    public static final StaticModifier<Finger_sword> finger_sword = MODIFIERS.register("finger_sword", Finger_sword::new);
    public static final StaticModifier<Familiar> familiar = MODIFIERS.register("familiar", Familiar::new);
    public static final StaticModifier<Time_success> time_success = MODIFIERS.register("time_success", Time_success::new);
    public static final StaticModifier<Phoenix_repair> phoenix_repair = MODIFIERS.register("phoenix_repair", Phoenix_repair::new);
    public static final StaticModifier<Phoenix_blessing> phoenix_blessing = MODIFIERS.register("phoenix_blessing", Phoenix_blessing::new);
    public static final StaticModifier<Dragon_prestige> dragon_prestige = MODIFIERS.register("dragon_prestige", Dragon_prestige::new);
    public static final StaticModifier<Reverse_scale> reverse_scale = MODIFIERS.register("reverse_scale", Reverse_scale::new);
    public static final StaticModifier<Dragon_tooth> dragon_tooth = MODIFIERS.register("dragon_tooth", Dragon_tooth::new);
    public static final StaticModifier<Haruhikage> haruhikage = MODIFIERS.register("haruhikage", Haruhikage::new);
    public static final StaticModifier<Hitoshizuku> HITOSHIZUKU = MODIFIERS.register("hitoshizuku", Hitoshizuku::new);
    public static final StaticModifier<Mygo> MYGO = MODIFIERS.register("mygo", Mygo::new);

    //类不毁特性，共用一个java类
    public static final StaticModifier<ImmortalBase> IDEA_MUSHROOM = MODIFIERS.register("idea_mushroom", () -> new ImmortalBase(0x89CFF0));
    //同类型例子 public static final StaticModifier<ImmortalBase> ETERNAL_WOOD = MODIFIERS.register("eternal_wood", () -> new ImmortalBase(0x228B22));

}
