package qwertzite.barostrain.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import qwertzite.barostrain.network.AbstractPacket;
import qwertzite.barostrain.network.PacketToClient;

public class PacketBaroStrain extends AbstractPacket implements PacketToClient {
	
	private Vec3d position;
	private float intencity;
	private Map<BlockPos, Vec3d> affectedBlockBlast;
	private Set<BlockPos> wiggledBlockPos;
	private Set<BlockPos> hitBlocks;
	private double motionX;
	private double motionY;
	private double motionZ;
	
	public PacketBaroStrain() {}
	
	public PacketBaroStrain(BSExplosionBase explosion, EntityPlayerMP entityplayer) {
		this.position = explosion.getPosition();
		this.intencity = explosion.getIntencity();
		this.affectedBlockBlast = explosion.getAffectedBlockBlasts();
		this.wiggledBlockPos = explosion.getWiggledBlockPos();
		this.hitBlocks = explosion.getHitParticleBlockPos();
		Vec3d knock = explosion.getPlayerKnockbackMap().get(entityplayer);
		if (knock != null) {
			this.motionX = knock.x;
			this.motionY = knock.y;
			this.motionZ = knock.z;
		}
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		this.position = new Vec3d(buf.readFloat(), buf.readFloat(), buf.readFloat());
		this.intencity = buf.readFloat();
		
		int x = (int) this.position.x;
		int y = (int) this.position.y;
		int z = (int) this.position.z;
		
		final int size = buf.readInt();
		this.affectedBlockBlast = new HashMap<>();
		for (int i = 0; i < size; i++) {
			BlockPos blockpos = new BlockPos(buf.readByte() + x, buf.readByte() + y, buf.readByte() + z);
			Vec3d vec = new Vec3d(buf.readFloat(), buf.readFloat(), buf.readFloat());
			this.affectedBlockBlast.put(blockpos, vec);
		}
		
		final int wiggled = buf.readInt();
		this.wiggledBlockPos = new HashSet<>();
		for (int i = 0; i < wiggled; i++) {
			BlockPos blockpos = new BlockPos(buf.readByte() + x, buf.readByte() + y, buf.readByte() + z);
			this.wiggledBlockPos.add(blockpos);
		}
		
		final int hit = buf.readInt();
		this.hitBlocks = new HashSet<>();
		for (int i = 0; i < hit; i++) {
			BlockPos blockpos = new BlockPos(buf.readByte() + x, buf.readByte() + y, buf.readByte() + z);
			this.hitBlocks.add(blockpos);
		}
		
		this.motionX = buf.readFloat();
		this.motionY = buf.readFloat();
		this.motionZ = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeFloat((float) this.position.x);
		buf.writeFloat((float) this.position.y);
		buf.writeFloat((float) this.position.z);
		buf.writeFloat(this.intencity);
		
		int x = (int) this.position.x;
		int y = (int) this.position.y;
		int z = (int) this.position.z;
		buf.writeInt(this.affectedBlockBlast.size());
		for (Map.Entry<BlockPos, Vec3d> e : this.affectedBlockBlast.entrySet()) {
			BlockPos blockpos = e.getKey();
			buf.writeByte(blockpos.getX() - x);
			buf.writeByte(blockpos.getY() - y);
			buf.writeByte(blockpos.getZ() - z);
			Vec3d vec = e.getValue();
			buf.writeFloat((float) vec.x);
			buf.writeFloat((float) vec.y);
			buf.writeFloat((float) vec.z);
		}
		buf.writeInt(this.wiggledBlockPos.size());
		for (BlockPos pos : this.wiggledBlockPos) {
			buf.writeByte(pos.getX() - x);
			buf.writeByte(pos.getY() - y);
			buf.writeByte(pos.getZ() - z);
		}
		buf.writeInt(this.hitBlocks.size());
		for (BlockPos pos : this.hitBlocks) {
			buf.writeByte(pos.getX() - x);
			buf.writeByte(pos.getY() - y);
			buf.writeByte(pos.getZ() - z);
		}
		buf.writeFloat((float) this.motionX);
		buf.writeFloat((float) this.motionY);
		buf.writeFloat((float) this.motionZ);
	}

	@Override
	public IMessage handleClientSide(EntityPlayer player) {
		this.checkThreadAndEnqueue(this, player, Minecraft.getMinecraft());
		BSExplosionBase explosion = new BSExplosionBase(Minecraft.getMinecraft().world, (Entity) null,
				this.position.x, this.position.y,
				this.position.z, this.intencity, this.affectedBlockBlast, this.wiggledBlockPos, this.hitBlocks);
		explosion.doExplosionB(true);
		Minecraft.getMinecraft().player.motionX += this.motionX;
		Minecraft.getMinecraft().player.motionY += this.motionY;
		Minecraft.getMinecraft().player.motionZ += this.motionZ;
		return null;
	}
}
