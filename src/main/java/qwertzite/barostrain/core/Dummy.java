package qwertzite.barostrain.core;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
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

/**
 *  DELETE THIS CLASS!
 * @author qwertzite
 * @date 2021/03/19
 */
public class Dummy extends Explosion {

	private final boolean causesFire;
	private final boolean damagesTerrain;
	private final Random random;
	private final World world;
	private final double x;
	private final double y;
	private final double z;
	private final Entity exploder;
	private final float size;
	private final List<BlockPos> affectedBlockPositions;
	private final Map<EntityPlayer, Vec3d> playerKnockbackMap;
	private final Vec3d position;

	@SideOnly(Side.CLIENT)
	public Dummy(World worldIn, Entity entityIn, double x, double y, double z, float size,
			List<BlockPos> affectedPositions) {
		this(worldIn, entityIn, x, y, z, size, false, true, affectedPositions);
	}

	@SideOnly(Side.CLIENT)
	public Dummy(World worldIn, Entity entityIn, double x, double y, double z, float size, boolean causesFire,
			boolean damagesTerrain, List<BlockPos> affectedPositions) {
		this(worldIn, entityIn, x, y, z, size, causesFire, damagesTerrain);
		this.affectedBlockPositions.addAll(affectedPositions);
	}

	public Dummy(World worldIn, Entity entityIn, double x, double y, double z, float size, boolean flaming,
			boolean damagesTerrain) {
		super(worldIn, entityIn, x, y, z, size, flaming, damagesTerrain);
		this.random = new Random();
		this.affectedBlockPositions = Lists.<BlockPos>newArrayList();
		this.playerKnockbackMap = Maps.<EntityPlayer, Vec3d>newHashMap();
		this.world = worldIn;
		this.exploder = entityIn;
		this.size = size;
		this.x = x;
		this.y = y;
		this.z = z;
		this.causesFire = flaming;
		this.damagesTerrain = damagesTerrain;
		this.position = new Vec3d(this.x, this.y, this.z);
	}

	public void doExplosionA() {
		Set<BlockPos> affectedset = Sets.<BlockPos>newHashSet();

		for (int ix = 0; ix < 16; ++ix) {
			for (int iy = 0; iy < 16; ++iy) {
				for (int iz = 0; iz < 16; ++iz) {
					if (ix == 0 || ix == 15 || iy == 0 || iy == 15 || iz == 0 || iz == 15) { // ??????????????????
						double dx = (double) ((float) ix / 15.0F * 2.0F - 1.0F);
						double dy = (double) ((float) iy / 15.0F * 2.0F - 1.0F);
						double dz = (double) ((float) iz / 15.0F * 2.0F - 1.0F);
						double dn = Math.sqrt(dx * dx + dy * dy + dz * dz);
						dx = dx / dn;
						dy = dy / dn;
						dz = dz / dn;
						float size = this.size * (0.7F + this.world.rand.nextFloat() * 0.6F);
						double cx = this.x;
						double cy = this.y;
						double cz = this.z;

						for (; size > 0.0F; size -= 0.225F) {
							BlockPos blockpos = new BlockPos(cx, cy, cz);
							IBlockState iblockstate = this.world.getBlockState(blockpos);

							if (iblockstate.getMaterial() != Material.AIR) {
								float f2 = this.exploder != null
										? this.exploder.getExplosionResistance(this, this.world, blockpos, iblockstate)
										: iblockstate.getBlock().getExplosionResistance(world, blockpos, (Entity) null,
												this);
								size -= (f2 + 0.3F) * 0.3F;
							}

							if (size > 0.0F && (this.exploder == null || this.exploder.canExplosionDestroyBlock(this,
									this.world, blockpos, iblockstate, size))) {
								affectedset.add(blockpos); // ????????????????????????????????????????????????????????????
							}

							cx += dx * 0.3d;
							cy += dy * 0.3d;
							cz += dz * 0.3d;
						}
					}
				}
			}
		}
		this.affectedBlockPositions.addAll(affectedset);
		
		float entityRadius = this.size * 2.0F;
		int k1 = MathHelper.floor(this.x - (double) entityRadius - 1.0D);
		int l1 = MathHelper.floor(this.x + (double) entityRadius + 1.0D);
		int i2 = MathHelper.floor(this.y - (double) entityRadius - 1.0D);
		int i1 = MathHelper.floor(this.y + (double) entityRadius + 1.0D);
		int j2 = MathHelper.floor(this.z - (double) entityRadius - 1.0D);
		int j1 = MathHelper.floor(this.z + (double) entityRadius + 1.0D);
		List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this.exploder,
				new AxisAlignedBB((double) k1, (double) i2, (double) j2, (double) l1, (double) i1, (double) j1));
		net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.world, this, list, entityRadius);
		Vec3d vec3d = new Vec3d(this.x, this.y, this.z);

		for (int k2 = 0; k2 < list.size(); ++k2) {
			Entity entity = list.get(k2);

			if (!entity.isImmuneToExplosions()) {
				double d12 = entity.getDistance(this.x, this.y, this.z) / (double) entityRadius;

				if (d12 <= 1.0D) {
					double d5 = entity.posX - this.x;
					double d7 = entity.posY + (double) entity.getEyeHeight() - this.y;
					double d9 = entity.posZ - this.z;
					double d13 = (double) MathHelper.sqrt(d5 * d5 + d7 * d7 + d9 * d9);

					if (d13 != 0.0D) {
						d5 = d5 / d13;
						d7 = d7 / d13;
						d9 = d9 / d13;
						double d14 = (double) this.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
						double d10 = (1.0D - d12) * d14;
						entity.attackEntityFrom(DamageSource.causeExplosionDamage(this),
								(float) ((int) ((d10 * d10 + d10) / 2.0D * 7.0D * (double) entityRadius + 1.0D)));
						double d11 = d10;

						if (entity instanceof EntityLivingBase) {
							d11 = EnchantmentProtection.getBlastDamageReduction((EntityLivingBase) entity, d10);
						}

						entity.motionX += d5 * d11;
						entity.motionY += d7 * d11;
						entity.motionZ += d9 * d11;

						if (entity instanceof EntityPlayer) {
							EntityPlayer entityplayer = (EntityPlayer) entity;

							if (!entityplayer.isSpectator()
									&& (!entityplayer.isCreative() || !entityplayer.capabilities.isFlying)) {
								this.playerKnockbackMap.put(entityplayer, new Vec3d(d5 * d10, d7 * d10, d9 * d10));
							}
						}
					}
				}
			}
		}
	}

	public void doExplosionB(boolean spawnParticles) {
		this.world.playSound((EntityPlayer) null, this.x, this.y, this.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
				SoundCategory.BLOCKS, 4.0F,
				(1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F);

		if (this.size >= 2.0F && this.damagesTerrain) {
			this.world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
		} else {
			this.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
		}

		if (this.damagesTerrain) {
			for (BlockPos blockpos : this.affectedBlockPositions) {
				IBlockState iblockstate = this.world.getBlockState(blockpos);
				Block block = iblockstate.getBlock();

				if (spawnParticles) {
					double d0 = (double) ((float) blockpos.getX() + this.world.rand.nextFloat());
					double d1 = (double) ((float) blockpos.getY() + this.world.rand.nextFloat());
					double d2 = (double) ((float) blockpos.getZ() + this.world.rand.nextFloat());
					double d3 = d0 - this.x;
					double d4 = d1 - this.y;
					double d5 = d2 - this.z;
					double d6 = (double) MathHelper.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
					d3 = d3 / d6;
					d4 = d4 / d6;
					d5 = d5 / d6;
					double d7 = 0.5D / (d6 / (double) this.size + 0.1D);
					d7 = d7 * (double) (this.world.rand.nextFloat() * this.world.rand.nextFloat() + 0.3F);
					d3 = d3 * d7;
					d4 = d4 * d7;
					d5 = d5 * d7;
					this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, (d0 + this.x) / 2.0D,
							(d1 + this.y) / 2.0D, (d2 + this.z) / 2.0D, d3, d4, d5);
					this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d0, d1, d2, d3, d4, d5);
				}

				if (iblockstate.getMaterial() != Material.AIR) {
					if (block.canDropFromExplosion(this)) {
						block.dropBlockAsItemWithChance(this.world, blockpos, this.world.getBlockState(blockpos),
								1.0F / this.size, 0);
					}

					block.onBlockExploded(this.world, blockpos, this);
				}
			}
		}

		if (this.causesFire) {
			for (BlockPos blockpos1 : this.affectedBlockPositions) {
				if (this.world.getBlockState(blockpos1).getMaterial() == Material.AIR
						&& this.world.getBlockState(blockpos1.down()).isFullBlock() && this.random.nextInt(3) == 0) {
					this.world.setBlockState(blockpos1, Blocks.FIRE.getDefaultState());
				}
			}
		}
	}

	public Map<EntityPlayer, Vec3d> getPlayerKnockbackMap() {
		return this.playerKnockbackMap;
	}

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

	public void clearAffectedBlockPositions() {
		this.affectedBlockPositions.clear();
	}

	public List<BlockPos> getAffectedBlockPositions() {
		return this.affectedBlockPositions;
	}

	public Vec3d getPosition() {
		return this.position;
	}

}
