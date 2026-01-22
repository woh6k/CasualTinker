package com.woh6k.casualtinker.Modifier;

import com.woh6k.casualtinker.Register.CasualtinkerModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 见面曾相识 (Familiar) 强化模组
 * 为匠魂头盔添加降低存在感的能力
 * 穿戴带有此强化的头盔时，怪物发现玩家的距离减少50%
 */
public class Familiar extends Modifier {

    public Familiar() {
        // 注册事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 监听生物可见性事件
     * 这是控制怪物仇恨范围最底层、最有效的逻辑
     */
    @SubscribeEvent
    public void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        // 1. 事件的主体(getEntity)是被观察者（也就是玩家）
        // 我们只关心玩家被看的情况
        if (event.getEntity() instanceof Player player) {

            // 2. 检查玩家是否穿戴了头盔，且头盔有效
            ItemStack helmetStack = player.getItemBySlot(EquipmentSlot.HEAD);
            if (helmetStack.isEmpty() || !helmetStack.is(TinkerTags.Items.ARMOR)) {
                return;
            }

            // 3. 将 ItemStack 转为匠魂 ToolStack 进行检查
            ToolStack tool = ToolStack.from(helmetStack);

            // 4. 检查：工具未损坏 && 拥有 familiar 强化
            if (!tool.isBroken() && tool.getModifierLevel(CasualtinkerModifier.familiar.get()) > 0) {

                // 5. 【核心逻辑】修改可见性
                // modifyVisibility 方法接收一个倍率。
                // 传入 0.5 意味着将当前的可见度减半。
                // 这与潜行(0.8)和隐身药水会乘算叠加。
                // 效果：怪物发现玩家的距离变为原来的 50%。
                event.modifyVisibility(0.5);
            }
        }
    }

    @Override
    public Component getDisplayName(int level) {
        // 使用淡紫色 (类似于附魔光泽的颜色)
        return Component.translatable(getTranslationKey())
                .withStyle(style -> style.withColor(TextColor.fromRgb(0xD8BFD8)));
    }
}