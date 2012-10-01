uniform sampler2D jvr_Texture0;
uniform bool jvr_UseTexture0;
uniform bool jvr_Texture0_IsSemiTransparent;
uniform vec4 jvr_Material_Ambient;

uniform bool  jvr_UseClipPlane0;
varying float clipDist0;

varying vec2 texCoord;

void main (void)
{
   	if(jvr_UseClipPlane0)
	{
		if(clipDist0<0.0) discard;
	}
   	
   	vec4 a = jvr_Material_Ambient;
   	if (jvr_UseTexture0)
   	{
        a *= texture2D(jvr_Texture0, texCoord);
        if(a.w<0.2) discard;
    }
	vec4 final_color = a;
	gl_FragColor = final_color;
	
	if(!jvr_Texture0_IsSemiTransparent && jvr_Material_Ambient.a == 1.0)
	{
		gl_FragColor.w=1.0;
	}
}
