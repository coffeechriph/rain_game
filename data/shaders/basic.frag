#version 420 core

layout(location = 0) out vec4 color;
layout(set = 0, binding = 2) uniform sampler2D texSampler;

layout(location = 0) in vec2 Uv;
layout(location = 1) in vec4 Color;
layout(location = 2) in float Intensity;

void main() {
    color = texture(texSampler, Uv);
    color.rgb += Color.rgb;
    color.rgb *= Intensity;
    if (color.w < 0.01f) {
        discard;
    }
}
