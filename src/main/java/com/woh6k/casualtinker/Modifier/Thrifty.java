package com.woh6k.casualtinker.Modifier;

import com.woh6k.casualtinker.Register.CasualtinkerModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * 节约风气 (Thrifty) 强化模组
 * 为匠魂远程武器添加弹药返还功能
 * 攻击时有40%概率使射出的弹药不消耗
 */
public class Thrifty extends Modifier {

    // 用一个 ThreadLocal 标记，用来在两个事件之间传递“这次射击是否免单”的信息
    private final ThreadLocal<Boolean> isFreeShot = ThreadLocal.withInitial(() -> false);

    public Thrifty() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 第一阶段：【预付款】
     * 监听玩家“松开右键”准备射击的瞬间
     * 如果判定成功，且背包没满，直接给一根箭，抵消即将发生的消耗
     */
    @SubscribeEvent
    public void onStopUsing(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity().level.isClientSide || !(event.getEntity() instanceof Player player)) {
            return;
        }

        // 1. 重置标记
        isFreeShot.set(false);

        // 2. 检查工具
        ItemStack stack = event.getItem();
        if (!stack.is(TinkerTags.Items.RANGED)) return;

        ToolStack tool = ToolStack.from(stack);
        if (tool.getModifierLevel(CasualtinkerModifier.thrifty.get()) <= 0) return;

        // 3. 检查是否拉满弓（是否达到发射时间）
        // 匠魂工具的拉弓时间是动态的，需要获取 stats
        float drawTime = tool.getStats().get(ToolStats.DRAW_SPEED);
        int useDuration = event.getDuration(); // 这里的 duration 是“剩余时间”还是“已用时间”取决于版本，通常是总时间-剩余时间
        int timeUsed = stack.getUseDuration() - useDuration;

        // 简单的判定：如果拉弓时间太短，甚至射不出去，就别送箭了 (这里取 10 tick 作为一个通用阈值，或者你可以更精确)
        if (timeUsed < 10) return;

        // 4. 判定概率 (40%)
        // 还要排除创造模式和无限附魔（防止多送）
        if (player.isCreative() || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) > 0) {
            return;
        }

        if (player.getRandom().nextFloat() < 0.4f) {
            // 判定成功！标记这次射击为免单
            isFreeShot.set(true);

            // --- 核心：寻找并填充弹药 ---
            // 我们需要找到即将被消耗的那组箭
            ItemStack ammo = findAmmo(player);

            // 如果找到了箭，且还没满组 ( < 64 )
            if (!ammo.isEmpty() && ammo.getCount() < ammo.getMaxStackSize()) {
                // 【预付款】：先加 1
                ammo.grow(1);
                // 此时背包数字变成了 X+1
                // 紧接着 Minecraft 原版逻辑运行：扣除 1
                // 最终结果：X。 完美无感！

                // 因为已经预付了，我们在第二阶段就不用再给箭了，只要把射出去的实体设为不可捡起即可
            } else {
                // 如果背包满了（64个），预付款失败。
                // 我们保持 isFreeShot = true，留给第二阶段去处理“事后返还”。
            }
        }
    }

    /**
     * 第二阶段：【事后处理】
     * 监听箭矢生成。
     * 1. 负责把射出去的箭设为“不可捡起”（防止无限刷）。
     * 2. 如果第一阶段因为“背包满”没能给箭，这里负责补给（丢在地上或者强塞）。
     */
    @SubscribeEvent
    public void onProjectileSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;

        if (event.getEntity() instanceof AbstractArrow arrow) {
            if (arrow.getOwner() instanceof Player player) {

                // 检查是否是本次触发了特性的射击
                if (isFreeShot.get()) {

                    // 1. 无论如何，这根射出去的箭必须是幻影（防止刷物品）
                    arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;

                    // 2. 检查是否需要“事后补票”
                    // 如果刚才预付款成功了（背包没满），这里就不需要动背包了。
                    // 但我们很难知道刚才预付款到底成功没，所以我们可以检查一下背包状态
                    // 或者更简单的：再次尝试寻找未满的箭矢堆

                    ItemStack shotAmmo = getArrowStackSafe(arrow);
                    if (shotAmmo.isEmpty()) return;

                    // 如果背包里这组箭是满的 (说明刚才预付款失败了，或者预付款后变成满的了)
                    // 或者我们简单粗暴点：
                    // 为了防止逻辑复杂，我们只播放音效。
                    // 只有在极少数情况（背包满64个箭时触发节约），玩家会看到数字 64 -> 63 -> 64 的闪烁（原版返还逻辑）
                    // 但绝大多数时候（背包不满），第一阶段已经处理完美了。

                    // 再次检查并“补票”的逻辑（仅针对满组情况）：
                    // 如果是满组，刚才第一阶段没加进去，现在变成了 63。
                    // 我们可以在这里把它加回 64。
                    ItemStack ammoInInv = findAmmoStack(player, shotAmmo);
                    if (!ammoInInv.isEmpty() && ammoInInv.getCount() == ammoInInv.getMaxStackSize() - 1) {
                        ammoInInv.grow(1); // 补回 64
                        if (player instanceof ServerPlayer sp) sp.inventoryMenu.sendAllDataToRemote();
                    }

                    // 播放提示音
                    player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1f, 0.5f);

                    // 重置标记 (防止连发弩或其他情况误判)
                    isFreeShot.set(false);
                }
            }
        }
    }

    // --- 辅助方法 ---

    // 模拟原版寻找弹药的逻辑
    private ItemStack findAmmo(Player player) {
        Predicate<ItemStack> predicate = (s) -> s.getItem() instanceof ArrowItem; // 简化版判定

        // 1. 先找副手
        if (predicate.test(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        // 2. 再找主手
        if (predicate.test(player.getMainHandItem())) {
            return player.getMainHandItem();
        }
        // 3. 遍历背包
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (predicate.test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // 寻找背包里特定的那组箭
    private ItemStack findAmmoStack(Player player, ItemStack target) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(stack, target)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack getArrowStackSafe(AbstractArrow arrow) {
        try {
            Method method = AbstractArrow.class.getDeclaredMethod("getPickupItem");
            method.setAccessible(true);
            return (ItemStack) method.invoke(arrow);
        } catch (Exception e) {
            try {
                Method methodSrg = AbstractArrow.class.getDeclaredMethod("m_36789_");
                methodSrg.setAccessible(true);
                return (ItemStack) methodSrg.invoke(arrow);
            } catch (Exception ignored) {}
            return new ItemStack(Items.ARROW);
        }
    }

    @Override
    public Component getDisplayName(int level) {
        return Component.translatable(getTranslationKey())
                .withStyle(style -> style.withColor(TextColor.fromRgb(0x0000FF)));
    }
}