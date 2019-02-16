#version 420 core

layout(location = 0) in ivec2 pos;
layout(location = 1) in int uv;

layout(push_constant) uniform ModelMatrix {
    mat4 matrix;
    vec4 startColor;
    vec4 endColor;
} inData;

layout(set = 0, binding = 0) uniform SceneData {
    mat4 projectionMatrix;
} sceneData;

layout(set = 0, binding = 1) uniform TextureData {
  vec2 uvScale;
} textureData;

layout (location = 0) out vec4 color;
layout (location = 1) out vec2 Uv;

void main() {
    vec2 TextureOffset = vec2(0.0f, 0.0f);
    float x = float(pos.x) * 0.01;
    float y = float(pos.y) * 0.01;
    float z = float((uv >> 2)) / 1000.0f;
    color = mix(inData.startColor, inData.endColor, z);
    gl_Position = sceneData.projectionMatrix * inData.matrix * vec4(x, y, 1.0 - z, 1.0);

    float u = float(uv&1);
    float v = float((uv>>1)&1);
    Uv = vec2(u,v);
}
