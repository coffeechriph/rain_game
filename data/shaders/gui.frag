#version 420 core

layout(location = 0) out vec4 color;

layout(location = 0) in vec3 Color;
layout(location = 1) in vec2 fpos;
layout(location = 2) in vec4 containerData;

void main() {
    if (fpos.x < containerData.x || fpos.x > containerData.x + containerData.z ||
        fpos.y < containerData.y || fpos.y > containerData.y + containerData.w) {
      discard;
    }
    else {
      color = vec4(Color, 1.0f);
    }
}
