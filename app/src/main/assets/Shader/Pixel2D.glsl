precision mediump float;

uniform sampler2D frameBuff;

varying vec2 textureUV;

void main() {
    gl_FragColor = texture2D(frameBuff, textureUV);
}