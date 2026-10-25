package com.leclowndu93150.particular.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.leclowndu93150.particular.config.ParticularConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * One-time startup notice mirroring the standalone Falling Leaves mod's own warning.
 * Particular drops its own falling leaves, so running it alongside another mod that
 * also spawns leaf / wind-blown foliage particles can visually double up the leaves.
 * When such a mod is detected, tell the player once (ever) how to turn one off.
 */
@SideOnly(Side.CLIENT)
public class CompatWarning {

    /** modid -> display name of mods that also add their own leaf / foliage particles. */
    private static final Map<String, String> KNOWN_LEAF_PARTICLE_MODS = new LinkedHashMap<String, String>();
    static {
        KNOWN_LEAF_PARTICLE_MODS.put("fallingleaves", "Falling Leaves");
        KNOWN_LEAF_PARTICLE_MODS.put("weather2", "Weather2");
        KNOWN_LEAF_PARTICLE_MODS.put("weather2remaster", "Weather2 Remastered");
    }

    /** Marker file written once the notice has been shown, so it never repeats. */
    private static final String MARKER_NAME = "particular_leaf_compat_notice";

    /** Ticks to wait after the player appears so the message lands in a settled chat. */
    private static final int SHOW_DELAY = 60;

    private boolean handled = false;
    private int delay = -1;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new CompatWarning());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (this.handled || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            this.delay = -1; // not in a world yet; reset the countdown
            return;
        }
        if (this.delay < 0) {
            this.delay = SHOW_DELAY;
        }
        if (this.delay-- > 0) {
            return;
        }
        this.handled = true;
        showWarningIfNeeded(mc);
    }

    private void showWarningIfNeeded(Minecraft mc) {
        if (!ParticularConfig.showCompatWarning || !ParticularConfig.fallingLeaves) {
            return;
        }

        List<String> detected = new ArrayList<String>();
        for (Map.Entry<String, String> entry : KNOWN_LEAF_PARTICLE_MODS.entrySet()) {
            if (Loader.isModLoaded(entry.getKey())) {
                detected.add(entry.getValue());
            }
        }
        if (detected.isEmpty()) {
            return;
        }

        File marker = new File(Loader.instance().getConfigDir(), MARKER_NAME);
        if (marker.exists()) {
            return; // already shown once, ever
        }

        StringBuilder mods = new StringBuilder();
        for (int i = 0; i < detected.size(); i++) {
            if (i > 0) {
                mods.append(i == detected.size() - 1 ? " and " : ", ");
            }
            mods.append(detected.get(i));
        }

        mc.player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "[Particular] " + TextFormatting.YELLOW
                + "Detected " + mods + ", which also add falling-leaf or wind-blown foliage "
                + "particles. You may see leaves doubled up - if so, disable the leaf "
                + "particles in one mod's config (e.g. set fallingLeaves=false in "
                + "config/particular.cfg). "
                + TextFormatting.GRAY
                + "(Set showCompatWarning=false in config/particular.cfg to hide this.)"));

        try {
            marker.createNewFile(); // never show again on this install
        } catch (IOException ignored) {
            // non-fatal: worst case the notice shows again next launch
        }
    }
}
