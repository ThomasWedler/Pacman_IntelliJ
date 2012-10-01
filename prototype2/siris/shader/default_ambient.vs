attribute vec4 jvr_Vertex;
attribute vec2 jvr_TexCoord;

uniform mat4 jvr_ModelViewProjectionMatrix;
uniform mat4 jvr_ProjectionMatrix;
uniform mat4 jvr_ModelViewMatrix;
uniform float jvr_PolygonOffset;

uniform bool jvr_UseClipPlane0;
uniform vec4 jvr_ClipPlane0;
varying float clipDist0;

varying vec2 texCoord;

float calcClipDist(vec4 point, vec4 plane)
{
	vec3 p = point.xyz / point.w;
	float len = length(plane.xyz);
	return (plane.x * p.x + plane.y * p.y + plane.z * p.z - plane.w) / len;
}

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
	
	if(jvr_UseClipPlane0)
	{
		clipDist0 = calcClipDist(jvr_ModelViewMatrix * jvr_Vertex, jvr_ClipPlane0);
	}
}