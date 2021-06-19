package qwertzite.barostrain.core;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import qwertzite.barostrain.mod.PacketBaroStrain;
import qwertzite.barostrain.network.AbstractPacket;

public class BsExplosions {
	private static SimpleNetworkWrapper networkHandler;
	
	/**
	 * Call this method on init.
	 * @param networkHandler
	 * @param next id the next ID that can be used to register packets.
	 * @return next id for registering packet.
	 */
	public static int onInit(SimpleNetworkWrapper networkHandler, int nextid) {
		BsExplosions.networkHandler = networkHandler;
		registerPacket(PacketBaroStrain.class, nextid++);
		return nextid;
	}
	
	private static void registerPacket(Class<? extends AbstractPacket> packetClass, int id) {
		@SuppressWarnings("unchecked")
		Class<AbstractPacket> handler = (Class<AbstractPacket>)packetClass;
		networkHandler.registerMessage(packetClass, handler, id, Side.CLIENT);
	}
	
	/**
	 * Call this method on server side.
	 * @return The created explosion on server side and null on client side.
	 */
	public static BSExplosionBase explode(World world, @Nullable Entity entityIn, double x, double y, double z, float strength) {
		if (world.isRemote) return null;
		BSExplosionBase explosion = new BSExplosionBase(world, entityIn, x, y, z, strength);
		explosion.doExplosionA();
		explosion.doExplosionB(false);
		for (EntityPlayer entityplayer : world.playerEntities) {
			if (entityplayer.getDistanceSq(x, y, z) < 4096.0D) {
				networkHandler.sendTo(new PacketBaroStrain(explosion, (EntityPlayerMP) entityplayer), (EntityPlayerMP) entityplayer);
			}
		}
		return explosion;
	}
	
}
