package com.brandon3055.draconicevolution.common.core.proxy;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.keybinding.KeyBindings;
import com.brandon3055.draconicevolution.client.keybinding.KeyInputHandler;
import com.brandon3055.draconicevolution.client.render.*;
import com.brandon3055.draconicevolution.common.blocks.ModBlocks;
import com.brandon3055.draconicevolution.common.entity.EntityCustomDragon;
import com.brandon3055.draconicevolution.common.items.ModItems;
import com.brandon3055.draconicevolution.common.tileentities.*;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyPylon;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyStorageCore;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

public class ClientProxy extends CommonProxy {
	private final static boolean debug = DraconicEvolution.debug;
	
	@Override
	public void preInit(FMLPreInitializationEvent event)
	{if(debug)
		System.out.println("on Client side");
		super.preInit(event);
		
		//Client Only
		FMLCommonHandler.instance().bus().register(new KeyInputHandler());
		registerRendering();
	}

	@Override
	public void init(FMLInitializationEvent event)
	{if(debug)
		System.out.println("on Client side");
		super.init(event);
		KeyBindings.init();
		//Client Only
	}

	@Override
	public void postInit(FMLPostInitializationEvent event)
	{if(debug)
		System.out.println("on Client side");
		super.postInit(event);
		
		//Client Only
	}

	public void registerRendering()
	{
		MinecraftForgeClient.registerItemRenderer(ModItems.wyvernBow, new BowRenderer());
		MinecraftForgeClient.registerItemRenderer(ModItems.draconicBow, new BowRenderer());
		MinecraftForgeClient.registerItemRenderer(ModItems.mobSoul, new SoulItemRenderer());
		
		TileEntitySpecialRenderer render = new ParticleGenRenderer();
		ClientRegistry.bindTileEntitySpecialRenderer(TileParticleGenerator.class, render);
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(ModBlocks.particleGenerator), new ItemParticleGenRenderer(render, new TileParticleGenerator()));

		render = new EnergyInfiserRenderer();
		ClientRegistry.bindTileEntitySpecialRenderer(TileEnergyInfuser.class, render);
		MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(ModBlocks.energyInfuser), new ItemEnergyInfuserRenderer(render, new TileEnergyInfuser()));

		render = new CustonSpawnerRenderer();
		ClientRegistry.bindTileEntitySpecialRenderer(TileCustomSpawner.class, render);

		render = new TestBlockRenderer();
		ClientRegistry.bindTileEntitySpecialRenderer(TileTestBlock.class, render);

		render = new EnergyStorageCoreRenderer();
		ClientRegistry.bindTileEntitySpecialRenderer(TileEnergyStorageCore.class, render);

		render = new EnergyPylonRenderer();
		ClientRegistry.bindTileEntitySpecialRenderer(TileEnergyPylon.class, render);

		render = new PlacedItemRenderer();
		ClientRegistry.bindTileEntitySpecialRenderer(TilePlacedItem.class, render);

		RenderingRegistry.registerEntityRenderingHandler(EntityCustomDragon.class, new CustomDragonRenderer());
	}
}