#version 150

// 领域展开·后处理：屏幕四周柔和暗化 + 刚好出现色彩重影的径向色差 + 暗红色调偏置。
// InSize / Time 由原版 PostPass 每帧写入：前者是输入缓冲的像素尺寸，后者是秒数，
// 用来让重影的摆幅有节律地呼吸。
uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform vec2 InSize;
uniform float Time;

out vec4 fragColor;

void main() {
    vec2 centered = texCoord - vec2(0.5);
    float radius = length(centered);
    float breath = 0.85 + 0.15 * sin(Time * 3.14159 + radius * 6.0);

    // 色差：红/蓝通道沿半径反向偏移，中心几乎为零、屏幕四周最大。
    //
    // 上一版"色差像没生效"的真正原因不是幅度不够，而是偏移量整个算在 UV 空间里：
    // UV 是 0~1 的正方形，16:9 屏上同样的数值横向摊到 1920 像素、纵向只摊到 1080
    // 像素，于是左右能看见一点、上下几乎没有，整体观感就是"暗角有了、重影没有"。
    // 这一版先把位置换算到像素空间，按"离屏幕中心多少像素"定重影宽度，再除回各轴
    // 自己的像素数变回 UV —— 这样四周的重影宽度一致，任何分辨率下都稳定可见。
    vec2 size = max(InSize, vec2(1.0));
    vec2 shift;
    if (size.x < 16.0 || size.y < 16.0) {
        // 兜底：万一拿不到真实缓冲尺寸，退回保守的 UV 比例偏移，绝不整屏撕裂。
        shift = centered * (0.0045 + 0.0035 * radius) * breath;
    } else {
        vec2 pixelPos = centered * size;
        float radiusPx = length(pixelPos);
        // 归一化半径：以屏幕竖直半高为 1.0，上下边缘 = 1.0，左右边缘 ≈ 1.78，四角更大。
        float t = clamp(radiusPx / max(0.5 * size.y, 1.0), 0.0, 1.35);
        // 方向向量：用 max(radiusPx, 1.0) 兜住除零，正中心 radiusPx = 0 → dir = 0，
        // 中心自然没有位移（也避开 GLSL 里对 vec 用三目运算符的兼容性坑）。
        vec2 dir = pixelPos / max(radiusPx, 1.0);
        // 单通道重影宽度（像素）：中心 0.5 看不出来，上下边缘约 5.3、左右与四角约 7.7。
        // 实测 1080p 下红蓝总分离：中心 0 像素、正上边缘 10.6 像素、左边缘与四角 15.5 像素
        // ——高对比轮廓上是明确可辨的色彩重影，画面中心依旧干净（CPU 复刻校验见
        // tools_check_domain_shader.py）。
        float ghostPx = (0.5 + 3.2 * t + 1.6 * t * t) * breath;
        shift = dir * (ghostPx / size);
    }

    vec3 col;
    col.r = texture(DiffuseSampler, texCoord + shift).r;
    col.g = texture(DiffuseSampler, texCoord).g;
    col.b = texture(DiffuseSampler, texCoord - shift).b;

    // 暗红氛围：轻压绿通道、微抬红通道，画面整体往血色偏。
    col = mix(col, col * vec3(1.10, 0.84, 0.90), 0.55);

    // 柔和暗化（vignette）：中心保持原亮度，向屏幕四周平滑压暗。
    // 写成 1.0 - smoothstep(小, 大, x)：GLSL 规范里 smoothstep 的 edge0 >= edge1 属于
    // 未定义行为（多数驱动碰巧按线性反插值算，个别驱动会直接给常数），换等价形式后
    // 暗化在任何显卡上都稳定。
    float vignette = 1.0 - smoothstep(0.35, 0.95, radius * 1.35);
    col *= mix(0.45, 1.0, vignette);

    fragColor = vec4(col, 1.0);
}
