uniform vec4  jvr_LightSource_Diffuse;
uniform vec4  jvr_LightSource_Specular;
uniform float jvr_LightSource_Intensity;
uniform vec4  jvr_LightSource_Position;
uniform vec3  jvr_LightSource_SpotDirection;
uniform float jvr_LightSource_SpotExponent;
uniform float jvr_LightSource_SpotCutOff;
uniform float jvr_LightSource_SpotCosCutOff;
uniform bool  jvr_LightSource_CastShadow;

uniform sampler2D jvr_Texture0;
uniform bool      jvr_UseTexture0;
uniform vec4      jvr_Material_Specular;
uniform vec4      jvr_Material_Diffuse;
uniform float     jvr_Material_Shininess;

varying vec3  normal;
varying vec3  eyeVec;
varying vec3  lightDir;
varying vec2  texCoord;
varying float attenuation;

uniform sampler2D jvr_ShadowMap;
varying vec4 shadowCoord;

uniform bool  jvr_UseClipPlane0;
varying float clipDist0;

void main (void)
{
    gl_FragColor = vec4( 0.0, 0.0, 1.0, 1.0 ) + vec4( 1.0, 1.0, 0.0, 0.0 ) * dot( normalize( eyeVec ), normalize( normal ) );
}