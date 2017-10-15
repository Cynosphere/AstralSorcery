/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2017
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.event.listener;

import hellfirepvp.astralsorcery.common.CommonProxy;
import hellfirepvp.astralsorcery.common.auxiliary.tick.ITickHandler;
import hellfirepvp.astralsorcery.common.base.Plants;
import hellfirepvp.astralsorcery.common.constellation.cape.impl.*;
import hellfirepvp.astralsorcery.common.entities.EntitySpectralTool;
import hellfirepvp.astralsorcery.common.item.wearable.ItemCape;
import hellfirepvp.astralsorcery.common.lib.Constellations;
import hellfirepvp.astralsorcery.common.network.PacketChannel;
import hellfirepvp.astralsorcery.common.network.packet.server.PktParticleEvent;
import hellfirepvp.astralsorcery.common.util.CropHelper;
import hellfirepvp.astralsorcery.common.util.MiscUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: EventHandlerCapeEffects
 * Created by HellFirePvP
 * Date: 10.10.2017 / 00:34
 */
public class EventHandlerCapeEffects implements ITickHandler {

    private static final Random rand = new Random();
    public static EventHandlerCapeEffects INSTANCE = new EventHandlerCapeEffects();

    private EventHandlerCapeEffects() {}

    //Prevent event overflow
    private static boolean discidiaChainingAttack = false;
    private static boolean evorsioChainingBreak = false;

    @SubscribeEvent
    public void breakBlock(BlockEvent.BreakEvent event) {
        if(event.getWorld().isRemote) return;
        if(evorsioChainingBreak) return;

        EntityPlayer pl = event.getPlayer();
        if(pl == null || !(pl instanceof EntityPlayerMP)) return;
        if(MiscUtils.isPlayerFakeMP((EntityPlayerMP) pl)) return;

        IBlockState state = event.getState();
        ItemStack held = pl.getHeldItemMainhand();


        CapeEffectPelotrio pel = ItemCape.getCapeEffect(pl, Constellations.pelotrio);
        if(pel != null) {
            if(("pickaxe".equalsIgnoreCase(state.getBlock().getHarvestTool(state)) ||
                    (!state.getMaterial().isToolNotRequired() && Items.DIAMOND_PICKAXE.canHarvestBlock(state))) &&
                    !pl.getHeldItemMainhand().isEmpty() && pl.getHeldItemMainhand().getItem().getToolClasses(held).contains("pickaxe")) {
                if (rand.nextFloat() < pel.getChanceSpawnPick()) {
                    BlockPos at = pl.getPosition().up();
                    EntitySpectralTool esp = new EntitySpectralTool(
                            event.getWorld(), at, new ItemStack(Items.DIAMOND_PICKAXE),
                            EntitySpectralTool.ToolTask.createPickaxeTask());
                    event.getWorld().spawnEntity(esp);
                    return;
                }
            }
            if((state.getBlock().isWood(event.getWorld(), event.getPos()) ||
                    state.getBlock().isLeaves(state, event.getWorld(), event.getPos())) &&
                    !pl.getHeldItemMainhand().isEmpty() && pl.getHeldItemMainhand().getItem().getToolClasses(held).contains("axe")) {
                if (rand.nextFloat() < pel.getChanceSpawnAxe()) {
                    BlockPos at = pl.getPosition().up();
                    EntitySpectralTool esp = new EntitySpectralTool(
                            event.getWorld(), at, new ItemStack(Items.DIAMOND_AXE),
                            EntitySpectralTool.ToolTask.createLogTask());
                    event.getWorld().spawnEntity(esp);
                }
            }
        }
        CapeEffectEvorsio ev =  ItemCape.getCapeEffect(pl, Constellations.evorsio);
        if(ev != null &&
                !pl.getHeldItemMainhand().isEmpty() &&
                !pl.getHeldItemMainhand().getItem().getToolClasses(pl.getHeldItemMainhand()).isEmpty()) {
            evorsioChainingBreak = true;
            try {
                RayTraceResult rtr = MiscUtils.rayTraceLook(pl);
                if(rtr != null) {
                    EnumFacing faceHit = rtr.sideHit;
                    if(faceHit != null) {
                        ev.breakBlocksPlane((EntityPlayerMP) pl, faceHit, event.getWorld(), event.getPos());
                    }
                }
            } finally {
                evorsioChainingBreak = false;
            }
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if(event.getEntityLiving().world.isRemote) return;

        if(event.getEntityLiving() != null && event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer pl = (EntityPlayer) event.getEntityLiving();
            CapeEffectDiscidia cd = ItemCape.getCapeEffect(pl, Constellations.discidia);
            if(cd != null) {
                cd.writeLastAttackDamage(event.getAmount());
            }
            CapeEffectArmara ca = ItemCape.getCapeEffect(pl, Constellations.armara);
            if(ca != null) {
                if(ca.shouldPreventDamage(event.getSource(), false)) {
                    event.setCanceled(true);
                    return;
                }
            }
            if(event.getSource().isFireDamage()) {
                CapeEffectFornax cf = ItemCape.getCapeEffect(pl, Constellations.fornax);
                if(cf != null) {
                    cf.healFor(pl, event.getAmount());
                    float mul = cf.getDamageMultiplier();
                    if(mul <= 0) {
                        event.setCanceled(true);
                        return;
                    } else {
                        event.setAmount(event.getAmount() * mul);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        if(event.getEntity().getEntityWorld().isRemote) return;

        DamageSource ds = event.getSource();
        if(ds.getTrueSource() != null && ds.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer pl = (EntityPlayer) ds.getTrueSource();
            if(!(pl instanceof EntityPlayerMP)) return;
            if(MiscUtils.isPlayerFakeMP((EntityPlayerMP) pl)) return;

            CapeEffectEvorsio ev = ItemCape.getCapeEffect(pl, Constellations.evorsio);
            if(ev != null) {
                ev.deathAreaDamage(ds, event.getEntityLiving());
            }
        }
    }

    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if(discidiaChainingAttack) return;
        if(event.getEntityLiving().world.isRemote) return;

        DamageSource ds = event.getSource();
        if(ds.getTrueSource() != null && ds.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) ds.getTrueSource();
            if(!(attacker instanceof EntityPlayerMP)) return;
            if(MiscUtils.isPlayerFakeMP((EntityPlayerMP) attacker)) return;

            CapeEffectDiscidia cd = ItemCape.getCapeEffect(attacker, Constellations.discidia);
            if(cd != null) {
                double added = cd.getLastAttackDamage();

                discidiaChainingAttack = true;
                try {
                    attacker.attackEntityFrom(DamageSource.causePlayerDamage(attacker), (float) added);
                    attacker.attackEntityFrom(CommonProxy.dmgSourceStellar, (float) (added / 2));
                } finally {
                    discidiaChainingAttack = false;
                }
            }
            CapeEffectPelotrio pel = ItemCape.getCapeEffect(attacker, Constellations.pelotrio);
            if (pel != null && !attacker.getHeldItemMainhand().isEmpty() && rand.nextFloat() < pel.getChanceSpawnSword()) {
                BlockPos at = attacker.getPosition().up();
                EntitySpectralTool esp = new EntitySpectralTool(
                        attacker.getEntityWorld(), at, new ItemStack(Items.DIAMOND_SWORD),
                        EntitySpectralTool.ToolTask.createAttackTask());
                attacker.getEntityWorld().spawnEntity(esp);
            }
        }
    }

    private void tickFornaxMelting(EntityPlayer pl) {
        if(pl.isBurning()) {
            CapeEffectFornax cf = ItemCape.getCapeEffect(pl, Constellations.fornax);
            if(cf != null) {
                cf.attemptMelt(pl);
            }
        }
    }

    private void tickAevitasEffect(EntityPlayer pl) {
        CapeEffectAevitas cd = ItemCape.getCapeEffect(pl, Constellations.aevitas);
        if(cd != null) {
            float potency = cd.getPotency();
            float range = cd.getRange();
            if(rand.nextFloat() < potency) {
                World w = pl.getEntityWorld();
                AxisAlignedBB bb = new AxisAlignedBB(-range, -range, -range, range, range, range);
                bb.offset(pl.posX, pl.posY, pl.posZ);
                Predicate<Entity> pr = EntitySelectors.NOT_SPECTATING.and(EntitySelectors.IS_ALIVE);
                List<EntityPlayer> players = w.getEntitiesWithinAABB(EntityPlayer.class, bb, pr::test);
                for (EntityPlayer player : players) {
                    player.heal(0.8F);
                    player.getFoodStats().addStats(2, 0.4F);
                }
            }
            if(rand.nextFloat() < cd.getTurnChance()) {
                int x = Math.round(-range + 1 + (2 * range * rand.nextFloat()));
                int y = Math.round(-range + 1 + (2 * range * rand.nextFloat()));
                int z = Math.round(-range + 1 + (2 * range * rand.nextFloat()));
                BlockPos at = pl.getPosition().add(x, y, z);
                IBlockState state = pl.getEntityWorld().getBlockState(at);
                if(Plants.matchesAny(state)) {
                    state = Plants.getAnyRandomState();
                    pl.getEntityWorld().setBlockState(at, state);
                    PktParticleEvent ev = new PktParticleEvent(PktParticleEvent.ParticleEventType.CE_CROP_INTERACT, at);
                    PacketChannel.CHANNEL.sendToAllAround(ev, PacketChannel.pointFromPos(pl.getEntityWorld(), at, 16));
                } else {
                    CropHelper.GrowablePlant growable = CropHelper.wrapPlant(pl.getEntityWorld(), at);
                    if(growable != null) {
                        growable.tryGrow(pl.getEntityWorld(), rand);
                        PktParticleEvent ev = new PktParticleEvent(PktParticleEvent.ParticleEventType.CE_CROP_INTERACT, at);
                        PacketChannel.CHANNEL.sendToAllAround(ev, PacketChannel.pointFromPos(pl.getEntityWorld(), at, 16));
                    }
                }
            }
        }
    }

    private void tickArmaraWornEffect(EntityPlayer pl) {
        CapeEffectArmara ca = ItemCape.getCapeEffect(pl, Constellations.armara);
        if(ca != null) {
            ca.wornTick();
        }
    }

    @Override
    public void tick(TickEvent.Type type, Object... context) {
        switch (type) {
            case WORLD:
                break;
            case PLAYER:
                EntityPlayer pl = (EntityPlayer) context[0];
                Side side = (Side) context[1];
                if(side == Side.SERVER) {
                    if(!(pl instanceof EntityPlayerMP)) return;
                    if(MiscUtils.isPlayerFakeMP((EntityPlayerMP) pl)) return;

                    tickAevitasEffect(pl);
                    tickFornaxMelting(pl);
                    tickArmaraWornEffect(pl);
                }
                break;
            case CLIENT:
                break;
            case SERVER:
                break;
            case RENDER:
                break;
        }
    }

    @Override
    public EnumSet<TickEvent.Type> getHandledTypes() {
        return EnumSet.of(TickEvent.Type.PLAYER);
    }

    @Override
    public boolean canFire(TickEvent.Phase phase) {
        return phase == TickEvent.Phase.END;
    }

    @Override
    public String getName() {
        return "Cape-EventHandler";
    }

}
