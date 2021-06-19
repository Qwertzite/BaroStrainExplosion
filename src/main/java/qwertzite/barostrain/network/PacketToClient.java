package qwertzite.barostrain.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public interface PacketToClient {
    public IMessage handleClientSide(EntityPlayer player);
}
