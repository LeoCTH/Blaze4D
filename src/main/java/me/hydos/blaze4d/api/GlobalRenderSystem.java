package me.hydos.blaze4d.api;

import java.util.*;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.hydos.blaze4d.Blaze4D;
import me.hydos.blaze4d.api.shader.ShaderContext;
import me.hydos.blaze4d.api.vertex.ConsumerCreationInfo;
import me.hydos.blaze4d.api.vertex.ConsumerRenderObject;
import me.hydos.blaze4d.api.vertex.ObjectInfo;
import me.hydos.blaze4d.mixin.shader.ShaderAccessor;
import me.hydos.rosella.Rosella;
import me.hydos.rosella.render.material.state.StateInfo;
import me.hydos.rosella.render.resource.Identifier;
import me.hydos.rosella.render.shader.RawShaderProgram;
import me.hydos.rosella.render.shader.ShaderProgram;
import me.hydos.rosella.render.texture.Texture;
import me.hydos.rosella.render.vertex.BufferVertexConsumer;
import me.hydos.rosella.scene.object.impl.SimpleObjectManager;
import net.minecraft.client.render.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.util.math.Vec3f;
import org.lwjgl.vulkan.VK10;

/**
 * Used to make bits of the code easier to manage.
 */
public class GlobalRenderSystem {
    // Shader Fields
    public static final Map<Integer, ShaderContext> SHADER_MAP = new Int2ObjectOpenHashMap<>();
    public static final Map<Integer, RawShaderProgram> SHADER_PROGRAM_MAP = new Int2ObjectOpenHashMap<>();

    public static final int DEFAULT_MAX_OBJECTS = 8092;
    public static String programErrorLog = "none";
    public static int nextShaderId = 1; // Minecraft is a special snowflake and needs shader's to start at 1
    public static int nextShaderProgramId = 1; // Same reason as above

    // Frame/Drawing Fields
    public static Set<ConsumerRenderObject> currentFrameObjects = new ObjectOpenHashSet<>();

    // Active Fields
    public static final int MAX_TEXTURES = 12;
    public static int[] boundTextureIds = new int[MAX_TEXTURES]; // TODO: generate an identifier instead of using int id, or switch everything over to ints
    public static int activeTexture = 0;

    static {
        Arrays.fill(boundTextureIds, -1);
    }

    public static ShaderProgram activeShader;

    // TODO maybe store snapshots of this in the materials so we keep the statelessness of vulkan
    public static StateInfo currentStateInfo = new StateInfo(
            VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT | VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT,
            true,
            false,
            0, 0, 0, 0,
            false,
            false,
            VK10.VK_BLEND_FACTOR_ONE, VK10.VK_BLEND_FACTOR_ZERO, VK10.VK_BLEND_FACTOR_ONE, VK10.VK_BLEND_FACTOR_ZERO,
            VK10.VK_BLEND_OP_ADD,
            true,
            false,
            VK10.VK_COMPARE_OP_LESS,
            false,
            VK10.VK_LOGIC_OP_COPY,
            1.0f
    );

    // Uniforms FIXME FIXME FIXME: to add support for custom uniforms and add support for mods like iris & lambdynamic lights, we need to do this
    // TODO: Custom uniforms are complete, but support for stuff like lambdynamic lights and iris is needed
    public static Matrix4f projectionMatrix = new Matrix4f();
    public static Matrix4f modelViewMatrix = new Matrix4f();
    public static Vector3f chunkOffset = new Vector3f();
    public static Vec3f shaderLightDirections0 = new Vec3f();
    public static Vec3f shaderLightDirections1 = new Vec3f();

    // Captured Shader for more dynamic uniforms and samplers
    public static ShaderAccessor blaze4d$capturedShader = null;

    //=================
    // Shader Methods
    //=================

    /**
     * @param glId the glId
     * @return a identifier which can be used instead of a glId
     */
    public static Identifier generateId(int glId) {
        return new Identifier("blaze4d", "gl_" + glId);
    }

    //=================
    // Frame/Drawing Methods
    //=================

    /**
     * Called when a frame is flipped. used to send all buffers to the engine to draw. Also allows for caching
     */
    public static void render() {
        Blaze4D.rosella.common.device.waitForIdle();

        GlobalRenderSystem.renderConsumers(); //TODO: move this probably

        ((SimpleObjectManager) Blaze4D.rosella.objectManager).renderObjects.clear();
        if (currentFrameObjects.size() < 2000) {
            for (ConsumerRenderObject renderObject : currentFrameObjects) {
                Blaze4D.rosella.objectManager.addObject(renderObject);
            }
        } else {
            Blaze4D.LOGGER.warn("Skipped a frame");
        }


        Blaze4D.rosella.renderer.rebuildCommandBuffers(Blaze4D.rosella.renderer.renderPass, (SimpleObjectManager) Blaze4D.rosella.objectManager);

        Blaze4D.window.update();
        Blaze4D.rosella.renderer.render(Blaze4D.rosella);

        for (ConsumerRenderObject consumerRenderObject : currentFrameObjects) {
            consumerRenderObject.free(Blaze4D.rosella.common.memory, Blaze4D.rosella.common.device);
        }
        currentFrameObjects.clear();
    }

    public static Texture[] createTextureArray() {
        Texture[] textures = new Texture[MAX_TEXTURES];
        for(int i = 0; i < MAX_TEXTURES; i++) {
            int texId = boundTextureIds[i];
            textures[i] = texId == -1 ? null : ((SimpleObjectManager) Blaze4D.rosella.objectManager).textureManager.getTexture(texId);
        }
        return textures;
    }

    public static void uploadObject(ObjectInfo objectInfo, Rosella rosella) {
        ConsumerRenderObject renderObject = new ConsumerRenderObject(objectInfo, rosella);
        currentFrameObjects.add(renderObject);
    }

    public static final Map<ConsumerCreationInfo, BufferVertexConsumer> GLOBAL_CONSUMERS_FOR_BATCH_RENDERING = new Object2ObjectOpenHashMap<>();

    public static void renderConsumers() {
        for (Map.Entry<ConsumerCreationInfo, BufferVertexConsumer> entry : GLOBAL_CONSUMERS_FOR_BATCH_RENDERING.entrySet()) {
            BufferVertexConsumer consumer = entry.getValue();
            List<Integer> indices = new ArrayList<>();
            ConsumerCreationInfo creationInfo = entry.getKey();

            if (creationInfo.drawMode() == VertexFormat.DrawMode.QUADS) {
                // Convert Quads to Triangle Strips
                //  0, 1, 2
                //  0, 2, 3
                //        v0_________________v1
                //         / \               /
                //        /     \           /
                //       /         \       /
                //      /             \   /
                //    v2-----------------v3

                for (int i = 0; i < consumer.getVertexCount(); i += 4) {
                    indices.add(i);
                    indices.add(1 + i);
                    indices.add(2 + i);

                    indices.add(2 + i);
                    indices.add(3 + i);
                    indices.add(i);
                }
            } else {
                for (int i = 0; i < consumer.getVertexCount(); i++) {
                    indices.add(i);
                }
            }

            if (consumer.getVertexCount() != 0) {
                ObjectInfo objectInfo = new ObjectInfo(
                        consumer,
                        creationInfo.drawMode(),
                        creationInfo.format(),
                        creationInfo.shader(),
                        creationInfo.textures(),
                        creationInfo.projMatrix(),
                        creationInfo.viewMatrix(),
                        creationInfo.chunkOffset(),
                        creationInfo.shaderLightDirections0(),
                        creationInfo.shaderLightDirections1(),
                        Collections.unmodifiableList(indices)
                );
                if (creationInfo.shader() != null) {
                    GlobalRenderSystem.uploadObject(objectInfo, Blaze4D.rosella);
                }
            }
        }
        GLOBAL_CONSUMERS_FOR_BATCH_RENDERING.clear();
    }
}
