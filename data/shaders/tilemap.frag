#version 420 core

layout(location = 0) out vec4 color;
layout(set = 0, binding = 2) uniform sampler2D texSampler;

layout(location = 0) in vec2 Uv;
void main() {
    color = texture(texSampler, Uv);
    if (color.w < 0.01f) {
        discard;
    }
}
