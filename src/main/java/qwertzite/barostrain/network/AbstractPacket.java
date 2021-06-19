package qwertzite.barostrain.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AbstractPacket implements IMessage, IMessageHandler<AbstractPacket, IMessage> {

	@Override
	public IMessage onMessage(AbstractPacket message, MessageContext ctx) {
		EntityPlayer player;
		IMessage reply = null;
		switch (FMLCommonHandler.instance().getEffectiveSide()) {
		case CLIENT:
			if (message instanceof PacketToClient) {
				player = this.getPlayer();
				if(player == null) break;
				try { reply = ((PacketToClient)message).handleClientSide(player); }
				catch (ThreadQuickExitException e) { /* Do nothing, quick exit. */ }
			}
			break;
		case SERVER:
			if (message instanceof PacketToServer) {
				player = ((NetHandlerPlayServer) ctx.netHandler).player;
				reply = ((PacketToServer)message).handleServerSide(player);
			}
			break;
		default:
		}
		return reply;
	}
	
	@SideOnly(Side.CLIENT)
	private EntityPlayer getPlayer() { return Minecraft.getMinecraft().player; }
	
	protected <T extends INetHandler> void checkThreadAndEnqueue(final PacketToClient packetIn, EntityPlayer player,
			IThreadListener scheduler) throws ThreadQuickExitException {
		if (!scheduler.isCallingFromMinecraftThread()) {
			scheduler.addScheduledTask(new Runnable() {
				@Override
				public void run() {
					packetIn.handleClientSide(player);
				}
			});
			throw ThreadQuickExitException.INSTANCE;
		}
	}
}
