package qwertzite.barostrain.core;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BlockStrainSimulator {
	private final World world;
	private final Entity exploder;
	private final BSExplosionBase explosion;
	
	// ==== ray-trace 処理 ====
	private Set<PressureRay> pendingRays = new HashSet<>(); // 今回は衝突しなかったray，分割処理を施して次の回に処理する
	
	// ==== FEM段階 ====
	private EnumMap<Axis, AxisStrain> axis = new EnumMap<>(Axis.class);
	@SuppressWarnings("unused")
	private AxisStrain axisX;
	@SuppressWarnings("unused")
	private AxisStrain axisY;
	@SuppressWarnings("unused")
	private AxisStrain axisZ;
	
	// ==== result ====
	/** Result and blasting direction. */
	private Map<BlockPos, Vec3d> affectedBlocks = new HashMap<>();
	private Object2DoubleMap<BlockPos> hitBlocks = new Object2DoubleOpenHashMap<>();
	
	// ==== cache ====
	private Object2DoubleMap<BlockPos> resistanceMap = new Object2DoubleOpenHashMap<BlockPos>();
	
	public BlockStrainSimulator(World world, Entity exploder, BSExplosionBase explosion) {
		this.world = world;
		this.exploder = exploder;
		this.explosion = explosion;
		this.axis.put(Axis.X, this.axisX = new AxisStrain(this, Axis.X));
		this.axis.put(Axis.Y, this.axisY = new AxisStrain(this, Axis.Y));
		this.axis.put(Axis.Z, this.axisZ = new AxisStrain(this, Axis.Z));
	}
	
	// ==== STEP 1: Ray Process ====
	
	/**
	 * ray の衝突に関する処理を行う．
	 * @param ray
	 * @return 末端 (衝突箇所 or トレースした最後の場所)
	 */
	public Vec3d rayTraceBlocks(PressureRay ray) {
		Vec3d from = ray.getAbsFrom();
		Vec3d to = ray.getAbsTo();
		
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
			return trace.hitVec; // 衝突した場合
		}
		
		for (double len = 0.3d; len < dn-0.1d; len += 0.3d) {
			pressure = ray.pressureAt(len);
			if (pressure <= 0.0d) return from.addVector(nx*len, ny*len, nz*len); // 消滅した場合
			int ix = MathHelper.floor(from.x + nx * len);
			int iy = MathHelper.floor(from.y + ny * len);
			int iz = MathHelper.floor(from.z + nz * len);
			if (ix == pos.getX() && iy == pos.getY() && iz == pos.getZ()) continue;
			pos = new BlockPos(ix, iy, iz);
			trace = this.pollBlockAt(pos, from, to, ray);
			if (trace != null) {
				ray.setTraceResult(trace);
				this.addCollidingRay(pos, ray, false);
				return trace.hitVec; // 衝突した場合
			}
		}
		
		pressure = ray.pressureAt(dn);
		if (pressure <= 0.0d) return to; // 消失した場合
		BlockPos lpos = new BlockPos(to);
		if (lpos.equals(pos)) {
			this.addPendingRay(ray);
			return to; // 衝突しなかった場合
		}
		trace = this.pollBlockAt(lpos, from, to, ray);
		if (trace == null) {
			this.addPendingRay(ray);
			return to; // 衝突しなかった場合
		} else {
			ray.setTraceResult(trace);
			this.addCollidingRay(lpos, ray, false);
			return trace.hitVec; // 衝突した場合，
		}
	}

	private void addCollidingRay(BlockPos pos, PressureRay ray, boolean reversed) {
		EnumFacing facing = ray.hit.sideHit;
		double force = ray.getHitPressure();
		if (reversed) force *= -1;
		this.axis.get(facing.getAxis()).applyForce(ray, pos, facing, force);
		BlockPos off = pos.offset(facing);
		synchronized (this) {
			this.hitBlocks.put(off, this.hitBlocks.getDouble(off) + force);
		}
	}
	
	/**
	 * 今回は衝突しなかったray, 分岐して次のステップへ
	 * @param ray
	 */
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
		if (this.affectedBlocks.containsKey(blockpos)) { // add blast vector to block velocity.
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
	private synchronized void addBlastVec(BlockPos pos, Vec3d vec) { // keyの追加は破壊処理の時のみなので気にする必要はない
		this.affectedBlocks.put(pos, this.affectedBlocks.get(pos).add(vec));
	}
	

	
	// ==== STEP 2 ====
	
	/**
	 * Evaluate each block.
	 * @return
	 */
	public Set<PressureRay> destructionEval() {
		// 軸毎に破壊判定されたブロックを集計，他の軸のstateに反映，最終的な破壊判定に追加する．１軸でも破壊判定されたらOK
		Set<BlockPos> newlyDestroyed = this.axis.values().stream()//.parallelStream()
				.flatMap(ax -> ax.evaluate().parallelStream())
				.collect(Collectors.toSet());
		this.axis.values().parallelStream().forEach(e -> e.checkBlocksDestroyed(newlyDestroyed));
		this.affectedBlocks.putAll(newlyDestroyed.parallelStream().collect(Collectors.toMap(pos -> pos, pos -> Vec3d.ZERO)));
		for (BlockPos pos: newlyDestroyed) {
			this.resistanceMap.remove(pos); // ついでに不要なキャッシュを消去
		}
//		this.hitBlocks.removeAll(newlyDestroyed);
//		System.out.println("Newly destroyed: " + newlyDestroyed.size() + " affected blocks: " + this.affectedBlocks.size());
		
		// 塊ごとに集計，各方位毎に次のPressureRayを算出
		
		Set<PressureRay> result = Stream.concat(
				this.pendingRays.parallelStream().flatMap(r -> r.nextStep()),
				this.axis.values().parallelStream().flatMap(ax -> ax.rayRefAndTr())).collect(Collectors.toSet());
		
		this.pendingRays.clear();
		this.axis.values().stream().forEach(ax -> ax.clear());
		return result;
	}
	
	public synchronized double getBlockResistanceAt(BlockPos pos) {
		if (this.affectedBlocks.containsKey(pos)) return 0.0d; // 破壊判定されている場合
		if (!this.resistanceMap.containsKey(pos)) {
			IBlockState iblockstate = this.world.getBlockState(pos);
			double resistance = this.exploder != null ?
					this.exploder.getExplosionResistance(this.explosion, this.world, pos, iblockstate)
					: iblockstate.getBlock().getExplosionResistance(this.world, pos, (Entity) null, this.explosion);
			this.resistanceMap.put(pos, resistance);
			return resistance;
		} else{
			return this.resistanceMap.getDouble(pos);
		}
	}
	
	public double getBlockHardnessAt(BlockPos pos) {
		if (this.affectedBlocks.containsKey(pos)) return 0.0d;
		else return this.world.getBlockState(pos).getBlockHardness(this.world, pos);
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
	
	/**
	 * 下にパーティクルを出すブロック
	 * @return
	 */
	public Set<BlockPos> getWiggledBlocks() {
		return this.axis.values().parallelStream().flatMap(ax -> ax.getWiggledBlocks().parallelStream())
				.filter(pos -> {
					BlockPos off = pos.offset(EnumFacing.DOWN);
					return !this.world.getBlockState(off).isOpaqueCube() ||
							this.affectedBlocks.containsKey(off);
				}).collect(Collectors.toSet());
	}
	
	public Set<BlockPos> getHitBlocks(Random rand) {
		return this.hitBlocks.entrySet().parallelStream().filter(e -> {
			double val = e.getValue();
			return val >= 1.0 || val >= rand.nextDouble();
		}).map(e -> e.getKey()).collect(Collectors.toSet());
	}
}
