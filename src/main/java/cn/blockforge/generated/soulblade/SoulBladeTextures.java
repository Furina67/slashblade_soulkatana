package cn.blockforge.generated.soulblade;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「刀」的全部特效贴图路径总入口：渲染器代码里不再写死任何 png 路径，
 * 统一从客户端配置文件 config/soulblade-client.toml 读取。
 *
 * 改贴图的两条路（都不需要重新编译模组）：
 *  1. 改 config/soulblade-client.toml 里的资源路径 → 直接指向另一张图
 *     （可以是本附属、主模组 slashblade、原版 minecraft 或任何已加载资源包的命名空间）；
 *  2. 路径不动，用资源包覆盖对应 png 文件 → 换皮不换配置。
 * 配置项被删掉或写成非法路径时自动退回内置默认值并打一条 warning，不会崩溃。
 *
 * 注：刀本体（皮肤）的贴图不在这里 —— 它由主模组读取
 * data/soulblade/slashblade/named_blades/ryomen_sukuna.json 的 render 段，
 * 那个 JSON 本身就是外部数据文件；皮肤 png 存放在本附属的
 * assets/soulblade/textures/model/ryomen_sukuna.png，直接换文件即可改刀面。
 */
public final class SoulBladeTextures {
    private static final Logger LOGGER = LoggerFactory.getLogger("soulblade-textures");

    /** 领域斩击贴图的样式数量（样式 0/1/2）。 */
    public static final int SLASH_STYLE_COUNT = 3;

    // ---- 内置默认路径（配置缺失/写错时兜底） ----
    private static final String DEFAULT_CRESCENT = "soulblade:textures/entity/crescent.png";
    private static final String DEFAULT_SHRINE = "soulblade:textures/entity/shrine.png";
    private static final List<String> DEFAULT_SLASH_FRAMES_0 = numberedFrames("slash_frame_", 4);
    private static final List<String> DEFAULT_SLASH_FRAMES_1 = numberedFrames("red_edge_frame_", 3);
    private static final List<String> DEFAULT_SLASH_FRAMES_2 = numberedFrames("red_frame_", 3);
    private static final ResourceLocation[] FALLBACK_FRAMES_0 = toLocations(DEFAULT_SLASH_FRAMES_0);
    private static final ResourceLocation[] FALLBACK_FRAMES_1 = toLocations(DEFAULT_SLASH_FRAMES_1);
    private static final ResourceLocation[] FALLBACK_FRAMES_2 = toLocations(DEFAULT_SLASH_FRAMES_2);

    /** 供模组构造器注册：ModConfig.Type.CLIENT。 */
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.ConfigValue<String> CRESCENT_TEXTURE;
    private static final ForgeConfigSpec.ConfigValue<String> SHRINE_TEXTURE;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLASH_FRAMES_0;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLASH_FRAMES_1;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLASH_FRAMES_2;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(
                "拔刀剑附属·魂刀 —— 贴图路径配置。",
                "每一项都是一个资源路径（命名空间:文件夹/文件.png），改路径就换贴图，无需重新编译模组。",
                "也可以不改这里，直接用资源包覆盖模组 assets 里的同名 png 来换皮。",
                "留空或写错会自动退回默认值（日志里能看到 warning）。")
                .push("textures");

        CRESCENT_TEXTURE = builder
                .comment("[解] 月牙波动的贴图")
                .define("crescent_texture", DEFAULT_CRESCENT);
        SHRINE_TEXTURE = builder
                .comment("[领域展开] 神龛（骨骸鸟居）实体的贴图")
                .define("shrine_texture", DEFAULT_SHRINE);
        SLASH_FRAMES_0 = builder
                .comment("领域斩击·样式0（原白斩）帧序列，按列表顺序播放；增删条目即可改帧数")
                .defineList("domain_slash_frames_0", DEFAULT_SLASH_FRAMES_0, SoulBladeTextures::isText);
        SLASH_FRAMES_1 = builder
                .comment("领域斩击·样式1（红边斩）帧序列")
                .defineList("domain_slash_frames_1", DEFAULT_SLASH_FRAMES_1, SoulBladeTextures::isText);
        SLASH_FRAMES_2 = builder
                .comment("领域斩击·样式2（赤红斩）帧序列")
                .defineList("domain_slash_frames_2", DEFAULT_SLASH_FRAMES_2, SoulBladeTextures::isText);

        builder.pop();
        SPEC = builder.build();
    }

    // ---- 解析缓存（同一字符串只解析一次；字符串即键，配置重载天然安全） ----
    private static final Map<String, ResourceLocation> PARSED = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private SoulBladeTextures() { }

    /** [解] 月牙贴图。 */
    public static ResourceLocation crescent() {
        return resolve(CRESCENT_TEXTURE.get(), DEFAULT_CRESCENT, "crescent_texture");
    }

    /** 神龛贴图。 */
    public static ResourceLocation shrine() {
        return resolve(SHRINE_TEXTURE.get(), DEFAULT_SHRINE, "shrine_texture");
    }

    /** 领域斩击某一样式的帧贴图序列（style 越界自动收进合法范围；返回数组至少 1 帧）。 */
    public static ResourceLocation[] domainSlashFrames(int style) {
        int clamped = Math.max(0, Math.min(SLASH_STYLE_COUNT - 1, style));
        return switch (clamped) {
            case 1 -> resolveFrames(SLASH_FRAMES_1.get(), DEFAULT_SLASH_FRAMES_1, FALLBACK_FRAMES_1, "domain_slash_frames_1");
            case 2 -> resolveFrames(SLASH_FRAMES_2.get(), DEFAULT_SLASH_FRAMES_2, FALLBACK_FRAMES_2, "domain_slash_frames_2");
            default -> resolveFrames(SLASH_FRAMES_0.get(), DEFAULT_SLASH_FRAMES_0, FALLBACK_FRAMES_0, "domain_slash_frames_0");
        };
    }

    // ---- 内部工具 ----

    private static ResourceLocation resolve(String configured, String fallback, String key) {
        String raw = configured == null || configured.isBlank() ? fallback : configured.trim();
        ResourceLocation loc = PARSED.get(raw);
        if (loc == null) {
            loc = ResourceLocation.tryParse(raw);
            if (loc == null) {
                warnOnce(key + '\0' + raw, "配置项 {} 的贴图路径不合法：\"{}\"，已退回默认 {}", key, raw, fallback);
                loc = parseDefault(fallback);
            } else {
                PARSED.put(raw, loc);
            }
        }
        return loc;
    }

    private static ResourceLocation[] resolveFrames(List<? extends String> configured,
                                                    List<String> fallbackDefaults,
                                                    ResourceLocation[] fallback, String key) {
        if (configured == null || configured.isEmpty()) {
            warnOnce(key + '\0' + "<empty>", "配置项 {} 为空，已退回默认帧序列", key);
            return fallback;
        }
        List<String> raws = new ArrayList<>(configured.size());
        for (String entry : configured) {
            if (entry == null || entry.isBlank()) {
                warnOnce(key + '\0' + "<blank>", "配置项 {} 含空条目，已退回默认帧序列（本次启动内只提示一次）", key);
                return fallback;
            }
            raws.add(entry.trim());
        }
        ResourceLocation[] out = new ResourceLocation[raws.size()];
        for (int i = 0; i < out.length; i++) {
            String raw = raws.get(i);
            ResourceLocation loc = PARSED.get(raw);
            if (loc == null) {
                loc = ResourceLocation.tryParse(raw);
                if (loc == null) {
                    warnOnce(key + '\0' + raw, "配置项 {} 的条目 \"{}\" 不是合法资源路径，已退回默认帧序列", key, raw);
                    return fallback;
                }
                PARSED.put(raw, loc);
            }
            out[i] = loc;
        }
        return out;
    }

    private static void warnOnce(String marker, String messageTemplate, Object... args) {
        if (WARNED.add(SoulBlade.MODID + '\0' + marker)) {
            LOGGER.warn(messageTemplate, args);
        }
    }

    private static ResourceLocation parseDefault(String raw) {
        ResourceLocation loc = PARSED.get(raw);
        if (loc == null) {
            loc = ResourceLocation.tryParse(raw);
            if (loc == null) {
                throw new IllegalStateException("内置默认贴图路径不合法: " + raw);
            }
            PARSED.put(raw, loc);
        }
        return loc;
    }

    private static boolean isText(Object element) {
        return element instanceof String;
    }

    private static List<String> numberedFrames(String prefix, int count) {
        List<String> frames = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            frames.add("soulblade:textures/entity/" + prefix + i + ".png");
        }
        return List.copyOf(frames);
    }

    private static ResourceLocation[] toLocations(List<String> raws) {
        ResourceLocation[] out = new ResourceLocation[raws.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = parseDefault(raws.get(i));
        }
        return out;
    }
}
