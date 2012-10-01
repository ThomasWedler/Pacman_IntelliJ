uniform sampler2D jvr_Texture0;
uniform int jvr_UseTexture0;
uniform vec4 jvr_Material_Ambient;
uniform sampler2D MyHeightMap;

varying vec3  eyeVec;
varying vec2  texCoord;

void main (void)
{
   	vec4 a = jvr_Material_Ambient;
   	vec3 E = normalize(eyeVec);
   	
   	// Parallax
   	float height = texture2D(MyHeightMap, texCoord).r;
    
    height = height * 0.09 - 0.04;
    //vec2 texCoord = texCoord + (height * E.xy); // calculate new texture coordinates
   	
   	//if (jvr_UseTexture0 != 0)
   	{
        a = texture2D(MyHeightMap, texCoord);
        if(a.w<0.2) discard;
    }
	gl_FragColor = a;
	// gl_FragColor.w=1.0; //<--- transparency is disabled
}
