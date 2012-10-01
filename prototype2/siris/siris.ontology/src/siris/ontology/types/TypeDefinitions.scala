/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/3/11
 * Time: 10:04 AM
 */
package siris.ontology.types

object TypeDefinitions {

  type Enum = Enumeration#Value


}

object DefaultEnum extends Enumeration {val Foo = Value("Foo")}