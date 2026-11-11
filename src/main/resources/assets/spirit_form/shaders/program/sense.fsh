#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform vec2 OutSize;
uniform float Strength;

in vec2 texCoord;
out vec4 fragColor;

const vec3 MINT = vec3(0.37, 0.88, 0.71);

// Глибина буфера -> відстань від камери (близько/далеко, як у ванільної камери)
float linearDepth(float d) {
    float n = 0.05;
    float f = 512.0;
    float z = d * 2.0 - 1.0;
    return 2.0 * n * f / (f + n - z * (f - n));
}

// Обернена відстань для площини змінюється лінійно по екрану, тож другі різниці дають нуль на рівних поверхнях
float invDepth(vec2 uv) {
    return 1.0 / linearDepth(texture(DiffuseDepthSampler, uv).r);
}

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    vec2 px = 1.0 / OutSize;

    float i0 = invDepth(texCoord);
    float l = invDepth(texCoord + vec2(-px.x, 0.0));
    float r = invDepth(texCoord + vec2(px.x, 0.0));
    float u = invDepth(texCoord + vec2(0.0, px.y));
    float d = invDepth(texCoord + vec2(0.0, -px.y));

    float lap = abs(l + r - 2.0 * i0) + abs(u + d - 2.0 * i0);
    float edge = smoothstep(0.03, 0.10, lap / max(i0, 0.0001));

    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 muted = mix(color.rgb, vec3(luma), 0.55 * Strength) * (1.0 - 0.30 * Strength);
    muted += MINT * 0.03 * Strength;

    vec3 result = mix(muted, MINT * 0.85, edge * 0.35 * Strength);
    fragColor = vec4(result, 1.0);
}
