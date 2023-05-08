#type vertex
#version 460 core
layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in float vextexColor;

uniform mat4 projectionUniform;
uniform mat4 viewUniform;

out vec4 fragmentColor;

vec4 decodeARGB(float argb) {
    int intBits = floatBitsToInt(argb);
    int a = (intBits >> 24) & 0xFF;
    int r = (intBits >> 16) & 0xFF;
    int g = (intBits >> 8) & 0xFF;
    int b = intBits & 0xFF;
    return vec4(float(r) / 255.0, float(g) / 255.0, float(b) / 255.0, float(a) / 255.0);
}

void main() {
    fragmentColor = decodeARGB(vextexColor);
    gl_Position = projectionUniform * viewUniform * vec4(vertexPosition, 1.0);
}

#type fragment
#version 460 core

in vec4 fragmentColor;
out vec4 color;

void main() {
    color = fragmentColor;
}