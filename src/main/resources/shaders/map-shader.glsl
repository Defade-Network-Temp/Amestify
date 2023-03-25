#type vertex
#version 460 core
layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in float vextexColor;
layout(location = 2) in vec2 vertexTexCoords;
layout(location = 3) in float vertexTexId;

uniform mat4 projectionUniform;
uniform mat4 viewUniform;

out vec3 fragmentColor;
out vec2 fragmentTexCoords;
out float fragmentTexId;

vec3 unpackColor(float rgb) {
    vec3 color;
    color.r = floor(rgb / 256.0 / 256.0);
    color.g = floor((rgb - color.r * 256.0 * 256.0) / 256.0);
    color.b = floor(rgb - color.r * 256.0 * 256.0 - color.g * 256.0);

    return color / 255.0;
}

void main() {
    fragmentColor = unpackColor(vextexColor);
    fragmentTexCoords = vertexTexCoords;
    fragmentTexId = vertexTexId;

    gl_Position = projectionUniform * viewUniform * vec4(vertexPosition, 1.0);
}

#type fragment
#version 460 core

in vec3 fragmentColor;
in vec2 fragmentTexCoords;
in float fragmentTexId;

uniform sampler2DArray textureSampler;

out vec4 color;

void main() {
    color = texture(textureSampler, vec3(fragmentTexCoords, int(fragmentTexId)));
    color.rgb *= fragmentColor;
}