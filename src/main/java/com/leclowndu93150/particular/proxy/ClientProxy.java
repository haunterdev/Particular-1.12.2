package com.leclowndu93150.particular.proxy;

import com.leclowndu93150.particular.client.CompatWarning;
import com.leclowndu93150.particular.client.ParticleSprites;
import com.leclowndu93150.particular.client.ParticularClientEvents;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side proxy. Initializes particle sprite registration and the
 * client-side event hub during Forge pre-initialization.
 */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        ParticleSprites.init();
        ParticularClientEvents.register();
        CompatWarning.register();
    }
}
