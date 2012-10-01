attribute vec4 jvr_Vertex;
attribute vec2 jvr_TexCoord;
attribute vec3 jvr_Normal;
attribute vec3 jvr_Binormal;
attribute vec3 jvr_Tangent;

uniform mat4 jvr_ProjectionMatrix;
uniform mat4 jvr_ModelViewMatrix;
uniform mat3 jvr_NormalMatrix;

varying vec3  eyeVec;
varying vec2  texCoord;

void main(void)
{
	gl_Position = jvr_ProjectionMatrix * jvr_ModelViewMatrix * jvr_Vertex;
	texCoord = jvr_TexCoord;
	
	vec3 vVertex = vec3(jvr_ModelViewMatrix * jvr_Vertex);
	eyeVec = -vVertex;
	
	vec3 n = normalize(jvr_NormalMatrix * jvr_Normal);
	vec3 t = normalize(jvr_NormalMatrix * jvr_Tangent);
	vec3 b = normalize(jvr_NormalMatrix * jvr_Binormal);
	
	mat3 tbn = mat3(t.x, b.x, n.x,
					t.y, b.y, n.y,
					t.z, b.z, n.z);
	
	eyeVec = tbn * eyeVec;
}