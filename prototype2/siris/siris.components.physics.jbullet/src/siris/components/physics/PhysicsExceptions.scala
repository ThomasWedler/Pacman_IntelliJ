package siris.components.physics

/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/5/11
 * Time: 3:42 PM
 */
import java.lang.Throwable

object Level extends Enumeration {
  val info = Value("info")
  val warn = Value("warn")
  val error = Value("error")
}

/**
 *  Holds information to JBulletComponent exceptions
 */
case class PhysicsException(errorMessage: String, level: Enumeration#Value = Level.error) extends java.lang.Throwable {
  override def toString: String =  {
    val callerClass = (new Throwable).getStackTrace.apply(1).getClass.getSimpleName
    "[" + level.toString + "]" + "[" + callerClass + "]" + errorMessage
  }
}