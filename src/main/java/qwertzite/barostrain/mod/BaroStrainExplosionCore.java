package qwertzite.barostrain.mod;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import qwertzite.barostrain.core.BsExplosions;
import qwertzite.barostrain.core.PressureRay;
import qwertzite.barostrain.mod.command.CommandBSTest;
import qwertzite.barostrain.mod.command.CommandBsMulti;
import qwertzite.barostrain.mod.command.CommandExplosion;
import qwertzite.barostrain.mod.command.CommandGenStructure;
import qwertzite.barostrain.util.BsxModLog;
import qwertzite.barostrain.util.TickScheduler;

@Mod(modid = BaroStrainExplosionCore.MODID, name = BaroStrainExplosionCore.MOD_NAME, version = BaroStrainExplosionCore.VERSION)
public class BaroStrainExplosionCore {
	public static final String MODID = "barostrainexp";
	public static final String MOD_NAME = "Barostrain Explosion";
	public static final String VERSION = "0.0.0-1.12.2-0000";
	public static final String RESOURCE_DOMAIN = "barostrainexp:";
	
	@Mod.Instance(MODID)
	public static BaroStrainExplosionCore INSTANCE;
	
	public final TickScheduler scheduler = new TickScheduler();
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		BsxModLog.onPreInit(event, true);
		BsExplosions.onInit(NetworkRegistry.INSTANCE.newSimpleChannel(MODID), 0);
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(this.scheduler);
	}
	
	@EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandExplosion());
		event.registerServerCommand(new CommandBSTest());
		event.registerServerCommand(new CommandBsMulti());
		event.registerServerCommand(new CommandGenStructure());
	}
	
//	@SubscribeEvent
	public void onRenderEvent(RenderWorldLastEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		RenderManager terd = mc.getRenderManager();
//		BlockPos pos = entry.position.getPosition();
//    	double eye = terd.viewerPosY + terd.renderViewEntity.getEyeHeight();
		double dx = terd.viewerPosX;
		double dy = terd.viewerPosY;
		double dz = terd.viewerPosZ;

		GlStateManager.pushMatrix();
		GlStateManager.enableDepth();
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableCull();
		GlStateManager.disableTexture2D();
		
		GlStateManager.translate(-dx, -dy, -dz);
		
		GlStateManager.enableBlend();
		GlStateManager.color(1.0f, 0.0f, 0.0f, 1.0f);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		synchronized (PressureRay.RAYS) {
			for (PressureRay ray : PressureRay.RAYS) {
				Vec3d from = ray.getAbsFrom();
				Vec3d to = ray.hit == null ? ray.getAbsTo() : ray.hit.hitVec;
				bufferbuilder.pos(from.x, from.y, from.z).color(1.0f, 0.0f, 0.0f, 0.333f).endVertex();
				bufferbuilder.pos(to.x, to.y, to.z).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
			}
		}
		tessellator.draw();
		GlStateManager.color(0.0f, 0.5f, 0.0f, 1.0f);
		bufferbuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
		synchronized (PressureRay.RAYS) {
			for (PressureRay ray : PressureRay.RAYS) {
				Vec3d[] seeds = ray.seed;
				Vec3d s0 = seeds[0].add(ray.origin);
				Vec3d s1 = seeds[1].add(ray.origin);
				Vec3d s2 = seeds[2].add(ray.origin);
				bufferbuilder.pos(s0.x, s0.y, s0.z).endVertex();
				bufferbuilder.pos(s1.x, s1.y, s1.z).endVertex();
				bufferbuilder.pos(s1.x, s1.y, s1.z).endVertex();
				bufferbuilder.pos(s2.x, s2.y, s2.z).endVertex();
				bufferbuilder.pos(s2.x, s2.y, s2.z).endVertex();
				bufferbuilder.pos(s0.x, s0.y, s0.z).endVertex();
			}
		}
		tessellator.draw();
		GlStateManager.color(1.0f, 1.0f, 1.0f);

		GlStateManager.popMatrix();
		
		GlStateManager.disableAlpha();
		GlStateManager.enableDepth();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.disableFog();
		GlStateManager.enableTexture2D();
	}
}
