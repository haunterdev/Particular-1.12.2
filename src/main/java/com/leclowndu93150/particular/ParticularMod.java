package com.leclowndu93150.particular;

import com.leclowndu93150.particular.proxy.CommonProxy;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = ParticularMod.MODID, name = "Particular", version = "1.5.2.0",
        clientSideOnly = true, acceptableRemoteVersions = "*")
public class ParticularMod {
    public static final String MODID = "particular";
    public static final Logger LOGGER = LogManager.getLogger("particular");

    @SidedProxy(clientSide = "com.leclowndu93150.particular.proxy.ClientProxy",
                serverSide = "com.leclowndu93150.particular.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }
}
