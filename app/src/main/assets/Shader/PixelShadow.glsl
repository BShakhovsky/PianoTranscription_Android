precision mediump float;

varying vec4 orthoPos;

void main() { // from Fabien Sangalard's DEngine
    vec4 comp = fract(((orthoPos.z / orthoPos.w + 1.) / 2.) * vec4(256 * 256 * 256, 256 * 256, 256, 1));
    gl_FragColor = comp - comp.xxyz * vec4(0, 1. / 256., 1. / 256., 1. / 256.);
}