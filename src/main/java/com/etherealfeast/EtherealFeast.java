package com.etherealfeast;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.command.FeastCommand;
import com.etherealfeast.entity.InvasionMonster;
import com.etherealfeast.event.FeastHudOverlay;
import com.etherealfeast.event.ModEvents;
import com.etherealfeast.invasion.InvasionManager;
import com.etherealfeast.network.ModNetwork;
import com.etherealfeast.registry.ModBlocks;
import com.etherealfeast.registry.ModCreativeTabs;
import com.etherealfeast.registry.ModEntities;
import com.etherealfeast.registry.ModItems;
import com.etherealfeast.registry.ModRecipes;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(EtherealFeast.MOD_ID)
public class EtherealFeast {
    public static final String MOD_ID = "ethereal_feast";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EtherealFeast(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Ethereal Feast - 异界食缘 v1.0.1 Initializing...");

        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        PlayerIdentityData.ATTACHMENT_TYPES.register(modEventBus);

        // Register entity attributes
        modEventBus.addListener(
                (EntityAttributeCreationEvent event) -> {
                    event.put(ModEntities.CORRUPTED_ANIMAL.get(), InvasionMonster.createAttributes(30.0).build());
                    event.put(ModEntities.SPORE_ZOMBIE.get(), InvasionMonster.createAttributes(40.0).build());
                    event.put(ModEntities.MIRROR_CREEPER.get(), InvasionMonster.createAttributes(50.0).build());
                    event.put(ModEntities.FLESH_GOLEM.get(), InvasionMonster.createAttributes(75.0).build());
                    event.put(ModEntities.FALSE_GOD.get(), InvasionMonster.createAttributes(150.0).build());
                });

        // Register network payload handlers on the mod event bus
        modEventBus.addListener(ModNetwork::onRegisterPayloads);

        // Register game event handlers
        NeoForge.EVENT_BUS.register(new ModEvents());
        PlayerIdentityData.Events.register();

        // Register commands
        NeoForge.EVENT_BUS.addListener(
                (RegisterCommandsEvent event) -> FeastCommand.register(event.getDispatcher()));

        // Register invasion system
        NeoForge.EVENT_BUS.register(InvasionManager.init());

        // Client-side only registration
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FeastHudOverlay.register(modEventBus);
        }

        LOGGER.info("Ethereal Feast - 异界食缘 v1.0.1 Initialized!");
    }
}
