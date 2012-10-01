package siris.components.renderer.createparameter

import de.bht.jvr.core._
import siris.core.helper.Loggable
import uniforms.{UniformBool, UniformColor, UniformFloat}
import java.io.File
import siris.components.renderer.jvr.ResourceManager
import java.awt.Color


abstract class ElementManipulator extends Loggable {
  def execute( node: SceneNode )
}

case class ParallaxMaterial( val elementId : String, val textureP: String, val heightMapP : String, val normalMapP : String, val heightScale: Float, val heightBias: Float, val shininess : Float ) extends ElementManipulator {

  //require( elementId != null, "The parameter 'elementId' must not be 'null'!" )
  //require( file != null, "The parameter 'file' must not be 'null'!" )

  override def execute( node: SceneNode ) = {
    info( "Trying to apply parallax mapping" )
    val elements = Finder.findAll( node, classOf[GroupNode], elementId ).toArray.toList.asInstanceOf[List[GroupNode]]

    for( g <- elements  ) {
			val childs = g.getChildNodes();
			if( childs.size() > 0 && childs.get(0).isInstanceOf[SceneNode]) {
				val s = childs.get(0).asInstanceOf[ShapeNode]
        try {
				s.setMaterial(makeParallaxMaterial( 0.01f, -0.00f, 40));
        } catch {
          case e: Exception => error( "Error creating parralax map {}", e )
        }
			}
		}
    info( "Applied parallax mapping" )
  }

  /**
   * This private function creates a ShaderMaterial for paralllax mapping.
   *
   * @param path The beginning of the file names of the color, height, and normal-map.
   * @param heightScala
   * @param heightBias
   * @param shininess
   */
  private def makeParallaxMaterial(  heightScale: Float, heightBias: Float, shininess : Float) : ShaderMaterial = {
    info( "Making parallax material" )
		var mat : ShaderMaterial = null;
		try {
			val ambientParallaxProg = ResourceManager.loadShaderProgram( Set(new File( "shader/bumpmapping_ambient.vs" ), new File( "shader/parallaxmapping_ambient.fs" ) ));
      val lightingParallaxProg = ResourceManager.loadShaderProgram( Set(new File( "shader/bumpmapping_lighting.vs" ), new File( "shader/parallaxmapping_lighting.fs" ) ));

			val colorMap = ResourceManager.loadTexture( new File( textureP ) );
			val heightMap = ResourceManager.loadTexture( new File( heightMapP ) );
			val normalMap = ResourceManager.loadTexture( new File( normalMapP ) );

			// create the shader material
			mat = new ShaderMaterial();

			mat.setShaderProgram("AMBIENT", ambientParallaxProg); // set the
																	// ambient
																	// shader
																	// program
			mat.setTexture("AMBIENT", "jvr_Texture0", colorMap);
			mat.setTexture("AMBIENT", "jvr_HeightMap", heightMap);
			mat.setUniform("AMBIENT", "jvr_HeightScale", new UniformFloat(heightScale));
			mat.setUniform("AMBIENT", "jvr_ParallaxBias", new UniformFloat(heightBias));
			mat.setUniform("AMBIENT", "jvr_Material_Ambient", new UniformColor(new de.bht.jvr.util.Color(0.1f, 0.1f, 0.1f, 1f)));

			mat.setShaderProgram("LIGHTING", lightingParallaxProg); // set the
																	// lighting
																	// shader
																	// program
			mat.setTexture("LIGHTING", "jvr_NormalMap", normalMap);
			mat.setTexture("LIGHTING", "jvr_Texture0", colorMap);
			mat.setTexture("LIGHTING", "jvr_HeightMap", heightMap);
			mat.setUniform("LIGHTING", "jvr_HeightScale", new UniformFloat(heightScale));
			mat.setUniform("LIGHTING", "jvr_ParallaxBias", new UniformFloat(heightBias));
			mat.setUniform("LIGHTING", "jvr_Material_Diffuse", new UniformColor(new de.bht.jvr.util.Color(1.0f, 1.0f, 1.0f, 1.0f)));
			mat.setUniform("LIGHTING", "jvr_Material_Specular", new UniformColor(new de.bht.jvr.util.Color(0.6f, 0.6f, 0.6f, 1.0f)));
			mat.setUniform("LIGHTING", "jvr_Material_Shininess", new UniformFloat(shininess));
      info( "Parallax material created" )
		} catch  {
			case e : Exception => warn( "Error creating parallax material!", e )
		}

		mat
  }
}


/**
 * This case class adds normal mapping material to a named node in the applied element.
 *
 * To use this manipulator you need a color, and normal map. They must be in the
 * same directory with the same name with different endings (_color.jpg, _NORMAL.jpg).
 *
 * @param elementId The name of the element where normal mapping should applied to.
 * @param file The beginning of the file names of the color and normal-map.
 */
case class NormalMaterial( val elementId : String, val textureP: String, val normalMapP : String, val shininess : Float  ) extends ElementManipulator {
  override def execute( node: SceneNode ) = {
    warn( "Trying to apply normal mapping" )
    val elements = Finder.findAll( node, classOf[GroupNode], elementId ).toArray.toList.asInstanceOf[List[GroupNode]]

    for( g <- elements  ) {
			val childs = g.getChildNodes();
			if( childs.size() > 0 && childs.get(0).isInstanceOf[SceneNode]) {
				val s = childs.get(0).asInstanceOf[ShapeNode]
				s.setMaterial(makeNormalMaterial(shininess));
			}
		}
    warn( "Applied parallax mapping" )
  }

   /**
   * This private function creates a ShaderMaterial for normal mapping.
   *
   * @param path The beginning of the file names of the color and normal-map.
   * @param shininess
   */
  def makeNormalMaterial( shininess : Float ) : ShaderMaterial = {
    warn( "Making normal material" )
		var mat: ShaderMaterial = null;
    try {
			val ambientBumpProg = ResourceManager.loadShaderProgram( Set(new File( "shader/default_ambient.vs" ), new File( "shader/default_ambient.fs" )) )
			val lightingBumpProg = ResourceManager.loadShaderProgram( Set(new File( "shader/bumpmapping_lighting.vs" ), new File( "shader/bumpmapping_lighting.fs" )) )

			val colorMap = ResourceManager.loadTexture( new File( textureP ) );
			val normalMap = ResourceManager.loadTexture( new File( normalMapP ) );

			// create the shader material
			mat = new ShaderMaterial();

			mat.setShaderProgram("AMBIENT", ambientBumpProg); // set the ambient
																// shader
																// program
			mat.setTexture("AMBIENT", "jvr_Texture0", colorMap);
			mat.setUniform("AMBIENT", "jvr_UseTexture0", new UniformBool(true));
			mat.setUniform("AMBIENT", "jvr_Material_Ambient", new UniformColor(new de.bht.jvr.util.Color(0.1f, 0.1f, 0.1f, 1f)));

			mat.setShaderProgram("LIGHTING", lightingBumpProg); // set the
																// lighting
																// shader
																// program
			mat.setTexture("LIGHTING", "jvr_NormalMap", normalMap);
			mat.setTexture("LIGHTING", "jvr_Texture0", colorMap);
			mat.setUniform("LIGHTING", "jvr_Material_Diffuse", new UniformColor(new de.bht.jvr.util.Color(1.0f, 1.0f, 1.0f, 1.0f)));
			mat.setUniform("LIGHTING", "jvr_Material_Specular", new UniformColor(new de.bht.jvr.util.Color(0.6f, 0.60f, 0.6f, 1.0f)));
			mat.setUniform("LIGHTING", "jvr_Material_Shininess", new UniformFloat(shininess));
      debug( "Normal material created" )
		} catch {
			case e : Exception => warn( "Error creating normal material!", e )
		}

	  mat
	}
}

case class GeneralShaderMaterial( vertexShaderFile: String, fragmentShaderFile : String, context: String  ) extends ElementManipulator {

  override def execute( node: SceneNode ) = {
    warn( "Trying to apply normal mapping" )
    val s = Finder.find( node, classOf[ShapeNode], null ).asInstanceOf[ShapeNode]
    s.setMaterial( makeMaterial( vertexShaderFile, fragmentShaderFile, context ) )
    warn( "Applied parallax mapping" )
  }

  def makeMaterial( vertexShaderFile: String, fragmentShaderFile: String, context: String ) : ShaderMaterial = {
    warn( "Making normal material" )
		var mat: ShaderMaterial = null;
    try {

      val shaderProg = ResourceManager.loadShaderProgram( Set(new File( vertexShaderFile ), new File( fragmentShaderFile ) ));
      mat = new ShaderMaterial
      mat.setShaderProgram( context, shaderProg )

		} catch {
			case e : Exception => warn( "Error creating normal material!", e )
		}

	  mat
	}
}