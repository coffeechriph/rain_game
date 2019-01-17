#version 420 core

layout(location = 0) out vec4 color;
layout (location = 0) in vec4 inColor;
layout (location = 1) in vec2 Uv;

layout (set = 0, binding = 2) uniform sampler2D texture0;

void main() {
    color = inColor * texture(texture0, Uv);

    if (color.w < 0.01f) {
        discard;
    }
}
