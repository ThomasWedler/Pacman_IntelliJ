package siris.components.renderer.jvr

import java.io.File
import de.bht.jvr.core._
import siris.ontology.Symbols
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.entity.description.{SVal, Aspect, Semantics}


abstract class JVRAspect( aspectType : SVal[Semantics], targets : List[Symbol] = Nil )
  extends Aspect( Symbols.graphics, aspectType, targets ) {
  def getProvidings =
    getFeatures
}


/**
 * This case class represents a SkyBox
 *
 * @param name Name of the skybox
 * @param backTexture Texture for the back side of the sky box.
 * @param bottomTexture Texture for the down side of the sky box.
 * @param frontTexture Texture for the front side of the sky box.
 * @param leftTexture Texture for the left side of the sky box.
 * @param rightTexture Texture for the right side of the sky box.
 * @param topTexture Texture for the up side of the sky box.
 */
case class SkyBoxCreater( name: String, backTexture: String, bottomTexture: String, frontTexture: String, leftTexture: String, rightTexture: String, topTexture: String ) {

  def create( shaderContext : String ) : SceneNode = {
    val plane = ResourceManager.getSceneNodeFromColladaFile( new File( "models/plane.dae" ) )
		val planeGeo = Finder.findGeometry( plane, null );
		val bk = ResourceManager.loadTexture(new File(backTexture));
		val dn = ResourceManager.loadTexture(new File(bottomTexture));
		val ft = ResourceManager.loadTexture(new File(frontTexture));
		val lf = ResourceManager.loadTexture(new File(leftTexture));
		val rt = ResourceManager.loadTexture(new File(rightTexture));
		val up = ResourceManager.loadTexture(new File(topTexture));

		val textureProg = ResourceManager.loadShaderProgram( "shader/default_ambient.vs" :: "shader/sky.fs" :: Nil );

    val skyBox = new GroupNode("SkyBox");

    // back
		val backMat = new ShaderMaterial(shaderContext, textureProg);
		backMat.setTexture(shaderContext, "jvr_Texture0", bk);
		val backShape = new ShapeNode("", planeGeo, backMat);
		backShape.setTransform(Transform.translate(0, 0, 0.5f).mul(Transform.rotateYDeg(180)));
		skyBox.addChildNodes(backShape);

		// down
		val downMat = new ShaderMaterial(shaderContext, textureProg);
		downMat.setTexture(shaderContext, "jvr_Texture0", dn);
		val downShape = new ShapeNode("", planeGeo, downMat);
		downShape.setTransform(Transform.translate(0, -0.5f, 0).mul(Transform.rotateYDeg(180)).mul(Transform.rotateXDeg(-90)));
		skyBox.addChildNodes(downShape);

		// front
		val frontMat = new ShaderMaterial(shaderContext, textureProg);
		frontMat.setTexture(shaderContext, "jvr_Texture0", ft);
		val frontShape = new ShapeNode("", planeGeo, frontMat);
		frontShape.setTransform(Transform.translate(0, 0, -0.5f));
		skyBox.addChildNodes(frontShape);

		// left
		val leftMat = new ShaderMaterial(shaderContext, textureProg);
		leftMat.setTexture(shaderContext, "jvr_Texture0", lf);
		val leftShape = new ShapeNode("", planeGeo, leftMat);
		leftShape.setTransform(Transform.translate(0.5f, 0, 0).mul(Transform.rotateYDeg(-90)));
		skyBox.addChildNodes(leftShape);

		// right
		val rightMat = new ShaderMaterial(shaderContext, textureProg);
		rightMat.setTexture(shaderContext, "jvr_Texture0", rt);
		val rightShape = new ShapeNode("", planeGeo, rightMat);
		rightShape.setTransform(Transform.translate(-0.5f, 0, 0).mul(Transform.rotateYDeg(90)));
		skyBox.addChildNodes(rightShape);

		// up
		val upMat = new ShaderMaterial(shaderContext, textureProg);
		upMat.setTexture(shaderContext, "jvr_Texture0", up);
		val upShape = new ShapeNode("", planeGeo, upMat);
		upShape.setTransform(Transform.translate(0, 0.5f, 0).mul(Transform.rotateYDeg(180)).mul(Transform.rotateXDeg(90)));
		skyBox.addChildNodes(upShape);

		val resultSky = new GroupNode();
		resultSky.addChildNode(skyBox.setTransform(Transform.rotateYDeg(180)));
    resultSky.setName( name )
		resultSky;
  }
}

case class PPE( ppe : PostProcessingEffect ) extends JVRAspect( Symbols.postProcessingEffect ) {

  require( ppe != null, "The parameter 'ppe' must not be null!" )

  val list = ppe.getSVarDescriptions

  override def getCreateParams = addCVars {
      Seq( siris.ontology.types.PostProcessingEffect( ppe ) )
  }

  override def getFeatures : Set[ConvertibleTrait[_]] = {
    var features = Set[ConvertibleTrait[_]]()
    for( sVarDescription <- list ) {
      features = features + sVarDescription
    }
    features
  }
}