attribute vec4 pos;
attribute vec3 norm;
uniform mat4 mv, mvp, lightMVO0, lightMVO1, lightMVO2;

varying vec3 viewDir, normal;
varying vec4 shadowPos0, shadowPos1, shadowPos2;

void main() {
    gl_Position = mvp * pos;

    viewDir = normalize(vec3(mv * pos));
    normal = norm;
    shadowPos0 = lightMVO0 * pos;
    shadowPos1 = lightMVO1 * pos;
    shadowPos2 = lightMVO2 * pos;
}