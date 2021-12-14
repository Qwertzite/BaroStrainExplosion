package qwertzite.barostrain.core.fem;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class BlockPropProviderImpl extends AbstractBlockPropProvider {
	
	private final Explosion explosion;
	private final World world;
	private final Entity exploder;
	
	private Set<BlockPos> destroyeds = new HashSet<>();
	
	private Object2DoubleMap<BlockPos> resistanceMap = new Object2DoubleOpenHashMap<>();
	private ReadWriteLock resistanceLock = new ReentrantReadWriteLock();
	
	public BlockPropProviderImpl(Explosion explosion, Entity exploder, World world) {
		this.explosion = explosion;
		this.world = world;
		this.exploder = exploder;
	}
	
	public void addDestroyed(Set<BlockPos> set) {
		set.forEach(this::markAsDestroyed);
	}
	
	public synchronized void markAsDestroyed(BlockPos pos) {
		this.resistanceMap.remove(pos);
		this.destroyeds.add(pos);
	}
	
	@Override
	protected double hardness(BlockPos pos) {
		if (this.destroyeds.contains(pos)) return 0.0d; // 破壊判定されている場合
		this.resistanceLock.readLock().lock();
		if (!this.resistanceMap.containsKey(pos)) {
			this.resistanceLock.readLock().unlock();
			
			this.resistanceLock.writeLock().lock();
			if (!this.resistanceMap.containsKey(pos)) {
				IBlockState iblockstate = this.world.getBlockState(pos);
				double resistance = this.exploder != null
						? this.exploder.getExplosionResistance(this.explosion, this.world, pos, iblockstate)
						: iblockstate.getBlock().getExplosionResistance(this.world, pos, (Entity) null, this.explosion);
				this.resistanceMap.put(pos, resistance);
				this.resistanceLock.writeLock().unlock();
				return resistance;
			}
			this.resistanceLock.writeLock().unlock();
			
			this.resistanceLock.readLock().lock();
		}
		double resistance = this.resistanceMap.getDouble(pos);
		this.resistanceLock.readLock().unlock();
		return resistance;
	}

	@Override
	protected double resistance(BlockPos pos) {
		if (this.destroyeds.contains(pos)) return 0.0d;
		else return this.world.getBlockState(pos).getBlockHardness(this.world, pos);
	}
	
	@Override
	public double getSigmaYield(BlockPos element) {
		return this.hardness(element);
	}
}
