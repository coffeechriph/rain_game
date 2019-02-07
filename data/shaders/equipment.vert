#version 420 core

layout(location = 0) in vec2 pos;
layout(location = 1) in vec2 uv;

layout(location = 0) out vec2 Uv;
layout(location = 1) out vec4 Color;
layout(location = 2) out float Intensity;

layout(push_constant) uniform ModelMatrix {
    mat4 matrix;
    vec4 color;
    vec2 textureOffset;
    float equipmentOffset;
    float intensity;
} customData;

layout(set = 0, binding = 0) uniform SceneData {
    mat4 projectionMatrix;
} sceneData;

layout(set = 0, binding = 1) uniform TextureData {
    vec2 uvScale;
} textureData;

void main() {
    gl_Position = sceneData.projectionMatrix * customData.matrix * vec4(pos.x, pos.y, 1.0, 1.0);
    Uv = vec2(uv.x * textureData.uvScale.x + textureData.uvScale.x * (customData.textureOffset.x+customData.equipmentOffset),
              uv.y * textureData.uvScale.y + textureData.uvScale.y * customData.textureOffset.y);
    Color = customData.color;
    Intensity = customData.intensity;
}
