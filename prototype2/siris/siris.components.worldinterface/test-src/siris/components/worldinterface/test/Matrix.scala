package siris.components.worldinterface.test

import siris.core.entity.typeconversion.{Converter, ConvertibleTrait}

/* author: dwiebusch
 * date: 07.09.2010
 */


object Matrix{
  val float = Array[Float](1,2,3,4, 1,2,3,4, 1,2,3,4, 1,2,3,4)
  val taolf = Array[Float](1,1,1,1, 2,2,2,2, 3,3,3,3, 4,4,4,4)
}

class Matrix[T <: AnyVal]( v : Array[T] ){
  private[test] var values = v
  override def toString =
    (for ( i <- 0 to 3; j <- 0 to 3 ) yield( apply(i,j) + (if (j == 3) "\n" else " "))).mkString

  def apply(x : Int, y : Int) =
    values(x*4+y)
}

class RowMajorMatrix[T <: AnyVal]( values : Array[T]) extends Matrix[T](values)

class ColumnMajorMatrix[T <: AnyVal]( values : Array[T]) extends Matrix[T](values){
  override def apply(x: Int, y: Int) =
    values(y*4+x)
}

class MatrixConverter[MT1 <: Matrix[Float], MT2 <: Matrix[Float]]( c1 : ConvertibleTrait[MT1], c2 : ConvertibleTrait[MT2])
        extends Converter[MT1, MT2](c1, c2){
  def convert(i: MT1) = createInstanceWith(transpose(i), c2)
  def revert(i: MT2)  = createInstanceWith(transpose(i), c1)
  
  //helper functions / variables
  protected var convertType = false

  protected def createInstanceWith[U <: Matrix[Float]]( values : Array[Float], c : ConvertibleTrait[U] ) : U = {
    val retVal = c.defaultValue()
    retVal.values = values
    retVal
  }

  protected def transpose[I <: Matrix[Float]]( input : I) : Array[Float] = {
    val retVal = input.values.clone()
    if (convertType) for (i <- 0 to 3; j <- 0 to 3)
      retVal(i*4+j) = input.values(i+j*4)
    retVal
  }
}
