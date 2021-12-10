package qwertzite.barostrain.core.common;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.world.World;
import qwertzite.barostrain.util.BsxMath;

public class ParticleBlockFragment extends ParticleDigging {
	private final double RATE = Math.log(Math.pow(0.5, 1.0 / 4));

	public ParticleBlockFragment(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn,
			double xSpeedIn, double ySpeedIn, double zSpeedIn, IBlockState state) {
		super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0, 0, 0, state);
		this.motionX = (Math.random()*0.4 + xSpeedIn) * (0.9 + Math.random() * 0.2D);
		this.motionY = (Math.random()*0.4 + ySpeedIn) * (0.9 + Math.random() * 0.2D);
		this.motionZ = (Math.random()*0.4 + zSpeedIn) * (0.9 + Math.random() * 0.2D);
		this.particleMaxAge = BsxMath.round(Math.log(1 - Math.random()*0.99) / RATE, Math.random());
		
		this.init();
	}

	@Override
	public void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;

		if (this.particleAge++ >= this.particleMaxAge) {
			this.setExpired();
		}

//		this.setParticleTextureIndex(7 - this.particleAge * 8 / this.particleMaxAge);
		this.motionY -= 0.06D * (double)this.particleGravity;
		this.move(this.motionX, this.motionY, this.motionZ);
		this.motionX *= 0.85D;
		this.motionY *= 0.85D;
		this.motionZ *= 0.85D;

		if (this.onGround) {
			this.motionX *= 0.7;
			this.motionZ *= 0.7;
		}
	}
}
