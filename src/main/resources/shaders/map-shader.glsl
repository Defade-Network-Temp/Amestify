#type vertex
#version 460 core
layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in float vextexColor;
layout(location = 2) in float vertexBiomeId;
layout(location = 3) in vec2 vertexTexCoords;
layout(location = 4) in float vertexTexId;
layout(std140, binding = 0) buffer BiomeColors {
    vec4 biomeColors[];
};

uniform mat4 projectionUniform;
uniform mat4 viewUniform;
uniform bool displayBiomeColor;
uniform float highlightedBiome;

out vec3 fragmentColor;
out vec2 fragmentTexCoords;
out float fragmentTexId;

vec3 decodeRGB(float rgb) {
    int intBits = floatBitsToInt(rgb);
    int r = (intBits >> 16) & 0xFF;
    int g = (intBits >> 8) & 0xFF;
    int b = intBits & 0xFF;
    return vec3(float(r) / 255.0, float(g) / 255.0, float(b) / 255.0);
}

void main() {
    if(displayBiomeColor) {
        fragmentColor = biomeColors[int(vertexBiomeId)].rgb;
    } else {
        fragmentColor = decodeRGB(vextexColor);
    }

    if(highlightedBiome == vertexBiomeId) fragmentColor *= vec3(0.5, 0.5, 0.5);

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