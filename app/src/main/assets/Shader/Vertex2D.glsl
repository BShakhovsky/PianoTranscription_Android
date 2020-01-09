attribute vec2 pos, texturePos;

varying vec2 textureXY;

void main() {
    gl_Position = vec4(pos.x, pos.y, 0, 1);
    textureXY = texturePos;
}