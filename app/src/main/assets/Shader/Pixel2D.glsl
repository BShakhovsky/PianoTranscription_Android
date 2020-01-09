precision mediump float;

uniform sampler2D frameBuff;

varying vec2 textureXY;

void main() { gl_FragColor = texture2D(frameBuff, textureXY); }