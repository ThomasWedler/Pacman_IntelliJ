uniform sampler2D jvr_Texture0;
uniform vec4 jvr_Material_Ambient;

uniform bool  jvr_UseClipPlane0;
varying float clipDist0;

varying vec2 texCoord;
varying vec3 eyeVecTSpace;

void main (void)
{
   	if(jvr_UseClipPlane0)
	{
		if(clipDist0<0.0) discard;
	}
	
	vec3 ETSpace = normalize(eyeVecTSpace);
    vec2 texCoord = texCoord + ETSpace.xy; // calculate new texture coordinates

	gl_FragColor = jvr_Material_Ambient * texture2D(jvr_Texture0, texCoord);
}
