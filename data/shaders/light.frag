#version 420 core

layout(location = 0) out vec4 color;
layout (location = 0) in vec4 Light;

void main() {
    float distance = Light.w;
    color.rgb = Light.rgb * distance;
    color.a = distance;
    if (color.w < 0.01f) {
        discard;
    }
}
