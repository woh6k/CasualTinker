package com.woh6k.casualtinker.Modifier.Immortal;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

public class ImmortalBase extends Modifier implements VolatileDataModifierHook, ToolDamageModifierHook {

    // 用来存储当前特性的颜色
    private final int color;

    //构造函数接收颜色参数
    public ImmortalBase(int color) {
        super();
        this.color = color;
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ModifierHooks.VOLATILE_DATA);
        builder.addHook(this, ModifierHooks.TOOL_DAMAGE);
    }

    // --- 第一道保险：NBT 标记 ---
    // 让 UI 显示无限符号，并隐藏耐久条
    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ModDataNBT volatileData) {
        volatileData.putBoolean(new ResourceLocation("tconstruct", "unbreakable"), true);
    }

    // --- 第二道保险：硬逻辑拦截 ---
    // 强制将扣除的耐久设为 0
    @Override
    public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder) {
        return 0;
    }

    //重写显示名称
    // 1. 去掉 super 调用，直接返回翻译名 -> 移除罗马数字等级
    // 2. 使用 this.color -> 实现不同特性不同颜色
    @Override
    public Component getDisplayName(int level) {
        return Component.translatable(this.getTranslationKey())
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.color)));
    }
}