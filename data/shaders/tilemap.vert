#version 420 core

layout(location = 0) in vec2 pos;
layout(location = 1) in vec2 uv;

layout(location = 0) out vec2 Uv;

layout(push_constant) uniform ModelMatrix {
    mat4 matrix;
    vec2 textureOffset;
} modelMatrix;

layout(set = 0, binding = 0) uniform SceneData {
    mat4 projectionMatrix;
} sceneData;

layout(set = 0, binding = 1) uniform TextureData {
    vec2 uvScale;
} textureData;

void main() {
    gl_Position = sceneData.projectionMatrix * modelMatrix.matrix * vec4(pos.x, pos.y, 1.0, 1.0);
    Uv = uv;
}
