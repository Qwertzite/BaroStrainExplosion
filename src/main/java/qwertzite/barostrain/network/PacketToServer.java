package qwertzite.barostrain.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public interface PacketToServer {
	public IMessage handleServerSide(EntityPlayer player);
}
