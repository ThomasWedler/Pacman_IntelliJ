package siris.components.renderer.jvr.examples

import simplex3d.math.floatm.{ConstMat4f, Mat3x4f, Vec3f}
import siris.components.renderer.setup._

object BasicDisplayConfiguration{
  /**
   *
   * Creates a display setup description that should fit for every desktop
   * computer or laptop.
   */
  def apply(widthInPx: Int, heightInPx: Int, fullscreen : Boolean = false, dpi : Double = 48.0) : DisplaySetupDesc = {
    val widthOfScreenInMeters = widthInPx / dpi * 0.0254
    val displayDesc = new DisplayDesc(
      if (fullscreen) None else Some( widthInPx -> heightInPx ),
      widthOfScreenInMeters -> widthOfScreenInMeters * heightInPx / widthInPx,
      ConstMat4f( Mat3x4f.translate( Vec3f( 0.0f, 0.0f, -0.6f ) ) ),
      new CamDesc( 0, Eye.RightEye )
    )
    new DisplaySetupDesc().addDevice( new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay ), 0 )
  }
}