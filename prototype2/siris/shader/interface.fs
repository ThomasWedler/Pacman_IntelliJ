uniform sampler2D iface_live;
uniform int lives;
uniform float health;
uniform float mana;

varying vec2 texCoord;

// inspired by marc roßberg

void drawAt( vec2 center, float size, sampler2D image, vec2 texC ) {
	if( texC.x > center.x - size/2.0 && texC.x < center.x + size/2.0 && texC.y > center.y - size/2.0 && texC.y < center.y + size/2.0 ) {
		gl_FragColor = texture2D( image, vec2( (texC.x - center.x - size/2.0) * 1.0/size, 1.0 - ((texC.y - center.y - size/2.0) * 1.0/size) ) );	
	}
}

void main (void)
{
	vec2 texC = texCoord;

	if( lives > 0 ) {
		drawAt( vec2(0.9,0.965), 0.03, iface_live, texC );	
	}
	if( lives > 1 ) {
		drawAt( vec2(0.935,0.965), 0.03, iface_live, texC );	
	}
	if( lives > 2 ) {
		drawAt( vec2(0.97,0.965), 0.03, iface_live, texC );	
	}

	// left
	if( texC.x > 0.195 && texC.x < 0.2 && texC.y > 0.95 && texC.y < 0.98 ) {
		gl_FragColor = vec4( 0.0, 0.0, 0.0, 0.8 );
	}
        // top
	if( texC.x > 0.195 && texC.x < 0.805 && texC.y > 0.98 && texC.y < 0.985 ) {
		gl_FragColor = vec4( 0.0, 0.0, 0.0, 0.8 );
	}
        // health bar
	if( texC.x > 0.2 && texC.x < health*0.6+0.2 && texC.y > 0.965 && texC.y < 0.98 ) {
		gl_FragColor = vec4( 1.0, 0.0, 0.0, 0.5 );
	}

	// mana bar
	if( texC.x > 0.2 && texC.x < mana*0.6+0.2 && texC.y > 0.95 && texC.y < 0.965 ) {
		gl_FragColor = vec4( 0.0, 1.0, 0.0, 0.5 );
	}

        // bottom
	if( texC.x > 0.195 && texC.x < 0.805 && texC.y > 0.945 && texC.y < 0.95 ) {
		gl_FragColor = vec4( 0.0, 0.0, 0.0, 0.8 );
	} 
	// right
	if( texC.x > 0.8 && texC.x < 0.805 && texC.y > 0.95 && texC.y < 0.98 ) {
		gl_FragColor = vec4( 0.0, 0.0, 0.0, 0.8 );
	} 
}
