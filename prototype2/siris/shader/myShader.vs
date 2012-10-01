attribute vec4 jvr_Vertex;
attribute vec2 jvr_TexCoord;
attribute vec3 jvr_Vertex_Color;

uniform mat4 jvr_ModelViewProjectionMatrix;
uniform mat4 jvr_ProjectionMatrix;
uniform mat4 jvr_ModelViewMatrix;
uniform float jvr_PolygonOffset;

varying vec2 texCoord;
varying vec3 _color;

void main(void)
{
	texCoord = jvr_TexCoord;
	if(jvr_PolygonOffset == 0.0)
	{
		gl_Position = jvr_ModelViewProjectionMatrix * jvr_Vertex;
	}
	else
	{
		gl_Position = jvr_ModelViewMatrix * jvr_Vertex;
		vec3 eyeVec = normalize(gl_Position.xyz);
		gl_Position.xyz += eyeVec*jvr_PolygonOffset;// the shadow map bias
		gl_Position = jvr_ProjectionMatrix * gl_Position;
	}
	
    _color = vec3(1,1,1);//jvr_Vertex_Color;
	// user defined clipping
}