package com.woh6k.casualtinker.Item;

import com.woh6k.casualtinker.Casualtinker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StartStoneItem extends Item {

    // 定义 Loot Table 的位置
    private static final ResourceLocation LOOT_TABLE = new ResourceLocation(Casualtinker.MODID, "items/start_stone_reward");

    public StartStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;

            // 1. 获取战利品表
            LootTable table = serverLevel.getServer().getLootTables().get(LOOT_TABLE);

            // 2. 构建战利品上下文 (LootContext)
            // 这一步是为了告诉战利品表：是谁打开的？在哪里打开的？
            // 这样如果你在 JSON 里写了 "luck" (幸运) 条件，它就能读取玩家的幸运值
            LootContext.Builder builder = new LootContext.Builder(serverLevel)
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withParameter(LootContextParams.ORIGIN, player.position());

            // 3. 生成物品列表
            // LootContextParamSets.GIFT 表示这是一个礼物/抽奖类型，通常用于这种物品右键
            List<ItemStack> generatedItems = table.getRandomItems(builder.create(LootContextParamSets.GIFT));

            // 4. 发放物品
            for (ItemStack reward : generatedItems) {

                // 先把名字存到一个变量里！
                // 必须在 add() 之前获取名字，否则 add() 会把 reward 变成空(0)
                Component name = reward.getDisplayName();

                // 如果背包满了，add 返回 false，则执行 drop 扔在地上
                // 这一步执行完，reward 的数量通常就变成 0 了
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }

                // 【使用】这里我们要用刚才存下来的 name，而不是 reward.getDisplayName()
                player.sendSystemMessage(Component.translatable("message.casualtinker.lottery.gain").append(name));
            }

            // 5. 消耗原初之石 & 播放音效
            if (!player.isCreative()) {
                itemStack.shrink(1);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.0F);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("tooltip.casualtinker.start_stone.desc_1"));
        tooltipComponents.add(Component.translatable("tooltip.casualtinker.start_stone.desc_2"));
    }
}