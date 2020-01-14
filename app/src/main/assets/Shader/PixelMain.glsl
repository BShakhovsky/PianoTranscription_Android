precision mediump float;

uniform vec4 color;
uniform bool shadow, specular;
uniform sampler2D deskTexture, depthBuff0, depthBuff1, depthBuff2;
uniform vec2 pixel0, pixel1, pixel2;

varying float withTex;
varying vec2 deskUV;
varying vec3 viewDir, normal, lightView0, lightView1, lightView2;
varying vec4 shadowPos0, shadowPos1, shadowPos2;

// From http://blog.shayanjaved.com/2011/03/13/shaders-android/
float Shadow(const vec3 light, const vec4 shadowPos, const sampler2D depths, const vec2 pixel, const float minDarkness) {
    if (!shadow) return 1.;

    float shadow = 0.;
    vec4 depthCord = (shadowPos / shadowPos.w + 1.) / 2.;
    for (float y = -1.5; y <= 1.5; y += 1.) for (float x = -1.5; x <= 1.5; x += 1.)
        if (dot(texture2D(depths, (depthCord + vec4(x * pixel[0], y * pixel[1], .05, 0.)).st),
            vec4(1. / (256. * 256. * 256.), 1. / (256. * 256.), 1. / 256., 1)) > depthCord.z -
            // From http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-16-shadow-mapping
            clamp(.005 * tan(acos(clamp(dot(normal, -light), 0., 1.))), 0., .01)) shadow += 1.;
    return clamp(shadow / 16., minDarkness, 1.);
}

void main() {
    const vec3         light0 = vec3(1        , .9607844 , .8078432),
                       light1 = vec3(.9647059 , .7607844 , .4078432),
                       light2 = vec3(.3231373 , .3607844 , .3937255);
    gl_FragColor = color * vec4(vec3(.05333332, .09882354, .1819608) // ambient, then diffuse:
                     + light0 * max(-dot(normal, lightView0), 0.)  * Shadow(lightView0, shadowPos0, depthBuff0, pixel0, 0.)
                     + light1 * max(-dot(normal, lightView1), 0.)//* Shadow(lightView1, shadowPos1, depthBuff1, pixel1, 0.)
                     + light2 * max(-dot(normal, lightView2), 0.)  * Shadow(lightView2, shadowPos2, depthBuff2, pixel2, .0)
        + (specular ? (light0 * pow(max(dot(-viewDir, reflect(lightView0, normal)), 0.), 2.)
                     + light1 * pow(max(dot(-viewDir, reflect(lightView1, normal)), 0.), 4.)
                     + light2 * pow(max(dot(-viewDir, reflect(lightView2, normal)), 0.), 32.)) : vec3(0)), 1);

    if (withTex > .5 && max(texture2D(deskTexture, deskUV).r,
                        max(texture2D(deskTexture, deskUV).g,
                            texture2D(deskTexture, deskUV).b)) < .98) {
        gl_FragColor.r *= 210. / 255. * 2.5; // Chocolate
        gl_FragColor.g *= 105. / 255. * 2.5;
        gl_FragColor.b *=  30. / 255. * 2.5;
    }
}