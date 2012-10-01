uniform sampler2D jvr_Texture0;
uniform bool jvr_UseTexture0;
uniform bool jvr_Texture0_IsSemiTransparent;
uniform vec4 jvr_Material_Ambient;

varying vec2 texCoord;
varying vec3 _color;


void discardForMac() {
	discard;
}

void main (void)
{
   	
   	vec4 a = vec4(_color ,1);//vec4(jvr_Material_Ambient.xyz, 1);
   	if (jvr_UseTexture0)
   	{
        a *= jvr_Material_Ambient * texture2D(jvr_Texture0, texCoord);
        if(a.w<0.2) discardForMac();
    }
	vec4 final_color = a;
	gl_FragColor = final_color;
	
	if(!jvr_Texture0_IsSemiTransparent && jvr_Material_Ambient.a == 1.0)
	{
		gl_FragColor.w=1.0;
	}
}
