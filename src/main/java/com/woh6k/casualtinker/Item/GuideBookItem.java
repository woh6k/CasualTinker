package com.woh6k.casualtinker.Item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

public class GuideBookItem extends Item {

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 只在客户端执行打开界面的操作
        if (level.isClientSide) {
            // 使用 DistExecutor 确保只在客户端调用 GUI 代码，防止服务器崩溃
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> openBookGui(stack));
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * 客户端专用方法：构建书本内容并打开界面
     */
    @OnlyIn(Dist.CLIENT)
    private void openBookGui(ItemStack originalStack) {
        // 1. 创建一个伪造的原版“成书”物品
        ItemStack writtenBook = new ItemStack(Items.WRITTEN_BOOK);

        // 2. 准备 NBT 数据
        CompoundTag tag = new CompoundTag();
        tag.putString("title", "CasualTinker Guide"); // 书名（内部用，不显示）
        tag.putString("author", "CasualTinker");      // 作者

        // 3. 构建页面 (ListTag)
        ListTag pages = new ListTag();

        // --- 在这里添加你的页面 ---
        // 每一页都是一个 Component (JSON 字符串格式)

        // 第 1 页：封面 / 简介
        pages.add(createPage("book.casualtinker.page1"));

        // 第 2 页：
        pages.add(createPage("book.casualtinker.page2"));

        // 第 3 页：
        pages.add(createPage("book.casualtinker.page3"));

        // 第 4 页：
        pages.add(createPage("book.casualtinker.page4"));

        // 第 5 页：
        pages.add(createPage("book.casualtinker.page5"));

        // 第 6 页：
        pages.add(createPage("book.casualtinker.page6"));

        // ... 你可以继续添加更多页面

        tag.put("pages", pages);
        writtenBook.setTag(tag);

        // 4. 打开原版阅读界面
        // BookViewScreen 需要一个 BookAccess 接口，我们用 WrittenBookAccess
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(writtenBook)));
    }

    /**
     * 辅助方法：将翻译键转换为书本需要的 NBT 格式 (JSON 字符串)
     */
    private StringTag createPage(String translationKey) {
        // 获取翻译文本
        Component text = Component.translatable(translationKey);
        // 序列化为 JSON 字符串 (原版书本的要求)
        String json = Component.Serializer.toJson(text);
        return StringTag.valueOf(json);
    }
}