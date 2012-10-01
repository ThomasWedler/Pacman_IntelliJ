package siris.components.network

case class Exception(errorMessage: String) extends java.lang.Throwable {
  override def toString: String = errorMessage
}