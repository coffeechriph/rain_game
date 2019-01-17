#version 420 core

layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 color;
layout(location = 2) in vec2 uv;

layout(location = 0) out vec2 Uv;
layout(location = 1) out vec2 fpos;
layout(location = 2) out vec4 containerData;
layout(location = 3) out vec3 Color;

layout(push_constant) uniform Container {
    vec4 bounds;
} container;

layout(set = 0, binding = 0) uniform SceneData {
    mat4 projectionMatrix;
} sceneData;

void main() {
    vec2 cpos = vec2(container.bounds.x, container.bounds.y);
    vec2 csize = vec2(container.bounds.z, container.bounds.w);

    gl_Position = sceneData.projectionMatrix * vec4(cpos.x + pos.x, cpos.y + pos.y, pos.z, 1.0);
    Uv = uv;
    Color = color;
    containerData = vec4(cpos.x, cpos.y, csize.x, csize.y);
    fpos = vec2(container.bounds.x + pos.x, container.bounds.y + pos.y);
}
