package com.etherealfeast;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.event.ModEvents;
import com.etherealfeast.network.ModNetwork;
import com.etherealfeast.registry.ModCreativeTabs;
import com.etherealfeast.registry.ModItems;
import com.etherealfeast.registry.ModRecipes;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(EtherealFeast.MOD_ID)
public class EtherealFeast {
    public static final String MOD_ID = "ethereal_feast";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EtherealFeast(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Ethereal Feast - 异界食缘 v1.0.1 Initializing...");

        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        PlayerIdentityData.ATTACHMENT_TYPES.register(modEventBus);

        ModNetwork.register();

        // Register game event handlers on the NeoForge event bus
        NeoForge.EVENT_BUS.register(new ModEvents());

        LOGGER.info("Ethereal Feast - 异界食缘 v1.0.1 Initialized!");
    }
}
