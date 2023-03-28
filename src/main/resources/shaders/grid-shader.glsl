#type vertex
#version 460 core

uniform mat4 projection;
uniform mat4 view;
uniform ivec4 coords;
uniform int xLines;
uniform int gridSize;

void main() {
    int firstX = coords.x;
    int firstY = coords.y;
    int endX = coords.z;
    int endY = coords.w;

    if(gl_InstanceID < xLines) {
        int x = firstX + (gl_InstanceID * gridSize);
        if (gl_VertexID == 0) {
            gl_Position = projection * view * vec4(x, endY, 1, 1);
        } else {
            gl_Position = projection * view * vec4(x, firstY, 1, 1);
        }
    } else {
        int y = firstY + (gridSize * (gl_InstanceID - xLines));
        if (gl_VertexID == 0) {
            gl_Position = projection * view * vec4(firstX, y, 1, 1);
        } else {
            gl_Position = projection * view * vec4(endX, y, 1, 1);
        }
    }
}

#type fragment
#version 460 core

out vec4 color;

void main() {
    color = vec4(0, 0, 0, 1);
}