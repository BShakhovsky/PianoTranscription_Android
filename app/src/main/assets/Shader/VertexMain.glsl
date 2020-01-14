attribute vec4 pos;
attribute vec3 norm;

attribute vec2 texturePos;
attribute float tex;

uniform vec3 light0, light1, light2;
uniform mat4 mv, mvp, inTrV, lightMVO0, lightMVO1, lightMVO2;

varying float withTex;
varying vec2 deskUV;

varying vec3 viewDir, normal, lightView0, lightView1, lightView2;
varying vec4 shadowPos0, shadowPos1, shadowPos2;

void main() {
    gl_Position = mvp * pos;

    withTex = tex;
    deskUV = texturePos * vec2(1, -1);

    viewDir    = normalize(vec3(mv * pos));
    normal     = normalize(vec3(inTrV * vec4(norm, 0)));

    lightView0 = normalize(vec3(inTrV * vec4(light0, 0)));
    lightView1 = normalize(vec3(inTrV * vec4(light1, 0)));
    lightView2 = normalize(vec3(inTrV * vec4(light2, 0)));

    shadowPos0 = lightMVO0 * pos;
    shadowPos1 = lightMVO1 * pos;
    shadowPos2 = lightMVO2 * pos;
}