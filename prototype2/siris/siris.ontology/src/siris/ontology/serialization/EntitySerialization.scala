package siris.ontology.serialization

import siris.core.entity.Entity
import siris.core.component.Component
import org.semanticweb.owlapi.model.IRI
import siris.core.svaractor.{SVar, SVarActor}
import java.io.File
import concurrent.SyncVar
import siris.core.entity.typeconversion.ConvertibleTrait

/* author: dwiebusch
 * date: 29.11.2010
 */

object EntitySerialization{
  private val ontology : SirisOWLOntology = new SirisOWLOntology(new File("test.owl"))
  ontology.importOntology(IRI.create(new File("./siris.ontology/ontologyFiles/current/application/SiXtonsCurse/config/config.owl")))

  def serialize(e : Entity, additionalInfo : Map[String, String]) {
    val m = e.getAllSVars.foldLeft(Map[String, String]()){
      (map, triple) => triple._3.owner() match {
        case c : Component => map + (triple._1.name -> c.componentName.name)
        case a : SVarActor => map + (triple._1.name -> ("Actor:" + a.toString))
      }
    }

    val entityClass   = ontology.getClass("Entity")
    val svarClass     = ontology.getClass("SVar")
    val nameProperty  = ontology.getDataProperty("hasName")
    val ownerProperty = ontology.getDataProperty("hasOwner")
    val svarProperty  = ontology.getObjectProperty("hasSVar")
    val individual    = ontology.getIndividual(e.id.toString)

    ontology.createInstance(entityClass, individual)

    m.foreach{ tuple =>
      val svar = ontology.getIndividual(tuple._1)
      ontology.createInstance(svarClass, svar)
      ontology.addObjectProperty(svarProperty, individual, svar)
      ontology.addDataProperty(svar, nameProperty, tuple._1)
      ontology.addDataProperty(svar, ownerProperty, tuple._2)
    }

  }

  private def serialize[T]( svar : SVar[T], sVarIdentifier : Symbol ) {
    val value = new SyncVar[T]
    svar.get( value.set )
    val matchingOntoloyMember = ontology.getClass(sVarIdentifier.name)
    val owner = svar.owner() match {
      case c : Component => c.componentName.name
      case a : SVarActor => a.toString
    }



  }

  def deserialize(s : String) = {
    null
  }
}