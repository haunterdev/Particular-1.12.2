package com.leclowndu93150.particular.client;

import com.leclowndu93150.particular.config.ParticularConfig;

import net.minecraft.block.BlockCake;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Central client-side Forge event hub for Particular.
 *
 * <p>Replaces the 1.20.1 {@code ForgeClientEvents.ForgeEvents} subscriber and the
 * vanilla-class mixins (all behavior is event/tick-driven here - zero mixins).
 * Register once during client pre-init.
 */
@SideOnly(Side.CLIENT)
public final class ParticularClientEvents {

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ParticularClientEvents());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }
        World world = mc.world;
        AmbientSpawner.tick(world);
        RainRippleSpawner.tick(world);
        SplashTrigger.tick(world);
        CascadeTracker.tick(world);
        ChestBubbleTracker.tick(world);
        WaveManager.tick(world);
    }

    /** Coastal waves draw here, after the translucent water layer. */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        WaveManager.render(event.getPartialTicks());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            WaveManager.clear();
        }
    }

    /**
     * Cake-eating crumb particles. Port of the 1.20.1 {@code InjectCakeBlock} mixin
     * (which hooked {@code CakeBlock.eat}). Detected here via the right-click event.
     */
    @SubscribeEvent
    public void onRightClickCake(PlayerInteractEvent.RightClickBlock event) {
        if (!ParticularConfig.cakeEatingParticles) {
            return;
        }
        World world = event.getWorld();
        if (!world.isRemote) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!(world.getBlockState(pos).getBlock() instanceof BlockCake)) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || !player.canEat(false)) {
            return;
        }
        int cakeId = Item.getIdFromItem(Items.CAKE);
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        for (int i = 0; i < 5; i++) {
            double vx = (world.rand.nextDouble() - 0.5) * 0.2;
            double vy = world.rand.nextDouble() * 0.2;
            double vz = (world.rand.nextDouble() - 0.5) * 0.2;
            world.spawnParticle(EnumParticleTypes.ITEM_CRACK, cx, cy, cz, vx, vy, vz, cakeId);
        }
        player.playSound(SoundEvents.ENTITY_PLAYER_BURP, 0.5f, world.rand.nextFloat() * 0.1f + 0.9f);
    }
}
