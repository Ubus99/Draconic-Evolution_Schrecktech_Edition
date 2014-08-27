package com.brandon3055.draconicevolution.client.interfaces;

import com.brandon3055.draconicevolution.common.core.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.lib.References;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import java.util.List;

/**
 * Created by Brandon on 6/08/2014.
 */
public class ConfigGUI  extends GuiConfig {
	public ConfigGUI(GuiScreen parent) {
		super(parent, new ConfigElement(ConfigHandler.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements() , References.MODID, false, false, GuiConfig.getAbridgedConfigPath(ConfigHandler.config.toString()));
		configElements.addAll(new ConfigElement(ConfigHandler.config.getCategory("spawner")).getChildElements());
		configElements.addAll(new ConfigElement(ConfigHandler.config.getCategory("long range dislocator")).getChildElements());
	}
}