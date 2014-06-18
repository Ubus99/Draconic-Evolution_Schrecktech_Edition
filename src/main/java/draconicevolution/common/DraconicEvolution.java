package draconicevolution.common;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.creativetab.CreativeTabs;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import draconicevolution.client.creativetab.TolkienTab;
import draconicevolution.common.core.network.ChannelHandler;
import draconicevolution.common.core.proxy.CommonProxy;
import draconicevolution.common.lib.References;

@Mod(modid = References.MODID, name = References.MODNAME, version = References.VERSION)
public class DraconicEvolution {
	@Instance
	public static DraconicEvolution instance;

	@SidedProxy(clientSide = References.CLIENTPROXYLOCATION, serverSide = References.COMMONPROXYLOCATION)
	public static CommonProxy proxy;

	private static CreativeTabs tolkienTabToolsWeapons = new TolkienTab(CreativeTabs.getNextID(), References.MODID, "toolsAndWeapons");
	private static CreativeTabs tolkienTabBlocksItems = new TolkienTab(CreativeTabs.getNextID(), References.MODID, "blocksAndItems");
	public static final String networkChannelName = "DraconicEvolution";
	public static ChannelHandler channelHandler = new ChannelHandler(References.MODID, networkChannelName);
	public static FMLEventChannel channel;
	public static boolean debug = false;
	public static final Logger logger = LogManager.getLogger("DraconicEvolution");
	
	public DraconicEvolution()
	{
		//logger.info("This is Draconic Evolution");
	}
	
	@Mod.EventHandler
	public static void preInit(final FMLPreInitializationEvent event)
	{if(debug)
		System.out.println("preInit()" + event.getModMetadata().name);

		event.getModMetadata().autogenerated = false;
		event.getModMetadata().credits = "Many Online tutorials";
		event.getModMetadata().description = "This is a mod originally made for the Tolkiencraft mod pack";
		event.getModMetadata().authorList = Arrays.asList("brandon3055");
		event.getModMetadata().logoFile = "banner.png";
		event.getModMetadata().url = "http://dragontalk.net/node/71";
		event.getModMetadata().version = References.VERSION + "-MC1.7.2";

		proxy.preInit(event);
	}

	@Mod.EventHandler
	public void init(final FMLInitializationEvent event)
	{if(debug)
		System.out.println("init()");
		
		proxy.init(event);
	}

	@Mod.EventHandler
	public void postInit(final FMLPostInitializationEvent event)
	{if(debug)
		System.out.println("postInit()");
	
		proxy.postInit(event);
		
	}

	public static CreativeTabs getCreativeTab(int tab)
	{
		return tab == 1 ? tolkienTabToolsWeapons : tolkienTabBlocksItems;
	}
}