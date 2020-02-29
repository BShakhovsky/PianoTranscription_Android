attribute vec4 pos;
uniform mat4 mvo;

varying vec4 orthoPos;

void main() {
    gl_Position = orthoPos = mvo * pos;
}