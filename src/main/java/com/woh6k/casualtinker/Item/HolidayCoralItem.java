package com.woh6k.casualtinker.Item;

import com.woh6k.casualtinker.Casualtinker;
import com.woh6k.casualtinker.Register.ModSounds;
import com.woh6k.casualtinker.Register.ModTab;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HolidayCoralItem extends Item {

    public HolidayCoralItem() {
        super(new Properties().tab(ModTab.CASUAL_TINKER_TAB).stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 1. 客户端逻辑 (isClientSide = true)：负责播放声音
        // 因为是在客户端执行，所以只有当前玩家的电脑会响，别人听不到。
        if (level.isClientSide) {
            player.playSound(
                    ModSounds.HOLIDAY_VOICE.get(),
                    0.8F, // 音量
                    1.0F  // 音调
            );
        }

        // 2. 服务端逻辑 (!isClientSide)：负责扣除冷却
        // 冷却时间必须在服务端记录，防止作弊。
        if (!level.isClientSide) {
            // 设置 60 tick (3秒) 的冷却
            player.getCooldowns().addCooldown(this, 60);
        }

        // 侧边成功 (sidedSuccess) 会让手部播放挥动动画
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}