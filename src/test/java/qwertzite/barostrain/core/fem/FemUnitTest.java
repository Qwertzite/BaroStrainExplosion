package qwertzite.barostrain.core.fem;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import qwertzite.barostrain.core.common.coord.ElemVertex;
import qwertzite.barostrain.core.common.coord.VertexPos;

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
		FemIter iter = new FemIter(Collections.emptyMap());
		
		Set<BlockPos> elements = new HashSet<>();
		elements.add(BlockPos.ORIGIN);
		
		Map<VertexPos, Vec3d> displacement = new HashMap<>();
		displacement.put(CoordHelper.vertexPos(BlockPos.ORIGIN, ElemVertex.VPNN), disp);
		displacement.put(CoordHelper.vertexPos(BlockPos.ORIGIN, ElemVertex.VPNP), disp);
		displacement.put(CoordHelper.vertexPos(BlockPos.ORIGIN, ElemVertex.VPPN), disp);
		displacement.put(CoordHelper.vertexPos(BlockPos.ORIGIN, ElemVertex.VPPP), disp);
		iter.setDisplacement(displacement);
		
		this.executeFEM(fem, iter, elements);
		
		System.out.println("test");
		for (Map.Entry<VertexPos, Vec3d> e : iter.getForceBalance().entrySet()) {
			System.out.println(e);
		}
	}
	
	@Test
	void compress() {
		IBlockPropertyProvider bpp = new DummyBlockPropProvider(1, 1);
		FEM fem = new FEM(bpp);
		FemIter iter = new FemIter(Collections.emptyMap());
		
		Set<BlockPos> elements = new HashSet<>();
		elements.add(BlockPos.ORIGIN);
		
		Map<VertexPos, Vec3d> displacement = new HashMap<>();
		displacement.put(CoordHelper.vertexPos(BlockPos.ORIGIN, ElemVertex.VPNN), new Vec3d(0, +0.1d, +0.1d)); // COMBAK: 力の正負，慣性力のテスト
		displacement.put(CoordHelper.vertexPos(BlockPos.ORIGIN, ElemVertex.VPNP), new Vec3d(0, +0.1d, -0.1d));
		displacement.put(CoordHelper.vertexPos(BlockPos.ORIGIN, ElemVertex.VPPN), new Vec3d(0, -0.1d, +0.1d));
		displacement.put(CoordHelper.vertexPos(BlockPos.ORIGIN, ElemVertex.VPPP), new Vec3d(0, -0.1d, -0.1d));
		
		iter.setDisplacement(displacement);
		
		this.executeFEM(fem, iter, elements);
		
		System.out.println("cmp");
		for (Map.Entry<VertexPos, Vec3d> e : iter.getForceBalance().entrySet()) {
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
		FemIter iter = new FemIter(Collections.emptyMap());
		
		Set<BlockPos> elements = new HashSet<>();
		elements.add(BlockPos.ORIGIN);
		
		Map<VertexPos, Vec3d> displacement = new HashMap<>();
		for (int i = 0; i < vs.length; i++) {
			displacement.put(CoordHelper.vertexPos(BlockPos.ORIGIN, vs[i]), disp[i]);
		}
		
		iter.setDisplacement(displacement);
		
		this.executeFEM(fem, iter, elements);
		
		System.out.println(msg);
		for (Map.Entry<VertexPos, Vec3d> e : iter.getForceBalance().entrySet()) {
			System.out.println(e);
		}
	}
	
	private void executeFEM(FEM fem, FemIter iter, Set<BlockPos> elem) {
		try {
			Method method = fem.getClass().getDeclaredMethod("computeVertexForce", FemIter.class, Set.class);
			method.setAccessible(true);
			method.invoke(fem, iter, elem);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
	}
}
