package me.hydos.blaze4d.api.vertex;

import me.hydos.rosella.render.shader.ShaderProgram;
import me.hydos.rosella.render.texture.Texture;
import me.hydos.rosella.render.vertex.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.Vec3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Contains information for rendering objects to the screen like a VertexConsumer and ModelViewMatrix
 */
public record ObjectInfo(VertexConsumer consumer,
                         VertexFormat.DrawMode drawMode,
                         VertexFormat format,
                         ShaderProgram shader,
                         Texture[] textures, Matrix4f projMatrix,
                         Matrix4f viewMatrix, Vector3f chunkOffset,
                         Vec3f shaderLightDirections0,
                         Vec3f shaderLightDirections1,
                         List<Integer> indices) {
}
