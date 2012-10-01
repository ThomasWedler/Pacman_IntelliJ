attribute vec4 jvr_Vertex;
attribute vec2 jvr_TexCoord;
attribute vec3 jvr_Normal;
attribute vec3 jvr_Binormal;
attribute vec3 jvr_Tangent;

uniform mat4 jvr_ModelViewProjectionMatrix;
uniform mat4 jvr_ProjectionMatrix;
uniform mat4 jvr_ModelViewMatrix;
uniform mat3 jvr_NormalMatrix;
uniform float jvr_PolygonOffset;

uniform bool jvr_UseClipPlane0;
uniform vec4 jvr_ClipPlane0;
varying float clipDist0;

varying vec2 texCoord;
varying vec3 eyeVecTSpace;

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
	
	// transform to tangent space ////////////////////////////////////////////////////////	
	vec3 n = normalize(jvr_NormalMatrix * jvr_Normal);
	vec3 t = normalize(jvr_NormalMatrix * jvr_Tangent);
	vec3 b = normalize(jvr_NormalMatrix * jvr_Binormal);
	
	mat3 tbn = mat3(t.x, b.x, n.x,
					t.y, b.y, n.y,
					t.z, b.z, n.z);
					
	vec4 vVertex = jvr_ModelViewMatrix * jvr_Vertex;	
	eyeVecTSpace = tbn * -vVertex.xyz;
}