package qwertzite.barostrain.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import qwertzite.barostrain.util.BsMath;
import qwertzite.barostrain.util.BsModLog;

public class BSExplosionBase extends Explosion {
	public static final double ERR = 0.00001d;
//	private final Random random;
	private final World world;
	private final double x;
	private final double y;
	private final double z;
	private final Entity exploder;
//	private final float size;
	private final Map<BlockPos, Vec3d> affectedBlockBlasts;
	private final Map<EntityPlayer, Vec3d> playerKnockbackMap;
	private final Vec3d position;
	
	private final float intencity;

//	@SideOnly(Side.CLIENT)
//	public BSExplosionBase(World worldIn, Entity entityIn, double x, double y, double z, float size,
//			List<BlockPos> affectedPositions) {
//		this(worldIn, entityIn, x, y, z, size, affectedPositions);
//	}

	@SideOnly(Side.CLIENT)
	public BSExplosionBase(World worldIn, Entity entityIn, double x, double y, double z, float size, Map<BlockPos, Vec3d> affectedPositions) {
		this(worldIn, entityIn, x, y, z, size);
		this.affectedBlockBlasts.putAll(affectedPositions);
	}

	public BSExplosionBase(World worldIn, Entity entityIn, double x, double y, double z, float intencity) {
		super(worldIn, entityIn, x, y, z, intencity, false, true);
//		this.random = new Random();
		this.affectedBlockBlasts = new HashMap<>();
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
	 * Does the first part of the explosion (destroy blocks)
	 */
	@Override
	public void doExplosionA() {
		
		Set<PressureRay> raySet;
//		Stream0	Rayを生成する	RaySet
		raySet =PressureRay.seedRays(this.position, this.intencity, this.world);
		
//		Ray Setが空でなければStream1へ
		BlockStrainSimulator blockStrain = new BlockStrainSimulator(this.world, this.exploder, this);
		while (!raySet.isEmpty()) {
			raySet.parallelStream().forEach(blockStrain::rayTraceBlocks);
			raySet = blockStrain.destructionEval();
			System.out.println(blockStrain.affectedBlocks.size() + " " + raySet.size());
		}
		
		this.affectedBlockBlasts.putAll(blockStrain.getBlockBlastSpeed());
		// TODO: playerKnock back
	}

	/**
	 * Does the second part of the explosion (sound, particles, drop spawn)
	 */
	@Override
	public void doExplosionB(boolean spawnParticles) {
		this.world.playSound((EntityPlayer) null, this.x, this.y, this.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
				SoundCategory.BLOCKS, 4.0F,
				(1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F);
		
		if (this.intencity >= 2.0F) {
			this.world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
		} else {
			this.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
		}
		
		for (Map.Entry<BlockPos, Vec3d> e : this.affectedBlockBlasts.entrySet()) {
			BlockPos blockpos = e.getKey();
			IBlockState iblockstate = this.world.getBlockState(blockpos);
			Block block = iblockstate.getBlock();
			Vec3d dir = e.getValue();
			if (spawnParticles) {
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
				this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, (ax + this.x) / 2.0D,
						(ay + this.y) / 2.0D, (az + this.z) / 2.0D, nx, ny, nz);
				this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, ax, ay, az, nx, ny, nz);
				for (int i = 0; i < 16; i++) {
					this.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
							(float) blockpos.getX() + this.world.rand.nextFloat(),
							(float) blockpos.getY() + this.world.rand.nextFloat(),
							(float) blockpos.getZ() + this.world.rand.nextFloat(), dir.x, dir.y, dir.z, Block.getStateId(iblockstate));
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
	
	public static class PressureRay {
//		private static final int CHILD_SHIFT = 1;
//		private static final Vec3i[] CHILDS = new Vec3i[] {
//				new Vec3i(0, 1, 1), new Vec3i(0, 1, -1), new Vec3i(0, -1, -1), new Vec3i(0, -1, 1),
//				new Vec3i(1, 0, 1), new Vec3i(1, 0, -1), new Vec3i(-1, 0, -1), new Vec3i(-1, 0, 1),
//				new Vec3i(1, 1, 0), new Vec3i(1, -1, 0), new Vec3i(-1, -1, 0), new Vec3i(-1, 1, 0)};
		private static final Vec3d[][] ICOSAHEDRON;
		
		static {
			double a = 1.0d / MathHelper.sqrt(5.0d);
			double b = (1.0d - a) / 2.0d;
			double c = (1.0d + a) / 2.0d;
			double d = MathHelper.sqrt(b);
			double e = MathHelper.sqrt(c);
			Vec3d v0p = new Vec3d(0, 1, 0);
			Vec3d vp0 = new Vec3d(-2.0d*a, a, 0);
			Vec3d vp1 = new Vec3d(     -b, a, e);
			Vec3d vp2 = new Vec3d(      c, a, d);
			Vec3d vp3 = new Vec3d(      c, a,-d);
			Vec3d vp4 = new Vec3d(     -b, a,-e);
			Vec3d vn0 = vp0.subtractReverse(Vec3d.ZERO);
			Vec3d vn1 = vp1.subtractReverse(Vec3d.ZERO);
			Vec3d vn2 = vp2.subtractReverse(Vec3d.ZERO);
			Vec3d vn3 = vp3.subtractReverse(Vec3d.ZERO);
			Vec3d vn4 = vp4.subtractReverse(Vec3d.ZERO);
			Vec3d v0n = new Vec3d(0, -1, 0);
			ICOSAHEDRON = new Vec3d[][] {
				{ v0p, vp0, vp1 }, { v0p, vp1, vp2 }, { v0p, vp2, vp3 }, { v0p, vp3, vp4 }, { v0p, vp4, vp0 },
				{ vn2, vn3, vp0 }, { vp0, vp1, vn3 },
				{ vn3, vn4, vp1 }, { vp1, vp2, vn4 },
				{ vn4, vn0, vp2 }, { vp2, vp3, vn0 },
				{ vn0, vn1, vp3 }, { vp3, vp4, vn1 },
				{ vn1, vn2, vp4 }, { vp4, vp0, vn2 },
				{ vn2, vn3, v0n }, { vn3, vn4, v0n }, { vn4, vn0, v0n }, { vn0, vn1, v0n }, { vn1, vn2, v0n }
				};
		}
		
		private World world;
		private double intencity;
		
		private double division; // multiply this to obtain actual force. これ倍する
		private double radius;
		public Vec3d origin;
		private Vec3d posPrev;
		public Vec3d[] seed;
		private Vec3d posNext;
		/** Distance travelled from the origin to {@code prevPos} */
		private double dist; // cumulative distance.
		
		public RayTraceResult hit;
		private double distFromPrevToHit;
		private double hitForce = -1;
		
		public PressureRay(float intencity, float division, Vec3d origin, Vec3d[] seed, World world) {
			this(intencity, division, 1.0d, origin, Vec3d.ZERO, seed, world, 0.0d);
		}
		
		public PressureRay(double intencity, double division, double radius, Vec3d origin, Vec3d posPrev, Vec3d[] seed, World world, double dist) {
			this.world = world;
			this.intencity = intencity;
			this.radius = radius;
			this.division = division;
			this.origin = origin;
			this.posPrev = posPrev;
			this.seed = seed;
			this.posNext = BsMath.average(seed[0], seed[1], seed[2]);
			this.dist = dist;
//			RAYS.add(this);
		}
		public static final Set<PressureRay> RAYS = Collections.synchronizedSet(new HashSet<>());
		
		public static Set<PressureRay> seedRays(Vec3d origin, float intencity, World world) {
			float division = 1.0f * 5f;
			return Stream.of(ICOSAHEDRON).parallel()
					.map(vs -> new PressureRay(intencity, division, origin, vs, world))
					.collect(Collectors.toSet());
		}
		
		// ==== step 1: Ray trace　レイトレース ====
		
		public void setTraceResult(RayTraceResult result) {
			this.hit = result;
			this.distFromPrevToHit = this.distFromPrev(result.hitVec);
			this.hitForce =  this.pressureAt(result.hitVec);
			if (this.hitForce < 0.0d) this.hitForce = 0.0d;
		}
		
		// ==== step 3: 次のバッチに向けて ====
		
		public Stream<PressureRay> nextStep() {
			double dist = BsMath.distance(this.posNext, this.posPrev);
			if (this.pressureAt(dist) <= 0.0d) {
				return Stream.empty();
			}
			Vec3d prev = this.posNext;
			double radius = this.radius * 2.0d;
			Vec3d ns01 = BsMath.sumScale(seed[0], seed[1], radius);
			Vec3d ns12 = BsMath.sumScale(seed[1], seed[2], radius);
			Vec3d ns20 = BsMath.sumScale(seed[2], seed[0], radius);
			Vec3d ns00 = seed[0].scale(2.0d);
			Vec3d ns11 = seed[1].scale(2.0d);
			Vec3d ns22 = seed[2].scale(2.0d);
			return Stream.of(
					new PressureRay(this.intencity, this.division/4, radius, this.origin, prev, new Vec3d[] { ns01, ns12, ns20 }, this.world, this.dist + dist),
					new PressureRay(this.intencity, this.division/4, radius, this.origin, prev, new Vec3d[] { ns00, ns01, ns20 }, this.world, this.dist + dist),
					new PressureRay(this.intencity, this.division/4, radius, this.origin, prev, new Vec3d[] { ns11, ns12, ns01 }, this.world, this.dist + dist),
					new PressureRay(this.intencity, this.division/4, radius, this.origin, prev, new Vec3d[] { ns22, ns20, ns12 }, this.world, this.dist + dist));
		}
		
		// reflection:  爆発体制の1/10をブロックのモーメントとする
		public Stream<PressureRay> reflection(double tranRate, double resistance) {
			if (this.hitForce <= 0.0d) return Stream.empty();
			if (this.hit.hitVec.distanceTo(this.origin.add(posPrev)) <= 0.01d) return Stream.empty();
			
			double transmission = division * tranRate / (1.0d + resistance*0.1d);
			double reflection = division * (1.0d - tranRate);
			Vec3d relHit = this.hit.hitVec.subtract(this.origin);
			PressureRay trans;
			PressureRay refl;
			if (transmission > 0.0d) {
				trans = new PressureRay(this.intencity, transmission, this.radius, this.origin, relHit, this.seed, this.world, this.dist + this.distFromPrevToHit);
			} else {
				trans = null;
			}
			Vec3d origin; // x軸　反射面を中心に反転
			Vec3d[] seeds; // x軸反転
			switch (this.hit.sideHit.getAxis()) {
			case X:
				origin = this.origin.addVector(2*relHit.x, 0, 0);
				seeds = BsMath.invert3X(this.seed);
				break;
			case Y:
				origin = this.origin.addVector(0, 2*relHit.y, 0);
				seeds = BsMath.invert3Y(this.seed);
				break;
			case Z:
				origin = this.origin.addVector(0, 0, 2*relHit.z);
				seeds = BsMath.invert3Z(this.seed);
				break;
			default:
			BsModLog.warn("Unknown hit side axis! {}", this.hit.sideHit.getAxis());
			assert(false);
			origin = null;
			seeds = null;
			}
//			System.out.println("" + this.origin + " " + this.hit.hitVec + this.hit.sideHit.getAxis() + " " + origin);
			refl = new PressureRay(this.intencity, reflection, this.radius, origin, this.hit.hitVec.subtract(origin), seeds, this.world, this.dist + this.distFromPrevToHit);
			if (trans == null) {
				return Stream.of(refl);
			} else {
				return Stream.of(refl, trans);
			}
		}
		
		// ==== util ====
		
		public double distFromPrev(Vec3d absPos) {
			double dx = absPos.x - this.posPrev.x - this.origin.x;
			double dy = absPos.y - this.posPrev.y - this.origin.y;
			double dz = absPos.z - this.posPrev.z - this.origin.z;
			return MathHelper.sqrt(dx*dx + dy*dy + dz*dz);
		}

		public double pressureAt(Vec3d absPos) {
			return this.pressureAt(this.distFromPrev(absPos));
		}
		
		public double pressureAt(double distFromFrom) { // これが間違っている？
			final double P = MathHelper.sqrt(intencity);
			final double Q = 0.5d*P + 0.5d;
			final double x1 = 2*Q;
			final double x2 = (2 + MathHelper.SQRT_2) * Q;
			final double y1 = P*Math.exp(-2)*4*Q*Q;
			final double y2 = P*Math.exp(-2-MathHelper.SQRT_2) * (6+4*MathHelper.SQRT_2) * Q * Q;
			double res = (y2 - y1)/(x2 - x1)*(this.dist + distFromFrom - x1) + y1;
			if (res < 0) res = 0.0d;
			else res *= this.division;
			return res;
		}
		
		public Vec3d getAbsFrom() {
			return BsMath.add(this.origin, this.posPrev);
		}
		
		public Vec3d getAbsTo() {
			return BsMath.add(this.origin, this.posNext);
		}
//		
//		public RayTraceResult getTraceResult() {
//			return this.hit;
//		}
		
		public double getHitPressure() {
			return this.hitForce;
		}
		
		public Vec3d getDirection() {
			return BsMath.subtract(this.posNext, this.posPrev).normalize();
		}
	}
	
	public static class BlockStrainSimulator {
		private final World world;
		private final Entity exploder;
		private final BSExplosionBase explosion;

		private Map<BlockPos, Set<PressureRay>> rayMap = Collections.synchronizedMap(new HashMap<>()); // 今回のバッチのray
		private Set<PressureRay> pendingRays = new HashSet<>();
		
		/** Force currently applied */
		private Map<BlockPos, Double> blockStatus = Collections.synchronizedMap(new HashMap<>());
		private EnumMap<Axis, AxisStrain> axis = new EnumMap<>(Axis.class);
		private AxisStrain axisX;
		private AxisStrain axisY;
		private AxisStrain axisZ;
		
		/** Result and blasting direction. */
		private Map<BlockPos, Vec3d> affectedBlocks = new HashMap<>();
		private Object2DoubleMap<BlockPos> resistanceMap = new Object2DoubleOpenHashMap<BlockPos>();
		
		public BlockStrainSimulator(World world, Entity exploder, BSExplosionBase explosion) {
			this.world = world;
			this.exploder = exploder;
			this.explosion = explosion;
			this.axis.put(Axis.X, this.axisX = new AxisStrain(world, this.exploder, this.explosion, Axis.X));
			this.axis.put(Axis.Y, this.axisY = new AxisStrain(world, this.exploder, this.explosion, Axis.Y));
			this.axis.put(Axis.Z, this.axisZ = new AxisStrain(world, this.exploder, this.explosion, Axis.Z));
		}
		
		// ==== STEP 1: Ray Process ====
		
		/**
		 * 
		 * @param ray
		 */
		public void rayTraceBlocks(PressureRay ray) {
			Vec3d from = ray.getAbsFrom();
			Vec3d to = ray.getAbsTo();
			
			// TODO: damage entity, player knock back map
			
			double dx = to.x - from.x;
			double dy = to.y - from.y;
			double dz = to.z - from.z;
			double dn = MathHelper.sqrt(dx*dx + dy*dy + dz*dz);
			double nx = dx/dn;
			double ny = dy/dn;
			double nz = dz/dn;
			
			double pressure = ray.pressureAt(0.0d);
			BlockPos pos = new BlockPos(from);
			RayTraceResult trace = this.pollBlockAt(pos, to, from, ray); // 内部の衝突が起こるかを確認
			if (trace != null) {
				ray.setTraceResult(trace);
				this.addCollidingRay(pos, ray, true);
				return; // 衝突した場合
			}
			
			for (double len = 0.3d; len < dn-0.1d; len += 0.3d) {
				pressure = ray.pressureAt(len);
				if (pressure <= 0.0d) return; // 消滅した場合
				int ix = MathHelper.floor(from.x + nx * len);
				int iy = MathHelper.floor(from.y + ny * len);
				int iz = MathHelper.floor(from.z + nz * len);
				if (ix == pos.getX() && iy == pos.getY() && iz == pos.getZ()) continue;
				pos = new BlockPos(ix, iy, iz);
				trace = this.pollBlockAt(pos, from, to, ray);
				if (trace != null) {
					ray.setTraceResult(trace);
					this.addCollidingRay(pos, ray, false);
					return; // 衝突した場合
				}
			}
			
			pressure = ray.pressureAt(dn);
			if (pressure <= 0.0d) return; // 消失した場合
			BlockPos lpos = new BlockPos(to);
			if (lpos.equals(pos)) {
				this.addPendingRay(ray);
				return; // 衝突しなかった場合
			}
			trace = this.pollBlockAt(lpos, from, to, ray);
			if (trace == null) {
				this.addPendingRay(ray);
				return; // 衝突しなかった場合
			} else {
				ray.setTraceResult(trace);
				this.addCollidingRay(lpos, ray, false);
				return; // 衝突した場合，
			}
		}

		private void addCollidingRay(BlockPos pos, PressureRay ray, boolean reversed) {
			if (!rayMap.containsKey(pos)) {
				rayMap.put(pos, new HashSet<>());
			}
			rayMap.get(pos).add(ray);
			EnumFacing facing = ray.hit.sideHit;
			double force = ray.getHitPressure();
			if (reversed) force *= -1;
			this.axis.get(facing.getAxis()).applyForce(pos, facing, force);
		}
		
		private synchronized void addPendingRay(PressureRay ray) {
			this.pendingRays.add(ray);
		}
		
		/**
		 * If there is a destroyed block and ray intersects with the block collision BB, adds blast direction vector.<br>
		 * Else, returns a ray trace result if the ray intersects with the BB.
		 * @param blockpos
		 * @param from
		 * @param to
		 * @return
		 */
		private RayTraceResult pollBlockAt(BlockPos blockpos, Vec3d from, Vec3d to, PressureRay ray) {
			IBlockState iblockstate = this.world.getBlockState(blockpos);
			if (this.affectedBlocks.containsKey(blockpos)) {
				if (iblockstate.getBlock().canCollideCheck(iblockstate, true)) {
					RayTraceResult trace = iblockstate.collisionRayTrace(this.world, blockpos, from, to);
					if (trace != null) {
						double pressure = ray.pressureAt(trace.hitVec);
						if (pressure > 0.0d) {
							this.addBlastVec(blockpos, ray.getDirection().scale(pressure));
						}
					}
				}
				return null;
			}
			if ((iblockstate.getMaterial() == Material.PORTAL)
					|| iblockstate.getCollisionBoundingBox(this.world, blockpos) != Block.NULL_AABB) {
				if (iblockstate.getBlock().canCollideCheck(iblockstate, true)) {
					return iblockstate.collisionRayTrace(this.world, blockpos, from, to);
				}
			}
			return null;
		}
		
		/**
		 * Add the amount of traversing blast to destroyed blocks.
		 * @param pos
		 * @param vec
		 */
		private synchronized void addBlastVec(BlockPos pos, Vec3d vec) {
			this.affectedBlocks.put(pos, this.affectedBlocks.get(pos).add(vec));
		}
		
		// ==== STEP 2 ====
		
		/**
		 * Evaluate each block.
		 * @return
		 */
		public Set<PressureRay> destructionEval() {
			this.axis.values().parallelStream().forEach(ax -> ax.evaluate());
			// TODO: 破壊されたブロックと余剰圧力を算出する　<BlockPos, Vec3d>
			
			Set<PressureRay> result = Stream.concat(this.pendingRays.parallelStream().flatMap(r -> r.nextStep()),
					this.rayMap.entrySet().parallelStream()
					.flatMap(e -> {
						BlockPos blockpos = e.getKey();
						if (!this.blockStatus.containsKey(blockpos)) this.blockStatus.put(blockpos, 0.0d);
						double force = this.blockStatus.get(blockpos);
						double batchSum = 0.0d;
						Set<PressureRay> rays = e.getValue();
						for (PressureRay ray : rays) {
							batchSum += ray.getHitPressure();
						}
						
						IBlockState iblockstate = this.world.getBlockState(blockpos);
						double resistance = this.exploder != null ?
								this.exploder.getExplosionResistance(this.explosion, this.world, blockpos, iblockstate)
								: iblockstate.getBlock().getExplosionResistance(this.world, blockpos, (Entity) null, this.explosion);
						
						// TODO: 個々の破壊判定を，置き換える
						double resRate = (batchSum + force - resistance) / batchSum;
						if (resRate >= 0.0d && (this.exploder == null || this.exploder.canExplosionDestroyBlock(this.explosion,
								this.world, blockpos, iblockstate, (float) batchSum))) {
							double vx = 0.0d;
							double vy = 0.0d;
							double vz = 0.0d;
							for (PressureRay ray : rays) {
								double hitP = ray.getHitPressure();
								Vec3d hitD = ray.getDirection();
								vx += hitD.x * hitP * resRate;
								vy += hitD.y * hitP * resRate;
								vz += hitD.z * hitP * resRate;
							}
							this.markAsAffected(blockpos, new Vec3d(vx, vy, vz), resistance);
							this.blockStatus.remove(blockpos);
							// Stream 透過分を得る，反射分を得る
						} else {
							resRate = 0.0d;
							// 反射分のStream
							this.blockStatus.put(blockpos, force + batchSum);
						}
						double transmittance = resRate;
						return rays.parallelStream().flatMap(r -> r.reflection(transmittance, resistance));
					}))
					.collect(Collectors.toSet());
			
			this.pendingRays.clear();
			this.rayMap.clear();
			return result;
		}
		
		private synchronized void markAsAffected(BlockPos pos, Vec3d vel, double resistance) {
			this.affectedBlocks.put(pos, vel);
			this.resistanceMap.put(pos, resistance);
		}
		
		// ==== final step: result retrieval ====
		
		/**
		 * 
		 * @return block and block velocity.
		 */
		public Map<BlockPos, Vec3d> getBlockBlastSpeed() {
			return this.affectedBlocks.entrySet().parallelStream().collect(Collectors.toMap(e -> e.getKey(),
					e -> {
						Vec3d force = e.getValue();
						final double sound = 340.0d * 0.1d / 5.0d; // m=0.1B, s=5t -> 340m/s = 340*0.1b/(5t))
						double resistance = this.resistanceMap.getDouble(e.getKey());
						double norm = force.lengthVector();
						return force.scale(sound / (norm + (1.0d+resistance)*sound));
					}));
		}
	}
	
	public static boolean isZero(double v) {
		return v > -ERR && v < ERR;
	}
}