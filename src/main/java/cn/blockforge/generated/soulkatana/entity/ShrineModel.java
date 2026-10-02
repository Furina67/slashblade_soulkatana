package cn.blockforge.generated.soulkatana.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/** Runtime mesh exported from the authored Sukuna shrine Blockbench model. */
public final class ShrineModel {
    private static final float TEXTURE_SIZE = 16.0F;
    private static final Cube[] CUBES = {
            cube(2F, 0F, 5F, 20F, 4F, 13F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(44F, 0F, 5F, 62F, 4F, 13F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(5F, 0F, 47F, 21F, 4F, 57F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(43F, 0F, 45F, 60F, 4F, 57F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(0F, 0F, 19F, 9F, 3F, 39F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(55F, 0F, 18F, 64F, 3F, 38F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(15F, 0F, 1F, 49F, 3F, 9F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(15F, 0F, 55F, 49F, 3F, 63F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(7F, 3F, 7F, 57F, 7F, 57F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(13F, 6F, 13F, 51F, 10F, 51F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(5F, 5F, 13F, 16F, 14F, 24F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(8F, 9F, 12.7F, 10F, 12F, 14F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(12F, 9F, 12.7F, 14F, 12F, 14F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(48F, 5F, 13F, 59F, 14F, 24F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(51F, 9F, 12.7F, 53F, 12F, 14F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(55F, 9F, 12.7F, 57F, 12F, 14F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(5F, 5F, 39F, 16F, 14F, 50F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(8F, 9F, 38.7F, 10F, 12F, 40F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(12F, 9F, 38.7F, 14F, 12F, 40F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(48F, 5F, 39F, 59F, 14F, 50F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(51F, 9F, 38.7F, 53F, 12F, 40F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(55F, 9F, 38.7F, 57F, 12F, 40F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(10F, 7F, 5F, 13F, 25F, 8F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(10F, 7F, 56F, 13F, 25F, 59F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(16F, 7F, 5F, 19F, 25F, 8F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(16F, 7F, 56F, 19F, 25F, 59F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(22F, 7F, 5F, 25F, 25F, 8F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(22F, 7F, 56F, 25F, 25F, 59F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(42F, 7F, 5F, 45F, 25F, 8F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(42F, 7F, 56F, 45F, 25F, 59F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(48F, 7F, 5F, 51F, 25F, 8F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(48F, 7F, 56F, 51F, 25F, 59F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(54F, 7F, 5F, 57F, 25F, 8F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(54F, 7F, 56F, 57F, 25F, 59F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(13F, 9F, 18F, 51F, 41F, 48F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(10F, 10F, 17F, 16F, 42F, 23F, uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F)),
            cube(10F, 10F, 41F, 16F, 42F, 47F, uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F)),
            cube(48F, 10F, 17F, 54F, 42F, 23F, uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F)),
            cube(48F, 10F, 41F, 54F, 42F, 47F, uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F)),
            cube(13F, 35F, 16F, 51F, 43F, 49F, uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F), uv(0F, 0F, 4F, 4F)),
            cube(8F, 41F, 13F, 56F, 46F, 52F, uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F)),
            cube(18F, 15F, 13F, 46F, 35F, 18F, uv(0F, 4F, 4F, 8F), uv(0F, 4F, 4F, 8F), uv(0F, 4F, 4F, 8F), uv(0F, 4F, 4F, 8F), uv(0F, 4F, 4F, 8F), uv(0F, 4F, 4F, 8F)),
            cube(19F, 14F, 12F, 45F, 19F, 17F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(20F, 31F, 12F, 44F, 36F, 17F, uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F), uv(12F, 4F, 16F, 8F)),
            cube(21F, 17F, 10.8F, 24F, 22F, 14F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(26F, 17F, 10.8F, 29F, 22F, 14F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(31F, 17F, 10.8F, 34F, 22F, 14F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(36F, 17F, 10.8F, 39F, 22F, 14F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(41F, 17F, 10.8F, 44F, 22F, 14F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(23F, 29F, 10.8F, 26F, 34F, 14F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(28F, 29F, 10.8F, 31F, 34F, 14F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(33F, 29F, 10.8F, 36F, 34F, 14F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(38F, 29F, 10.8F, 41F, 34F, 14F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
            cube(26F, 24F, 10.5F, 38F, 30F, 14F, uv(0F, 4F, 4F, 8F), uv(0F, 4F, 4F, 8F), uv(0F, 4F, 4F, 8F), uv(0F, 4F, 4F, 8F), uv(0F, 4F, 4F, 8F), uv(0F, 4F, 4F, 8F)),
            cube(3F, 42F, 8F, 61F, 47F, 56F, uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F)),
            cube(1F, 46F, 6F, 63F, 49F, 58F, uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F)),
            cube(8F, 47F, 12F, 56F, 53F, 52F, uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F)),
            cube(6F, 52F, 10F, 58F, 55F, 54F, uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F)),
            cube(14F, 53F, 18F, 50F, 59F, 46F, uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F), uv(4F, 0F, 8F, 4F)),
            cube(12F, 58F, 16F, 52F, 60F, 48F, uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F)),
            cube(25F, 59F, 24F, 39F, 64F, 40F, uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F), uv(8F, 4F, 12F, 8F)),
            cube(19F, 59F, 25F, 26F, 66F, 31F, uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F)),
            cube(14F, 64F, 25F, 20F, 71F, 31F, uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F)),
            cube(9F, 69F, 25F, 14F, 76F, 31F, uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F)),
            cube(4F, 74F, 25F, 9F, 80F, 31F, uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F)),
            cube(38F, 59F, 25F, 45F, 66F, 31F, uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F)),
            cube(44F, 64F, 25F, 50F, 71F, 31F, uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F)),
            cube(50F, 69F, 25F, 55F, 76F, 31F, uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F)),
            cube(55F, 74F, 25F, 60F, 80F, 31F, uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F)),
            cube(0F, 79F, 26F, 5F, 83F, 31F, uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F)),
            cube(59F, 79F, 26F, 64F, 83F, 31F, uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F), uv(12F, 0F, 16F, 4F)),
            cube(27F, 64F, 23F, 37F, 71F, 41F, uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F), uv(4F, 4F, 8F, 8F)),
            cube(29F, 66F, 20F, 35F, 72F, 24F, uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F), uv(8F, 0F, 12F, 4F)),
    };

    private ShrineModel() { }

    public static void render(PoseStack pose, VertexConsumer consumer, int packedLight) {
        Matrix4f matrix = pose.last().pose();
        for (Cube cube : CUBES) cube.render(matrix, consumer, packedLight);
    }

    private static Cube cube(float x1, float y1, float z1, float x2, float y2, float z2,
                             Uv north, Uv east, Uv south, Uv west, Uv top, Uv bottom) {
        return new Cube(x1, y1, z1, x2, y2, z2, north, east, south, west, top, bottom);
    }

    private static Uv uv(float u1, float v1, float u2, float v2) {
        return new Uv(u1 / TEXTURE_SIZE, v1 / TEXTURE_SIZE,
                u2 / TEXTURE_SIZE, v2 / TEXTURE_SIZE);
    }

    private record Uv(float u1, float v1, float u2, float v2) { }

    private static final class Cube {
        private final float x1, y1, z1, x2, y2, z2;
        private final Uv north, east, south, west, top, bottom;

        private Cube(float x1, float y1, float z1, float x2, float y2, float z2,
                     Uv north, Uv east, Uv south, Uv west, Uv top, Uv bottom) {
            this.x1 = (x1 - 32.0F) / 16.0F;
            this.y1 = y1 / 16.0F;
            this.z1 = (z1 - 32.0F) / 16.0F;
            this.x2 = (x2 - 32.0F) / 16.0F;
            this.y2 = y2 / 16.0F;
            this.z2 = (z2 - 32.0F) / 16.0F;
            this.north = north;
            this.east = east;
            this.south = south;
            this.west = west;
            this.top = top;
            this.bottom = bottom;
        }

        private void render(Matrix4f matrix, VertexConsumer consumer, int light) {
            face(matrix, consumer, light, north, 0, 0, -1,
                    x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
            face(matrix, consumer, light, south, 0, 0, 1,
                    x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2);
            face(matrix, consumer, light, west, -1, 0, 0,
                    x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1);
            face(matrix, consumer, light, east, 1, 0, 0,
                    x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
            face(matrix, consumer, light, bottom, 0, -1, 0,
                    x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2);
            face(matrix, consumer, light, top, 0, 1, 0,
                    x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        }

        private static void face(Matrix4f matrix, VertexConsumer consumer, int light, Uv uv,
                                 float nx, float ny, float nz,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float x3, float y3, float z3, float x4, float y4, float z4) {
            vertex(matrix, consumer, light, x1, y1, z1, uv.u1, uv.v1, nx, ny, nz);
            vertex(matrix, consumer, light, x2, y2, z2, uv.u1, uv.v2, nx, ny, nz);
            vertex(matrix, consumer, light, x3, y3, z3, uv.u2, uv.v2, nx, ny, nz);
            vertex(matrix, consumer, light, x4, y4, z4, uv.u2, uv.v1, nx, ny, nz);
        }

        private static void vertex(Matrix4f matrix, VertexConsumer consumer, int light,
                                   float x, float y, float z, float u, float v,
                                   float nx, float ny, float nz) {
            consumer.vertex(matrix, x, y, z).color(255, 255, 255, 255).uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(nx, ny, nz).endVertex();
        }
    }
}
