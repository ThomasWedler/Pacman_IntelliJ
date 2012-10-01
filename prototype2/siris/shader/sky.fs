uniform sampler2D jvr_Texture0;

varying vec2 texCoord;

// inspired by marc roßberg

void main (void)
{
	vec2 texC = texCoord;
	
	// remove border lines
	if(texC.x>0.997)texC.x = 0.997;
	if(texC.y>0.997)texC.y = 0.997;
	if(texC.x<0.003)texC.x = 0.003;
	if(texC.y<0.003)texC.y = 0.003;
	
	gl_FragColor = texture2D(jvr_Texture0, texC);
}
