package qwertzite.barostrain.core;

import java.util.EnumMap;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.math.BlockPos;
import qwertzite.barostrain.util.BsxMath;

/**
 * あるブロックの爆破耐性，現在それぞれの面に掛かっている力，働いている慣性力を記録する<br>
 * また，爆破耐性，破壊耐性に基づき，それぞれの面があとどのくらいの力に耐えられるのかを計算する
 * 
 * @author qwertzite
 * @date 2021/04/01
 */
public class BlockStrain {
	private BlockPos pos;
	private Axis axis;
	private long blastResistance = -1;
	private long hardness;
	
	private long absorved = 0;
	/** その面が相手に及ぼしている力．<br>
	 * 正なら，その軸の正の向きの力を及ぼしている (自分は負の向きの反力を受けている)
	 */
	private EnumMap<EnumFacing, Long> transmittingForce = new EnumMap<>(EnumFacing.class);
	
	public BlockStrain(BlockPos pos, Axis axis, double resistance, double hardness) {
		this.pos = pos;
		this.axis = axis;
		this.blastResistance = Math.round(resistance*AxisStrain.BASE);
		this.hardness = Math.round(hardness*AxisStrain.BASE);
	}
	
	public BlockPos getPos() { return this.pos; }
	
	public long getAbsorveable(long applied) {
		long ret = BsxMath.clamp(applied, -this.getInertialCapacity() - this.absorved, this.getInertialCapacity()-this.absorved);
		return ret * applied < 0 ? 0 : ret;
	}
	
	/** どちらか一方の上限に達しているか */
	public boolean hasReachedCapacity() {
		return absorved <= -this.getInertialCapacity() || this.getInertialCapacity() <= absorved;
	}
	
	public boolean isElastoPasticDeforming(EnumFacing face) {
		long force = this.transmittingForce.getOrDefault(face, 0L);
//		final long ERR = BSExplosionBase.ERR;
		long maxLim = this.getMaxLim(face);
		long minLim = this.getMinLim(face);
		return force <= minLim || maxLim <= force;
	}
	
	public void absorveForce(long force) {
		this.absorved += force;
	}
	
	/**
	 * twice of that of non initial force.
	 * @param face
	 * @param applied
	 * @return
	 */
	public long calcFlowableInitialForceForFace(EnumFacing face, long applied) {
		long current = this.transmittingForce.getOrDefault(face, 0L);
		long maxLim = this.getMaxLim(face)*2 - current;
		long minLim = this.getMinLim(face)*2 - current;
//		System.out.println("flowable " + this.getPos() + " " + current + " " + this.getMinLim(face) + " " + this.getMaxLim(face) + " " + applied);
		long ret = BsxMath.clamp(applied, minLim, maxLim);
		return ret * applied < 0 ? 0 : ret;
	}

	public long calcFlowableForceForFace(EnumFacing face, long applied) {
		long current = this.transmittingForce.getOrDefault(face, 0L);
		long maxLim = this.getMaxLim(face) - current;
		long minLim = this.getMinLim(face) - current;
		long ret = BsxMath.clamp(applied, minLim, maxLim);
		return ret * applied < 0 ? 0 : ret;
	}
	
	private long getMaxLim(EnumFacing facing) {
		if (facing.getAxis() == this.axis) {
			return facing.getAxisDirection() == AxisDirection.POSITIVE ?
					this.getCompressiveStress():
					this.getTensileStress();
		} else {
			return this.getShearingStress();
		}
	}
	
	private long getMinLim(EnumFacing facing) {
		if (facing.getAxis() == this.axis) {
			return facing.getAxisDirection() == AxisDirection.POSITIVE ?
					- this.getTensileStress() :
					- this.getCompressiveStress();
		} else {
			return - this.getShearingStress();
		}
	}
	
	private long getInertialCapacity() { return this.blastResistance / 16; }
	private long getCompressiveStress() { return this.blastResistance; }
	private long getTensileStress() { return this.hardness / 2; } // 想定の1/2
	private long getShearingStress() { return this.hardness / 2; }
	
	/** 
	 * その面が (相手に) 及ぼしている力を加算する
	 * @param direction
	 * @param force
	 */
	public void flowForceThroughFace(EnumFacing direction, long force) {
		this.transmittingForce.put(direction, this.transmittingForce.getOrDefault(direction, 0L) + force);
	}
	
	public long getAbsorved() { return this.absorved; }
	
	public long getTransmittingForce(EnumFacing direction) {
		return this.transmittingForce.getOrDefault(direction, 0L);
	}
	
	/** multiplied by BASE */
	public long getBlastResistance() { return this.blastResistance; }
	public boolean isBlastResistable() { return this.blastResistance != 0 || this.hardness != 0; }
	
	@Override
	public String toString() {
		return this.pos + ",res=" + this.blastResistance + ",hrd=" + this.hardness + ",abs=" + absorved + " " 
				+ "D:" + this.transmittingForce.getOrDefault(EnumFacing.DOWN, 0L) + " "
				+ "U:" + this.transmittingForce.getOrDefault(EnumFacing.UP, 0L) + " "
				+ "N:" + this.transmittingForce.getOrDefault(EnumFacing.NORTH, 0L) + " "
				+ "S:" + this.transmittingForce.getOrDefault(EnumFacing.SOUTH, 0L) + " "
				+ "W:" + this.transmittingForce.getOrDefault(EnumFacing.WEST, 0L) + " "
				+ "E:" + this.transmittingForce.getOrDefault(EnumFacing.EAST, 0L);
	}
}
