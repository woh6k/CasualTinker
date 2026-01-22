package com.woh6k.casualtinker.Modifier;

import com.woh6k.casualtinker.Register.CasualtinkerModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 全力以赴 (All_out) 强化模组
 * 为匠魂工具添加强制触发暴击的能力
 * 等级越高，暴击触发概率越大
 */
public class All_out extends Modifier {

    public All_out() {
        // 注册到 Forge 事件总线，以便监听暴击事件
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 监听玩家攻击前的暴击判定事件
     * 在这里我们可以无视玩家是否跳跃，强制触发暴击
     */
    @SubscribeEvent
    public void onCriticalHit(CriticalHitEvent event) {
        // 1. 安全检查：必须是玩家，且有旧的结果（避免覆盖其他强力模组的判定）
        if (event.getEntity() == null) return;

        Player player = event.getEntity();
        ItemStack heldItem = player.getMainHandItem();

        // 2. 性能优化：先检查手中是否是匠魂工具 (通过 Tag 判断)
        if (!heldItem.is(TinkerTags.Items.MODIFIABLE)) {
            return;
        }

        // 3. 解析工具数据
        // 注意：ToolStack.from 稍微有点消耗，所以前面先检查 Tag
        ToolStack tool = ToolStack.from(heldItem);

        // 4. 获取 all_out 特性的等级
        // 确保 CasualtinkerModifier.all_out 是你在注册类里写的名字
        int level = tool.getModifierLevel(CasualtinkerModifier.all_out.get());

        // 5. 核心逻辑：如果有这个特性
        if (level > 0) {
            // 计算概率：
            // 等级 1 = 40% (0.3 + 0.1)
            // 等级 2 = 50% (0.3 + 0.2)
            // 等级 3 = 60% (0.3 + 0.3)
            // 超过 3 级按照等级 3 触发
            level = Math.min(level, 3);
            float chance = 0.30f + (0.10f * level);

            // 随机判定
            if (player.getRandom().nextFloat() < chance) {
                // 【强制暴击】
                // setDamageModifier(1.5F) 是原版暴击倍率，你也可以改更高
                event.setDamageModifier(1.5F);

                // Result.ALLOW 会强制触发暴击效果（颗粒+伤害提升），无视玩家是否在空中
                event.setResult(Event.Result.ALLOW);
            }
        }
    }
    @Override
    public Component getDisplayName(int level) {
        // 这里用了 0xDC143C，你可以随意改成任何十六进制颜色
        return super.getDisplayName(level).copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xDC143C)));
    }
}