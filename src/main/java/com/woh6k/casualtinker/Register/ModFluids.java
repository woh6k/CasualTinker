package com.woh6k.casualtinker.Register;

import com.woh6k.casualtinker.Casualtinker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModFluids {

    // 基础注册器
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Casualtinker.MODID);
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Casualtinker.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Casualtinker.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Casualtinker.MODID);

    // ========================================================================
    // 🔥 在这里注册流体，只需要一行！
    // 格式：register("流体名", 温度, 亮度, 颜色tint(如果不染色填0xFFFFFFFF))
    // ========================================================================

    // 1. 熔融凤血赤金
    public static final FluidRegistryObject MOLTEN_PHOENIX_GOLD = register("molten_phoenix_gold", 1500, 12, 0xFFFFFFFF);

    // 2. 熔融龙纹黑金
    public static final FluidRegistryObject MOLTEN_DRAGON_GOLD = register("molten_dragon_gold", 1500, 12, 0xFFFFFFFF);

    // 2. 熔融高松灯
    public static final FluidRegistryObject MOLTEN_TOMORI = register("molten_tomori", 1500, 12, 0xFFFFFFFF);


    // ========================================================================
    // 🛠️ 自动化工厂逻辑
    // ========================================================================

    /**
     * 这是一个辅助类，用来存那一堆 Source, Flowing, Block, Bucket 对象
     * 以后你要用桶，就调用 ModFluids.MOLTEN_PHOENIX_GOLD.bucket.get()
     */
    public static class FluidRegistryObject {
        public RegistryObject<FluidType> type;
        public RegistryObject<ForgeFlowingFluid> source;
        public RegistryObject<ForgeFlowingFluid> flowing;
        public RegistryObject<LiquidBlock> block;
        public RegistryObject<Item> bucket;
        public ForgeFlowingFluid.Properties properties;
    }

    private static FluidRegistryObject register(String name, int temperature, int lightLevel, int tintColor) {
        FluidRegistryObject obj = new FluidRegistryObject();

        // 1. 自动注册 FluidType
        obj.type = FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .density(2000)
                .viscosity(10000)
                .temperature(temperature)
                .lightLevel(lightLevel)
                .descriptionId("block." + Casualtinker.MODID + "." + name)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)) {

            // --- 修复部分开始 ---
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    // 以前这里加了 static 导致报错，现在直接在方法里生成，或者去掉 static

                    @Override
                    public ResourceLocation getStillTexture() {
                        return new ResourceLocation(Casualtinker.MODID, "fluid/" + name + "_still");
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return new ResourceLocation(Casualtinker.MODID, "fluid/" + name + "_flowing");
                    }

                    @Override
                    public int getTintColor() {
                        return tintColor;
                    }
                });
            }
        });

        // 2. 准备 Properties (核心)
        // 注意：.tickRate(30) 决定了流速像岩浆一样慢
        obj.properties = new ForgeFlowingFluid.Properties(obj.type, () -> obj.source.get(), () -> obj.flowing.get())
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .block(() -> obj.block.get())
                .bucket(() -> obj.bucket.get())
                .explosionResistance(100f)
                .tickRate(30);

        // 3. 注册 Source 和 Flowing
        obj.source = FLUIDS.register(name, () -> new ForgeFlowingFluid.Source(obj.properties));
        obj.flowing = FLUIDS.register(name + "_flowing", () -> new ForgeFlowingFluid.Flowing(obj.properties));

        // 4. 注册方块
        obj.block = BLOCKS.register(name, () -> new LiquidBlock(obj.source, Block.Properties.of(Material.LAVA)
                .strength(100f)
                .noLootTable()
                .lightLevel((BlockState state) -> lightLevel)));

        // 5. 注册桶
        obj.bucket = ITEMS.register(name + "_bucket", () -> new BucketItem(obj.source, new Item.Properties()
                .tab(ModTab.CASUAL_TINKER_TAB)
                .stacksTo(1)));

        return obj;
    }

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}