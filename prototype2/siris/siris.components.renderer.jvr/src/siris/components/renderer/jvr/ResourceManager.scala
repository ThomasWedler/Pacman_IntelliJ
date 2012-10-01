package siris.components.renderer.jvr

import de.bht.jvr.collada14.loader.ColladaLoader
import java.io.File
import de.bht.jvr.core.{ShaderProgram, Texture2D, SceneNode}
import siris.core.helper.Loggable

/**
 * This is a resource manager for OntologyJVR textures, scenes, nodes and shader programs.
 *
 * @author Stephan Rehfeld
 */
object ResourceManager extends Loggable {

  /**
   * Cached scene graph nodes.
   */
  var sceneNodes: Map[File,SceneNode] = Map()

  /**
   * This function returns a scene graph from internal cache or loads it from a given file.
   *
   * @param file The file where to load the scene graph from.
   * @return The scene graph.
   */
  def getSceneNodeFromColladaFile( file: File ) : SceneNode = {
    require( file != null, "The parameter 'file' must not be 'null'" )
    require( file.isFile, "The parameter 'file' must point to a file." )
    require( file.exists, "The parameter 'file' must point to a existing file." )

    if( !sceneNodes.contains( file ) ) {
      sceneNodes = sceneNodes + (file -> ColladaLoader.load( file ) )
      info("Loading scene element from file: {}", file.getName )
    } else {
      info("Already loaded scene element from file: {}", file.getName )
    }
    val node = sceneNodes( file )
    node

//    val g = new GroupNode
//    g.addChildNode( node )
//    g
  }

  def getSceneNodeFromColladaFile( file: String ) : SceneNode = {
    require( file != null, "The parameter 'file' must not be null." )
    require( file.equals( "" ), "The parameter 'file' must not be an empty string." )
    getSceneNodeFromColladaFile( new File( file ) )
  }

  /**
   * Cached textures.
   */
  var textures : Map[File,Texture2D] = Map()

  /**
   * This function returns a texture from the internal chache or loads it from a given file.
   *
   * @param file The file where to load the texture from.
   * @return The texture.
   */
  def loadTexture( file: File ) : Texture2D = {
    require( file != null, "The parameter 'file' must not be 'null'" )
    require( file.isFile, "The parameter 'file' must point to a file." )
    require( file.exists, "The parameter 'file' must point to a existing file." )
    if( !textures.contains( file ) ) {
      textures = textures + (file -> new Texture2D( file ))
      info( "Loading texture: {}", file.getName  )
    } else {
      info( "Already loaded texture: {}", file.getName )
    }
    textures( file )
  }

   def loadTexture( file: String ) : Texture2D = {
     require( file != null, "The parameter 'file' must not be null." )
     require( file.equals( "" ), "The parameter 'file' must not be an empty string." )
     loadTexture( new File( file ) )
   }

  /**
   * Cached shader programs.
   */
  var shaderPrograms: Map[Set[File],ShaderProgram] = Map()

  /**
   * This function returns a shader program from the internal chache or loads it from a given file.
   *
   * @param shader The vertex and fragement shader of the shader program.
   * @return The shader program.
   */
  def loadShaderProgram( shader : Set[File] ) : ShaderProgram = {
    require( shader != null, "The parameter 'shader' must not be 'null'" )
    for( file <- shader ) {
      require( file.isFile, "The parameter 'file' must point to a file." )
      require( file.exists, "The parameter 'file' must point to a existing file." )
    }
    if( !shaderPrograms.contains( shader ) ) {
      shaderPrograms = shaderPrograms + (shader -> new ShaderProgram( shader.toArray : _* ))
      //info( "Loading shader program: {} and {}", shader._1.getName, shader._2.getName  )
    }  else {
      //info( "Already loaded shader program: {} and {}", shader._1.getName, shader._2.getName )
    }
    shaderPrograms( shader )
  }

  def loadShaderProgram( shader : List[String] ) : ShaderProgram = {
    require( shader != null, "The parameter 'shader' must not be null!" )

    val files = for( s <- shader ) yield {
      require( s != null, "The parameter 'shader' must not be null." )
      require( !s.equals( "" ), "The parameter 'file' must not be an empty string." )
      new File( s )
    }

    loadShaderProgram( Set( files.toArray : _* ) )
  }
}
