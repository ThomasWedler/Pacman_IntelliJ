package siris.components.renderer.jvr

import de.bht.jvr.core.{VRCameraNode, SceneNode}

/**
 * The abstract class CameraUpdater is the base class for every class which keeps two cameras in sync.
 *
 * @author Stephan Rehfeld
 */
abstract class CameraUpdater {

  /**
   * The update method is called by the render connector to sync the cameras.
   *
   */
  def update()
}


/**
 * This CameraUpdater connects to cameras to render a mirror plane. The transformation
 * of the reference camera is mirrored at the mirror plane and written to the mirror camera.
 * Over and above the head position and eye separation is kept in sync.
 *
 * @author Stephan Rehfeld
 *
 * @param mirrorCamera The camera that is used to render the image of the surface. Values get written in this object. Must not be 'null'!
 * @param referenceCamera The original camera that is used to render the scene. Value are read from this object. Must not be 'null'!
 * @param mirrorPlane The mirror plane. The transformation of the mirror camera is calculated in relation to the mirror plane. Must not be 'null'!
 */
class MirrorCameraUpdater( mirrorCamera : VRCameraNode, referenceCamera : VRCameraNode, mirrorPlane : SceneNode ) extends CameraUpdater {

  require( mirrorCamera != null, "The parameter 'mirrorCamera' must not be 'null'!" )
  require( referenceCamera != null, "The parameter 'referenceCamera' must not be 'null'!" )

  override def update() {


    //println(Transform.scale(1,1,-1).mul( mirrorPlane.getTransform.invert.mul( referenceCamera.getTransform ) ).toString)
    //mirrorCamera.setHeadTransform( mirrorPlane.getTransform.mul( Transform.scale(1,1,-1).mul( mirrorPlane.getTransform.invert.mul( referenceCamera.getTransform ) ) ) )
    mirrorCamera.setHeadTransform( referenceCamera.getHeadTransform )
    mirrorCamera.setEyeSeparation( referenceCamera.getEyeSeparation )
  }

  override def toString = "This MirrorCameraUpdater keeps the camera " + " in sync with mirror camera " + mirrorCamera + " relative to the plane " + mirrorPlane + "."


}