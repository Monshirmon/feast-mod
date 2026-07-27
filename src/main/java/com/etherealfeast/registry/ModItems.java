package com.etherealfeast.registry;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.item.BaiWeiItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.ITEM, EtherealFeast.MOD_ID);

    public static final Supplier<Item> BAIWEI_DUZHUO = ITEMS.register("baiwei_duzhuo",
            () -> new BaiWeiItem(BaiWeiItem.IdentityType.SOLO, new Item.Properties().stacksTo(1).fireResistant()));

    public static final Supplier<Item> BAIWEI_GONGXIANG = ITEMS.register("baiwei_gongxiang",
            () -> new BaiWeiItem(BaiWeiItem.IdentityType.TEAM, new Item.Properties().stacksTo(1).fireResistant()));

    public static final Supplier<Item> COOKBOOK = ITEMS.register("cookbook",
            () -> new Item(new Item.Properties().stacksTo(1)));
}
