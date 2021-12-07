package qwertzite.barostrain.core.fem;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

class FemUnitTest {

	@Test
	void test() {
		test(new Vec3d(8, 0.0d, 0.0d));
		test(new Vec3d(4, 0.0d, 0.0d));
		test(new Vec3d(2, 0.0d, 0.0d));
		test(new Vec3d(1, 0.0d, 0.0d));
		test(new Vec3d(0.0d, 8, 0.0d));
		test(new Vec3d(0.0d, 4, 0.0d));
		test(new Vec3d(0.0d, 2, 0.0d));
		test(new Vec3d(0.0d, 1, 0.0d));
		test(new Vec3d(-8, 0.0d, 0.0d));
		test(new Vec3d(-4, 0.0d, 0.0d));
		test(new Vec3d(-2, 0.0d, 0.0d));
		test(new Vec3d(-1, 0.0d, 0.0d));
//		test(0.1);
//		test(0.01);
//		test(0.001);
		
		assertTrue(true);
	}
	
	private void test(Vec3d disp) {
		IBlockPropertyProvider bpp = new DummyBlockPropProvider(1, 1);
		FEM fem = new FEM(bpp);
		FemIter iter = new FemIter();
		
		Set<BlockPos> elements = new HashSet<>();
		elements.add(BlockPos.ORIGIN);
		iter.targetElements = elements;
		
		Map<VertexPos, Vec3d> displacement = new HashMap<>();
		displacement.put(new VertexPos(BlockPos.ORIGIN, ElemVertex.VPNN), disp);
		displacement.put(new VertexPos(BlockPos.ORIGIN, ElemVertex.VPNP), disp);
		displacement.put(new VertexPos(BlockPos.ORIGIN, ElemVertex.VPPN), disp);
		displacement.put(new VertexPos(BlockPos.ORIGIN, ElemVertex.VPPP), disp);
		
		iter.displacement = displacement;
		
		fem.computeVertexForce(iter);
		
		for (Map.Entry<VertexPos, Vec3d> e : iter.vertexForce.entrySet()) {
			System.out.println(e);
		}
	}
	
	@Test
	void compress() {
		IBlockPropertyProvider bpp = new DummyBlockPropProvider(1, 1);
		FEM fem = new FEM(bpp);
		FemIter iter = new FemIter();
		
		Set<BlockPos> elements = new HashSet<>();
		elements.add(BlockPos.ORIGIN);
		iter.targetElements = elements;
		
		Map<VertexPos, Vec3d> displacement = new HashMap<>();
		displacement.put(new VertexPos(BlockPos.ORIGIN, ElemVertex.VPNN), new Vec3d(0, +0.1d, +0.1d));
		displacement.put(new VertexPos(BlockPos.ORIGIN, ElemVertex.VPNP), new Vec3d(0, +0.1d, -0.1d));
		displacement.put(new VertexPos(BlockPos.ORIGIN, ElemVertex.VPPN), new Vec3d(0, -0.1d, +0.1d));
		displacement.put(new VertexPos(BlockPos.ORIGIN, ElemVertex.VPPP), new Vec3d(0, -0.1d, -0.1d));
		
		iter.displacement = displacement;
		
		fem.computeVertexForce(iter);
		
		System.out.println("cmp");
		for (Map.Entry<VertexPos, Vec3d> e : iter.vertexForce.entrySet()) {
			System.out.println(e);
		}
	}
	
	@Test
	void elaborate() {
		
		elaborate(
				new ElemVertex[] {     ElemVertex.VNNN,     ElemVertex.VNNP,     ElemVertex.VNPN,     ElemVertex.VNPP,     ElemVertex.VPNN,     ElemVertex.VPNP,     ElemVertex.VPPN,     ElemVertex.VPPP, },
				new Vec3d[]      { new Vec3d( 1, 1, 1), new Vec3d( 1, 1,-1), new Vec3d( 1,-1, 1), new Vec3d( 1,-1,-1), new Vec3d(-1, 1, 1), new Vec3d(-1, 1,-1), new Vec3d(-1,-1, 1), new Vec3d(-1,-1,-1), }, "等方");
		elaborate(
				new ElemVertex[] {     ElemVertex.VNNN,     ElemVertex.VNNP,     ElemVertex.VNPN,     ElemVertex.VNPP,     ElemVertex.VPNN,     ElemVertex.VPNP,     ElemVertex.VPPN,     ElemVertex.VPPP, },
				new Vec3d[]      { new Vec3d( 0, 1, 1), new Vec3d( 0, 1,-1), new Vec3d( 0,-1, 1), new Vec3d( 0,-1,-1), new Vec3d( 0, 1, 1), new Vec3d( 0, 1,-1), new Vec3d( 0,-1, 1), new Vec3d( 0,-1,-1), }, "x以外");
		elaborate(
				new ElemVertex[] {     ElemVertex.VNNN,     ElemVertex.VNNP,     ElemVertex.VNPN,     ElemVertex.VNPP,     ElemVertex.VPNN,     ElemVertex.VPNP,     ElemVertex.VPPN,     ElemVertex.VPPP, },
				new Vec3d[]      { new Vec3d( 1, 0, 0), new Vec3d( 1, 0, 0), new Vec3d( 1, 0, 0), new Vec3d( 1, 0, 0), new Vec3d(-1, 0, 0), new Vec3d(-1, 0, 0), new Vec3d(-1, 0, 0), new Vec3d(-1, 0, 0), }, "xのみ");
		
	}
	
	private void elaborate(ElemVertex[] vs, Vec3d[] disp, String msg) {
		IBlockPropertyProvider bpp = new DummyBlockPropProvider(1, 1);
		FEM fem = new FEM(bpp);
		FemIter iter = new FemIter();
		
		Set<BlockPos> elements = new HashSet<>();
		elements.add(BlockPos.ORIGIN);
		iter.targetElements = elements;
		
		Map<VertexPos, Vec3d> displacement = new HashMap<>();
		for (int i = 0; i < vs.length; i++) {
			displacement.put(new VertexPos(BlockPos.ORIGIN, vs[i]), disp[i]);
		}
		
		iter.displacement = displacement;
		
		fem.computeVertexForce(iter);
		
		System.out.println(msg);
		for (Map.Entry<VertexPos, Vec3d> e : iter.vertexForce.entrySet()) {
			System.out.println(e);
		}
	}
}
