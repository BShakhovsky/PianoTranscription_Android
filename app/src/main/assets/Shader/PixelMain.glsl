precision mediump float;

uniform vec4 color;
uniform vec3 light0, light1, light2;
uniform sampler2D depthBuff0, depthBuff1, depthBuff2;
uniform float pixel;

varying vec3 viewDir, normal;
varying vec4 shadowPos0, shadowPos1, shadowPos2;

// From http://blog.shayanjaved.com/2011/03/13/shaders-android/
float Shadow(const vec3 dir, const vec4 shadowPos, const sampler2D depths) {
    float shadow = 0.;
    vec4 depthCord = (shadowPos / shadowPos.w + 1.) / 2.;
    for (float y = -1.5; y <= 1.5; y += 1.) for (float x = -1.5; x <= 1.5; x += 1.)
        if (dot(texture2D(depths, (depthCord + vec4(x * pixel, y * pixel, .05, 0.)).st),
            vec4(1. / (256. * 256. * 256.), 1. / (256. * 256.), 1. / 256., 1)) > depthCord.z -
            // From http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-16-shadow-mapping
            clamp(.0001 * tan(acos(clamp(dot(normal, -dir), 0., 1.))), 0., .01)) shadow += 1.;
    return clamp(shadow / 16., .0, 1.);
}

void main() {
    vec3 normLight0 = normalize(light0), normLight1 = normalize(light1), normLight2 = normalize(light2);
    gl_FragColor = color * vec4(vec3(.05333332, .09882354, .1819608) // ambient
        // Diffuse:
        + vec3(1         ,  .9607844 ,  .8078432) * max(-dot(normal, normLight0), 0.) * Shadow(normLight0, shadowPos0, depthBuff0)
        + vec3( .9647059 ,  .7607844 ,  .4078432) * max(-dot(normal, normLight1), 0.) * Shadow(normLight1, shadowPos1, depthBuff1)
        + vec3( .3231373 ,  .3607844 ,  .3937255) * max(-dot(normal, normLight2), 0.) * Shadow(normLight2, shadowPos2, depthBuff2)
        // Specular:
        + vec3(1         ,  .9607844 ,  .8078432) * pow(max(dot(-viewDir, reflect(normLight0, normal)), 0.), 16.)
        + vec3(0         , 0         , 0        ) * pow(max(dot(-viewDir, reflect(normLight1, normal)), 0.), 16.)
        + vec3( .3231373 ,  .3607844 ,  .3937255) * pow(max(dot(-viewDir, reflect(normLight2, normal)), 0.), 16.), 1);
}