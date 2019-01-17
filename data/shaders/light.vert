#version 420 core

layout(location = 0) in vec2 pos;
layout(location = 1) in vec4 light;

layout(location = 0) out vec4 Light;

layout(push_constant) uniform ModelMatrix {
    mat4 matrix;
    vec2 textureOffset;
} modelMatrix;

layout(set = 0, binding = 0) uniform SceneData {
    mat4 projectionMatrix;
} sceneData;

void main() {
    gl_Position = sceneData.projectionMatrix * modelMatrix.matrix * vec4(pos.x, pos.y, 1.0, 1.0);
    Light = light;
}
