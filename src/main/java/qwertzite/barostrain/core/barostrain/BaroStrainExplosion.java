package qwertzite.barostrain.core.barostrain;

import java.util.Map;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import qwertzite.barostrain.core.BSExplosionBase;

public class BaroStrainExplosion extends BSExplosionBase {
	
	@SideOnly(Side.CLIENT)
	public BaroStrainExplosion(World worldIn, Entity entityIn, double x, double y, double z, float size,
			Map<BlockPos, Vec3d> affectedPositions, Set<BlockPos> wiggledBlocks, Set<BlockPos> hitBlocks) {
		this(worldIn, entityIn, x, y, z, size);
//		this.affectedBlockBlasts.putAll(affectedPositions);
//		this.wiggledBlocks.addAll(wiggledBlocks);
//		this.hitBlocks.addAll(hitBlocks);
	}

	public BaroStrainExplosion(World worldIn, Entity entityIn, double x, double y, double z, float intencity) {
		super(worldIn, entityIn, x, y, z, intencity);
//		this.random = new Random();
//		this.affectedBlockBlasts = new HashMap<>();
//		this.wiggledBlocks = new HashSet<>();
//		this.hitBlocks = new HashSet<>();
//		this.playerKnockbackMap = Maps.<EntityPlayer, Vec3d>newHashMap();
//		this.entityBlast = new HashMap<>();
//		this.entityDamage = new Object2DoubleOpenHashMap<>();
	}
	
	@Override
	public void doExplosionA() {
		
		BaroRaySimulator simulator = new BaroRaySimulator(this);
		simulator.evaluate();
		
		// TODO:
	}
	

	
	
	
	
	@Override
	public void doExplosionB(boolean spawnParticles) {
		
	}

	
	
	// TODO: player knock back map
	// TODO: clearAffectedBlockPositions
	// TODO: getAffectedBlockPositions
	// TODO: getAffectedBlockBlast
	// TODO: getWiggledBlockPos
	// TODO: getHitParticleBlockPos
	
	
	// ======== getters setters ========
	public World getWorld() { return this.world; }
	public Entity getExploder() { return this.exploder; }
}
