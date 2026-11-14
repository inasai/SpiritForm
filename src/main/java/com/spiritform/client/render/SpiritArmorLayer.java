package com.spiritform.client.render;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.SpiritTransparency;
import com.spiritform.data.SpiritData;
import com.spiritform.form.SpiritFormState;
import com.spiritform.registry.SpiritItems;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Малює 3D-моделі обладунків Потойбіччя на гравці. Кожна частина моделі кріпиться до своєї частини тіла
 * (голова, тулуб, ліва й права нога) і рухається разом із нею: при ході, присіданні, повороті голови.
 *
 * Моделі: assets/spirit_form/models/armor/*.json (ті самі елементи, що й в іконках, лише розрізані по частинах),
 * текстури: assets/spirit_form/textures/item/spirit_*.png. Ванільна плоска броня (textures/models/armor)
 * прозора, щоб не заважати.
 *
 * Положення й розмір підганяються без перекомпіляції у файлі config/spirit_form-armor-fit.json
 * (перечитується сам щосекунди, навіть під час гри):
 *   scale: розмір (1 = як у моделі), dx: убік (+ до лівого боку гравця), dy: угору (+), dz: уперед (+), у пікселях.
 */
public class SpiritArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private enum Anchor { HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }

    /** Яка точка моделі (за її габаритами) ставиться в опорну точку: центр, верх чи низ по висоті. */
    private enum Mode { CENTER, TOP, BOTTOM }

    /** wide: у скільки разів частина ширша й глибша за модель (висота не змінюється), щоб лежати поверх кінцівки. */
    private record Part(String file, Anchor anchor, Mode mode, float tx, float ty, float tz, float wide) {
        Part(String file, Anchor anchor, Mode mode, float tx, float ty, float tz) {
            this(file, anchor, mode, tx, ty, tz, 1.0F);
        }
    }

    private enum Piece {
        HELMET("spirit_helmet", EquipmentSlot.HEAD, 0.47,
                new Part("spirit_helmet", Anchor.HEAD, Mode.CENTER, 0.0F, -4.0F, -4.45F)),
        CHESTPLATE("spirit_chestplate", EquipmentSlot.CHEST, 1.1,
                new Part("spirit_chestplate_right", Anchor.RIGHT_ARM, Mode.TOP, -1.0F, -2.4F, 0.0F),
                new Part("spirit_chestplate_left", Anchor.LEFT_ARM, Mode.TOP, 1.0F, -2.4F, 0.0F)),
        LEGGINGS("spirit_leggings", EquipmentSlot.LEGS, 1.0,
                new Part("spirit_leggings_left", Anchor.LEFT_LEG, Mode.BOTTOM, 0.0F, 12.0F, 0.0F, 1.3F),
                new Part("spirit_leggings_right", Anchor.RIGHT_LEG, Mode.BOTTOM, 0.0F, 12.0F, 0.0F, 1.3F)),
        BOOTS("spirit_boots", EquipmentSlot.FEET, 0.75,
                new Part("spirit_boots_left", Anchor.LEFT_LEG, Mode.BOTTOM, 0.0F, 12.0F, 0.0F),
                new Part("spirit_boots_right", Anchor.RIGHT_LEG, Mode.BOTTOM, 0.0F, 12.0F, 0.0F));

        final String name;
        final EquipmentSlot slot;
        final double defaultScale;
        final List<Part> parts;
        final ResourceLocation texture;

        Piece(String name, EquipmentSlot slot, double defaultScale, Part... parts) {
            this.name = name;
            this.slot = slot;
            this.defaultScale = defaultScale;
            this.parts = List.of(parts);
            this.texture = ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "textures/item/" + name + ".png");
        }
    }

    private static final class Fit {
        double scale;
        double dx;
        double dy;
        double dz;
    }

    /** Одна грань: чотири вершини (x, y, z у пікселях моделі), uv у діапазоні 0..1. */
    private record Quad(float[] x, float[] y, float[] z, float[] u, float[] v) {
    }

    private record Mesh(List<Quad> quads, float ax, float ay, float az, float topY, float bottomY) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SpiritArmorLayer.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Piece, Fit> FITS = new EnumMap<>(Piece.class);
    private static final Map<String, Mesh> MESHES = new HashMap<>();
    private static ResourceManager meshesFrom;
    private static long loadedStamp = Long.MIN_VALUE;
    private static long lastCheck;

    static {
        defaults();
    }

    private static void defaults() {
        for (Piece piece : Piece.values()) {
            Fit fit = FITS.computeIfAbsent(piece, p -> new Fit());
            fit.scale = piece.defaultScale;
            fit.dx = 0.0;
            fit.dy = 0.0;
            fit.dz = 0.0;
        }
    }

    // ---------- налаштування положення ----------

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("spirit_form-armor-fit.json");
    }

    private static void reloadIfChanged() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < 1000) {
            return;
        }
        lastCheck = now;
        Path file = path();
        try {
            if (!Files.exists(file)) {
                defaults();
                save(file);
                loadedStamp = Files.getLastModifiedTime(file).toMillis();
                return;
            }
            long stamp = Files.getLastModifiedTime(file).toMillis();
            if (stamp == loadedStamp) {
                return;
            }
            loadedStamp = stamp;
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            defaults();
            for (Piece piece : Piece.values()) {
                if (!root.has(piece.name) || !root.get(piece.name).isJsonObject()) {
                    continue;
                }
                JsonObject o = root.getAsJsonObject(piece.name);
                Fit fit = FITS.get(piece);
                fit.scale = num(o, "scale", fit.scale);
                fit.dx = num(o, "dx", fit.dx);
                fit.dy = num(o, "dy", fit.dy);
                fit.dz = num(o, "dz", fit.dz);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Не вдалося прочитати {}: {}", file, e.toString());
        }
    }

    private static double num(JsonObject o, String key, double fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : fallback;
    }

    private static void save(Path file) throws IOException {
        JsonObject root = new JsonObject();
        for (Piece piece : Piece.values()) {
            Fit f = FITS.get(piece);
            JsonObject o = new JsonObject();
            o.addProperty("scale", f.scale);
            o.addProperty("dx", f.dx);
            o.addProperty("dy", f.dy);
            o.addProperty("dz", f.dz);
            root.add(piece.name, o);
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    // ---------- читання моделі з JSON ----------

    /** Вершини граней у тому ж порядку, що й у ванільній моделі блоків: (x0,y0,z0) .. (x1,y1,z1). */
    private static float[][] corners(String face, float x0, float y0, float z0, float x1, float y1, float z1) {
        return switch (face) {
            case "north" -> new float[][] {{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
            case "south" -> new float[][] {{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
            case "west" -> new float[][] {{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
            case "east" -> new float[][] {{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
            case "up" -> new float[][] {{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case "down" -> new float[][] {{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
            default -> null;
        };
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    private static Mesh mesh(String file) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager != meshesFrom) {
            MESHES.clear();
            meshesFrom = manager;
        }
        Mesh cached = MESHES.get(file);
        if (cached != null) {
            return cached;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "models/armor/" + file + ".json");
        List<Quad> quads = new ArrayList<>();
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        try (Reader reader = new InputStreamReader(manager.open(id), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("elements")) {
                JsonObject e = el.getAsJsonObject();
                float[] from = floats(e.getAsJsonArray("from"));
                float[] to = floats(e.getAsJsonArray("to"));
                float angle = 0.0F;
                String axis = "y";
                float[] origin = {0.0F, 0.0F, 0.0F};
                if (e.has("rotation") && e.get("rotation").isJsonObject()) {
                    JsonObject r = e.getAsJsonObject("rotation");
                    angle = r.has("angle") ? r.get("angle").getAsFloat() : 0.0F;
                    axis = r.has("axis") ? r.get("axis").getAsString() : "y";
                    origin = r.has("origin") ? floats(r.getAsJsonArray("origin")) : origin;
                }
                double rad = Math.toRadians(angle);
                float cos = (float) Math.cos(rad);
                float sin = (float) Math.sin(rad);
                JsonObject faces = e.getAsJsonObject("faces");
                for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                    float[][] c = corners(entry.getKey(), from[0], from[1], from[2], to[0], to[1], to[2]);
                    JsonObject face = entry.getValue().getAsJsonObject();
                    if (c == null || !face.has("uv")) {
                        continue;
                    }
                    float[] uv = floats(face.getAsJsonArray("uv"));
                    float[] xs = new float[4];
                    float[] ys = new float[4];
                    float[] zs = new float[4];
                    for (int i = 0; i < 4; i++) {
                        float px = c[i][0] - origin[0];
                        float py = c[i][1] - origin[1];
                        float pz = c[i][2] - origin[2];
                        float rx = px;
                        float ry = py;
                        float rz = pz;
                        if (angle != 0.0F) {
                            switch (axis) {
                                case "x" -> {
                                    ry = py * cos - pz * sin;
                                    rz = py * sin + pz * cos;
                                }
                                case "z" -> {
                                    rx = px * cos - py * sin;
                                    ry = px * sin + py * cos;
                                }
                                default -> {
                                    rx = px * cos + pz * sin;
                                    rz = -px * sin + pz * cos;
                                }
                            }
                        }
                        xs[i] = rx + origin[0];
                        ys[i] = ry + origin[1];
                        zs[i] = rz + origin[2];
                        minX = Math.min(minX, xs[i]);
                        maxX = Math.max(maxX, xs[i]);
                        minY = Math.min(minY, ys[i]);
                        maxY = Math.max(maxY, ys[i]);
                        minZ = Math.min(minZ, zs[i]);
                        maxZ = Math.max(maxZ, zs[i]);
                    }
                    float u0 = uv[0] / 16.0F;
                    float v0 = uv[1] / 16.0F;
                    float u1 = uv[2] / 16.0F;
                    float v1 = uv[3] / 16.0F;
                    quads.add(new Quad(xs, ys, zs, new float[] {u0, u0, u1, u1}, new float[] {v0, v1, v1, v0}));
                }
            }
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Не вдалося прочитати модель {}: {}", id, ex.toString());
        }
        Mesh mesh = quads.isEmpty()
                ? new Mesh(quads, 0, 0, 0, 0, 0)
                : new Mesh(quads, (minX + maxX) / 2.0F, (minY + maxY) / 2.0F, (minZ + maxZ) / 2.0F, maxY, minY);
        MESHES.put(file, mesh);
        return mesh;
    }

    // ---------- малювання ----------

    public SpiritArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static boolean wears(AbstractClientPlayer player, Piece piece) {
        Item item = player.getItemBySlot(piece.slot).getItem();
        return switch (piece) {
            case HELMET -> item == SpiritItems.SPIRIT_HELMET.get();
            case CHESTPLATE -> item == SpiritItems.SPIRIT_CHESTPLATE.get();
            case LEGGINGS -> item == SpiritItems.SPIRIT_LEGGINGS.get();
            case BOOTS -> item == SpiritItems.SPIRIT_BOOTS.get();
        };
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        SpiritData data = SpiritFormState.get(player);
        if (player.isInvisible() && !data.active()) {
            return;
        }
        boolean ghost = data.active() || data.invisible();
        int color = ghost ? SpiritTransparency.GHOST_COLOR : -1;
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        reloadIfChanged();

        for (Piece piece : Piece.values()) {
            if (!wears(player, piece)) {
                continue;
            }
            Fit fit = FITS.get(piece);
            VertexConsumer consumer = buffer.getBuffer(ghost
                    ? RenderType.entityTranslucent(piece.texture)
                    : RenderType.entityCutoutNoCull(piece.texture));
            for (Part part : piece.parts) {
                ModelPart target = switch (part.anchor()) {
                    case HEAD -> model.head;
                    case LEFT_ARM -> model.leftArm;
                    case RIGHT_ARM -> model.rightArm;
                    case LEFT_LEG -> model.leftLeg;
                    case RIGHT_LEG -> model.rightLeg;
                    default -> model.body;
                };
                draw(poseStack, consumer, target, part, fit, packedLight, color);
            }
        }
    }

    private static void draw(PoseStack poseStack, VertexConsumer consumer, ModelPart target, Part part, Fit fit,
                             int light, int color) {
        Mesh mesh = mesh(part.file());
        if (mesh.quads().isEmpty()) {
            return;
        }
        float s = (float) fit.scale;
        float ay = switch (part.mode()) {
            case TOP -> mesh.topY();
            case BOTTOM -> mesh.bottomY();
            default -> mesh.ay();
        };
        float tx = part.tx() + (float) fit.dx;
        float ty = part.ty() - (float) fit.dy;
        float tz = part.tz() - (float) fit.dz;

        poseStack.pushPose();
        target.translateAndRotate(poseStack);
        // Далі координати в пікселях частини тіла; модель повернута на 180 градусів навколо осі Z (x і y міняють знак)
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        for (Quad q : mesh.quads()) {
            float[] px = new float[4];
            float[] py = new float[4];
            float[] pz = new float[4];
            for (int i = 0; i < 4; i++) {
                px[i] = tx - s * part.wide() * (q.x()[i] - mesh.ax());
                py[i] = ty - s * (q.y()[i] - ay);
                pz[i] = tz + s * part.wide() * (q.z()[i] - mesh.az());
            }
            // Нормаль грані з трьох вершин (порядок вершин проти годинникової стрілки зовні)
            float ux = px[1] - px[0];
            float uy = py[1] - py[0];
            float uz = pz[1] - pz[0];
            float vx = px[2] - px[1];
            float vy = py[2] - py[1];
            float vz = pz[2] - pz[1];
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0E-6F) {
                continue;
            }
            nx /= len;
            ny /= len;
            nz /= len;
            for (int i = 0; i < 4; i++) {
                consumer.addVertex(pose, px[i], py[i], pz[i])
                        .setColor(color)
                        .setUv(q.u()[i], q.v()[i])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(pose, nx, ny, nz);
            }
        }
        poseStack.popPose();
    }
}
