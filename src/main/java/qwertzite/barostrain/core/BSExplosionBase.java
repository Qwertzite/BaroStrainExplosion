package qwertzite.barostrain.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import qwertzite.barostrain.core.common.ParticleBlockFragment;

public class BSExplosionBase extends Explosion {
	public static final double ERR = 0.00001d;
//	private final Random random;
	private final World world;
	private final double x;
	private final double y;
	private final double z;
	private final Entity exploder;
	// ==== cache ====
	private final Set<Entity> entityCache = new HashSet<>();
	
	// ==== results ====
	private final Map<BlockPos, Vec3d> affectedBlockBlasts;
	private final Set<BlockPos> wiggledBlocks;
	private final Set<BlockPos> hitBlocks;
	private final Map<EntityPlayer, Vec3d> playerKnockbackMap;
	private final Vec3d position;
	
	private final float intencity;

//	@SideOnly(Side.CLIENT)
//	public BSExplosionBase(World worldIn, Entity entityIn, double x, double y, double z, float size,
//			List<BlockPos> affectedPositions) {
//		this(worldIn, entityIn, x, y, z, size, affectedPositions);
//	}

	@SideOnly(Side.CLIENT)
	public BSExplosionBase(World worldIn, Entity entityIn, double x, double y, double z, float size,
			Map<BlockPos, Vec3d> affectedPositions, Set<BlockPos> wiggledBlocks, Set<BlockPos> hitBlocks) {
		this(worldIn, entityIn, x, y, z, size);
		this.affectedBlockBlasts.putAll(affectedPositions);
		this.wiggledBlocks.addAll(wiggledBlocks);
		this.hitBlocks.addAll(hitBlocks);
	}

	public BSExplosionBase(World worldIn, Entity entityIn, double x, double y, double z, float intencity) {
		super(worldIn, entityIn, x, y, z, intencity, false, true);
//		this.random = new Random();
		this.affectedBlockBlasts = new HashMap<>();
		this.wiggledBlocks = new HashSet<>();
		this.hitBlocks = new HashSet<>();
		this.playerKnockbackMap = Maps.<EntityPlayer, Vec3d>newHashMap();
		this.world = worldIn;
		this.exploder = entityIn;
		this.x = x;
		this.y = y;
		this.z = z;
		this.position = new Vec3d(this.x, this.y, this.z);
		this.intencity = intencity;
	}

	/**
	 * Does the first part of the explosion (evaluate block destruction. Runs only on server side.)
	 */
	@Override
	public void doExplosionA() {
		this.initEntityMap();
		
		BlockStrainSimulator blockStrain = new BlockStrainSimulator(this.world, this.exploder, this);
		
		Set<PressureRay> raySet; // 最初のrayを生成する
		raySet =PressureRay.seedRays(this.position, this.intencity, this.world);
		
		while (!raySet.isEmpty()) {
			raySet.parallelStream().forEach(ray -> {
				Vec3d end = blockStrain.rayTraceBlocks(ray);
				damageEntity(ray, end);
			});
			raySet = blockStrain.destructionEval();
		}
		
		this.affectedBlockBlasts.putAll(blockStrain.getBlockBlastSpeed());
		this.wiggledBlocks.addAll(blockStrain.getWiggledBlocks());
		this.hitBlocks.addAll(blockStrain.getHitBlocks(this.world.rand));
	}
	
	// 影響されうる最大範囲のエンティティを取得する
	private void initEntityMap() {
		final double P = MathHelper.sqrt(this.intencity);
		final double Q = 0.5d*P + 0.5d;
		final double x1 = 2*Q;
		final double x2 = (2 + MathHelper.SQRT_2) * Q;
		final double y1 = P*Math.exp(-2)*4*Q*Q;
		final double y2 = P*Math.exp(-2-MathHelper.SQRT_2) * (6+4*MathHelper.SQRT_2) * Q * Q;
		final double maxDist = x1 - y1 * (x2 - x1) / (y2 - y1);
		if (maxDist <= 0.0d) return;
		AxisAlignedBB aabb = new AxisAlignedBB(this.x - maxDist, this.y - maxDist, this.z - maxDist, this.x + maxDist, this.y + maxDist, this.z + maxDist);
		this.entityCache.addAll(this.world.getEntitiesWithinAABBExcludingEntity(null, aabb));
	}
	
	/** 並列呼び出し */
	private void damageEntity(PressureRay ray, Vec3d to) {
		this.entityCache.stream().forEach(e -> {
			e.getEntityBoundingBox().calculateIntercept(ray.getAbsFrom(), to);
		});
		// TODO:
	}
	
	private void addEntityBlastVec(Entity entity, Vec3d vec) {
		double len = vec.lengthVector();
		synchronized (this) {
			// TODO: add vec to blast direction
		}
		synchronized (this) {
			// TODO: increase damage for given entity.
		}
	}

	/**
	 * Does the second part of the explosion (play sound, create particles, destroy blocks and spawn drops)<br>
	 * Runs on both server and client sides.
	 */
	@Override
	public void doExplosionB(boolean spawnParticles) {
		this.world.playSound((EntityPlayer) null, this.x, this.y, this.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
				SoundCategory.BLOCKS, 4.0F,
				(1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F);
		
		if (this.intencity >= 2.0F) {
			this.world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D); // 強度に応じて大きさが変わるものを独自実装する？
		} else {
			this.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
		}
		
		if (spawnParticles) {
			for (int i = Math.round((this.intencity*this.intencity*this.intencity + 1) / 16); i >= 0; i--) {
				double px = this.world.rand.nextFloat()*2 - 1;
				double py = this.world.rand.nextFloat()*2 - 1;
				double pz = this.world.rand.nextFloat()*2 - 1;
				double sq = MathHelper.sqrt(px*px + py*py + pz*pz);
				if (sq == 0) continue;
				double vel = this.intencity * (double) (this.world.rand.nextFloat() * this.world.rand.nextFloat() + 0.1F) / 20 / sq;
				px *= vel;
				py *= vel;
				pz *= vel;
				this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, this.x, this.y, this.z, px, py, pz);
				this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.x, this.y, this.z, px, py, pz);
			}
		}
		
		for (Map.Entry<BlockPos, Vec3d> e : this.affectedBlockBlasts.entrySet()) {
			BlockPos blockpos = e.getKey();
			IBlockState iblockstate = this.world.getBlockState(blockpos);
			Block block = iblockstate.getBlock();
			Vec3d dir = e.getValue();
			if (spawnParticles) {
				ParticleManager effectRenderer = Minecraft.getMinecraft().effectRenderer;
			// 音速に近づいていく，resistanceに応じて変化する
				double ax = (double) ((float) blockpos.getX() + this.world.rand.nextFloat());
				double ay = (double) ((float) blockpos.getY() + this.world.rand.nextFloat());
				double az = (double) ((float) blockpos.getZ() + this.world.rand.nextFloat());
				double nx = ax - this.x;
				double ny = ay - this.y;
				double nz = az - this.z;
				double dn = (double) MathHelper.sqrt(nx * nx + ny * ny + nz * nz);
				nx = nx / dn;
				ny = ny / dn;
				nz = nz / dn;
				double vel = 0.5D / (dn / (double) this.intencity + 0.1D);
				vel = vel * (double) (this.world.rand.nextFloat() * this.world.rand.nextFloat() + 0.3F);
				nx = nx * vel;
				ny = ny * vel;
				nz = nz * vel;
				if (iblockstate.getMaterial() != Material.AIR) {
					for (int i = 0; i < 16; i++) { // vel = 1 ~ 0.01
						effectRenderer.addEffect(
								new ParticleBlockFragment(
										this.world, 
										(float) blockpos.getX() + this.world.rand.nextFloat(),
										(float) blockpos.getY() + this.world.rand.nextFloat(),
										(float) blockpos.getZ() + this.world.rand.nextFloat(),
										dir.x/2, dir.y/2, dir.z/2, iblockstate)); // ~ 10
					}
					this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, (ax + this.x) / 2.0D,
							(ay + this.y) / 2.0D, (az + this.z) / 2.0D, nx, ny, nz);
					this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, ax, ay, az, nx, ny, nz);
				}
			}
			
			if (iblockstate.getMaterial() != Material.AIR) {
				if (block.canDropFromExplosion(this)) {
					block.dropBlockAsItemWithChance(this.world, blockpos, this.world.getBlockState(blockpos),
							1.0F / this.intencity, 0);
				}
				block.onBlockExploded(this.world, blockpos, this);
			}
		}
		
		if (spawnParticles) {
			for (BlockPos pos : this.wiggledBlocks) {
				IBlockState iblockstate = this.world.getBlockState(pos);
				if (iblockstate.isOpaqueCube()) {
					if (!this.world.getBlockState(pos.offset(EnumFacing.DOWN)).isOpaqueCube()) {
						int blockstateid = Block.getStateId(iblockstate);
						this.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
								(float) pos.getX() + this.world.rand.nextFloat(),
								(float) pos.getY(),
								(float) pos.getZ() + this.world.rand.nextFloat(), 0, 0, 0, blockstateid);
					}
				}
			}
			for (BlockPos pos : this.hitBlocks) {
				if (!this.world.getBlockState(pos).isOpaqueCube()) {
					this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
							pos.getX() + this.world.rand.nextFloat(),
							pos.getY() + this.world.rand.nextFloat(),
							pos.getZ() + this.world.rand.nextFloat(), 0, 0, 0);
					this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
							pos.getX() + this.world.rand.nextFloat(),
							pos.getY() + this.world.rand.nextFloat(),
							pos.getZ() + this.world.rand.nextFloat(), 0, 0, 0);
				}
			}
		}
	}
	
	@Override
	public Map<EntityPlayer, Vec3d> getPlayerKnockbackMap() {
		return this.playerKnockbackMap;
	}

	@Override
	@Nullable
	public EntityLivingBase getExplosivePlacedBy() {
		if (this.exploder == null) {
			return null;
		} else if (this.exploder instanceof EntityTNTPrimed) {
			return ((EntityTNTPrimed) this.exploder).getTntPlacedBy();
		} else {
			return this.exploder instanceof EntityLivingBase ? (EntityLivingBase) this.exploder : null;
		}
	}

	@Override
	public void clearAffectedBlockPositions() {
		this.affectedBlockBlasts.clear();
	}

	@Override
	public List<BlockPos> getAffectedBlockPositions() {
		return Lists.newArrayList(this.affectedBlockBlasts.keySet());
	}

	@Override
	public Vec3d getPosition() {
		return this.position;
	}
	
	public float getIntencity() {
		return this.intencity;
	}
	
	public Map<BlockPos, Vec3d> getAffectedBlockBlasts() {
		return this.affectedBlockBlasts;
	}
	
	public Set<BlockPos> getWiggledBlockPos() { return this.wiggledBlocks; }
	public Set<BlockPos> getHitParticleBlockPos() { return this.hitBlocks; }
	
	public static boolean isZero(double v) {
		return -ERR < v && v < ERR;
	}
}