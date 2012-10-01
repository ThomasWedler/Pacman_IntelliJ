package siris.core.helper

import simplex3d.math.floatm.Vec3f

/*
* Created by IntelliJ IDEA.
* User: martin
* Date: 8/19/11
* Time: 10:45 AM
*/
case class BoundingBox(min: Vec3f, max: Vec3f) {
  val size = max - min

  override def toString =
    "BoundingBox [min=" + min.toString + ", max=" +max.toString + "]"
}