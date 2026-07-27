package com.etherealfeast.event;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.item.BaiWeiItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = EtherealFeast.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();

        for (Player player : mc.level.players()) {
            if (player == null || player.isSpectator()) continue;

            PlayerIdentityData.IdentityData data = PlayerIdentityData.get(player);
            if (!data.isBound()) continue;

            renderHalo(poseStack, bufferSource, player, data, event.getPartialTick().getGameTimeDeltaTicks());
        }
    }

    private static void renderHalo(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                   Player player, PlayerIdentityData.IdentityData data, float partialTicks) {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        double x = player.xOld + (player.getX() - player.xOld) * partialTicks - cameraPos.x;
        double y = player.yOld + (player.getY() - player.yOld) * partialTicks - cameraPos.y;
        double z = player.zOld + (player.getZ() - player.zOld) * partialTicks - cameraPos.z;

        y += player.getEyeHeight() + 0.5;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.5f, 0.5f, 0.5f);

        float r, g, b, a;
        if (data.getIdentityType() == BaiWeiItem.IdentityType.SOLO) {
            r = 0.3f; g = 0.7f; b = 1.0f; a = 0.4f;
        } else {
            r = 1.0f; g = 0.8f; b = 0.3f; a = 0.4f;
        }

        if (data.isDamaged()) {
            a *= 0.3f;
        }

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.LIGHTNING);

        float size = 0.3f;
        buffer.addVertex(matrix, -size, -size, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, -size, size, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, size, size, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, size, -size, 0).setColor(r, g, b, a);

        poseStack.popPose();
    }
}
