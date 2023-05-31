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
uniform float biomeHighlightColorMultiplier;

out vec3 fragmentColor;
out float chunkDisabled;
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
    int decompressedVertexBiomeId = floatBitsToInt(vertexBiomeId) & 0x3FFFFFFF;

    if(displayBiomeColor) {
        fragmentColor = biomeColors[decompressedVertexBiomeId].rgb;
    } else {
        fragmentColor = decodeRGB(vextexColor);
    }

    chunkDisabled = floatBitsToInt(vertexBiomeId) >> 30;

    if(highlightedBiome == decompressedVertexBiomeId) fragmentColor *= biomeHighlightColorMultiplier;

    fragmentTexCoords = vertexTexCoords;
    fragmentTexId = vertexTexId;

    gl_Position = projectionUniform * viewUniform * vec4(vertexPosition, 1.0);
}

#type fragment
#version 460 core

in vec3 fragmentColor;
in float chunkDisabled;
in vec2 fragmentTexCoords;
in float fragmentTexId;

uniform sampler2DArray textureSampler;

out vec4 color;

void main() {
    color = texture(textureSampler, vec3(fragmentTexCoords, int(fragmentTexId)));
    color.rgb *= fragmentColor;

    if(chunkDisabled == 1) {
        float gray = 0.21 * color.r + 0.71 * color.g + 0.07 * color.b;
        color = vec4(gray, gray, gray, color.a);

        vec2 uv = gl_FragCoord.xy / vec2(1920, 1080);

        float diagonalOffset = 0;
        float diagonalDensity = 150.0;

        float stripeIndex = floor((uv.x + uv.y + diagonalOffset) * diagonalDensity);
        float stripePattern = mod(stripeIndex, 2.0);

        if (stripePattern < 1) {
            color = (vec4(0, 0, 0, 1) + color) / 2;
        }
    }
}