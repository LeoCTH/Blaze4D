package me.hydos.blaze4d.mixin.vertices;

import me.hydos.blaze4d.api.GlobalRenderSystem;
import me.hydos.blaze4d.api.vertex.ConsumerCreationInfo;
import me.hydos.blaze4d.api.vertex.UploadableConsumer;
import me.hydos.rosella.render.shader.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin extends FixedColorVertexConsumer implements UploadableConsumer {

    private me.hydos.rosella.render.vertex.BufferVertexConsumer consumer;

    @Inject(method = "begin", at = @At("HEAD"))
    private void setupConsumer(VertexFormat.DrawMode drawMode, VertexFormat format, CallbackInfo ci) {
        Matrix4f projMatrix = new Matrix4f(GlobalRenderSystem.projectionMatrix);
        Matrix4f viewMatrix = new Matrix4f(GlobalRenderSystem.modelViewMatrix);
        Vector3f chunkOffset = new Vector3f(GlobalRenderSystem.chunkOffset);
        Vec3f shaderLightDirections0 = GlobalRenderSystem.shaderLightDirections0.copy();
        Vec3f shaderLightDirections1 = GlobalRenderSystem.shaderLightDirections1.copy();

        this.consumer = GlobalRenderSystem.GLOBAL_CONSUMERS.computeIfAbsent(new ConsumerCreationInfo(drawMode, format, GlobalRenderSystem.createTextureArray(), GlobalRenderSystem.activeShader, projMatrix, viewMatrix, chunkOffset, shaderLightDirections0, shaderLightDirections1), _format -> {
            me.hydos.rosella.render.vertex.BufferVertexConsumer consumer;
            if (_format.format() == VertexFormats.POSITION) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION());
            } else if (_format.format() == VertexFormats.POSITION_COLOR) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_COLOR4());
            } else if (_format.format() == VertexFormats.POSITION_COLOR_TEXTURE) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_COLOR4_UV());
            } else if (_format.format() == VertexFormats.POSITION_TEXTURE) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_UV());
            } else if (_format.format() == VertexFormats.POSITION_TEXTURE_COLOR) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_UV_COLOR4());
            } else if (_format.format() == VertexFormats.LINES) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_COLOR_NORMAL());
            } else if (_format.format() == VertexFormats.POSITION_COLOR_TEXTURE_LIGHT) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_COLOR4_UV_LIGHT());
            } else if (_format.format() == VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_COLOR4_UV_LIGHT_NORMAL());
            } else if (_format.format() == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_COLOR4_UV_UV0_LIGHT_NORMAL());
            } else if (_format.format() == VertexFormats.POSITION_TEXTURE_COLOR_NORMAL) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_UV_COLOR4_NORMAL());
            } else if (_format.format() == VertexFormats.POSITION_TEXTURE_COLOR_LIGHT) {
                consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_UV_COLOR4_LIGHT());
            } else {
                // Check if its text
                List<VertexFormatElement> elements = _format.format().getElements();
                if (elements.size() == 4 && elements.get(0) == VertexFormats.POSITION_ELEMENT && elements.get(1) == VertexFormats.COLOR_ELEMENT && elements.get(2) == VertexFormats.TEXTURE_0_ELEMENT && elements.get(3).getByteLength() == 4) {
                    consumer = new me.hydos.rosella.render.vertex.BufferVertexConsumer(me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_COLOR4_UV0_UV());
                } else {
                    throw new RuntimeException("Format not implemented: " + _format.format());
                }
            }
            return consumer;
        });
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void clear(CallbackInfo ci) {
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        consumer.pos((float) x, (float) y, (float) z);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        consumer.normal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        consumer.color(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        consumer.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        consumer.light((short) u, (short) v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        consumer.uv((short) u, (short) v);
        return this;
    }

    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        if (consumer.getFormat() == me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_UV_COLOR4()) {
            this.vertex(x, y, z);
            this.texture(u, v);
            this.color(red, green, blue, alpha);
            return;
        }

        if (consumer.getFormat() == me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_UV()) {
            this.vertex(x, y, z);
            this.texture(u, v);
            return;
        }

        this.vertex(x, y, z);
        this.color(red, green, blue, alpha);
        this.texture(u, v);
        if (consumer.getFormat() != me.hydos.rosella.render.vertex.VertexFormats.Companion.getPOSITION_COLOR4_UV_LIGHT_NORMAL()) {
            this.overlay(overlay);
        }
        this.light(light);
        this.normal(normalX, normalY, normalZ);
        this.next();
    }

    @Override
    public void next() {
        consumer.nextVertex();
    }

    @Override
    public me.hydos.rosella.render.vertex.BufferVertexConsumer getConsumer() {
        return consumer;
    }

    @Override
    public ShaderProgram getShader() {
        return GlobalRenderSystem.activeShader;
    }
}