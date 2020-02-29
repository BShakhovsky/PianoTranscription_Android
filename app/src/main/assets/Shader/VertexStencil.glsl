attribute vec4 pos;
uniform mat4 mvp;

void main() {
    gl_Position = mvp * pos;
}