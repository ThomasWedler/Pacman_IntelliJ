package siris.ontology.serialization

import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.util.DefaultPrefixManager
import org.semanticweb.owlapi.model._
import java.io.File
import siris.ontology.generation.OntologyException


/* author: dwiebusch
 * date: 29.11.2010
 */

class SirisOWLOntology(iri : IRI){
  def this(file : File) = this( IRI.create(file) )
  
  private val manager = OWLManager.createOWLOntologyManager()
  private val factory = manager.getOWLDataFactory()

  private var currentOntology : Option[OWLOntology]          = Some( manager.loadOntology( iri ) )
  private var currentPrefix   : Option[String]               = Some( getOntology.getOntologyID.getOntologyIRI.toString )
  private val prefixManager   : Option[DefaultPrefixManager] = Some( new DefaultPrefixManager(currentPrefix.getOrElse("") + "#") )

  protected def getPrefixManager = prefixManager match {
    case Some(pm) => pm
    case None => throw OntologyException("no prefixmanager defined")
  }

  protected def getOntology : OWLOntology = currentOntology match {
    case Some(ontology) => ontology
    case None => throw OntologyException("no ontology has been loaded")
  }

  def importOntology( iri : IRI ) : Unit =
    manager.loadOntology(iri)

  def getClass( name : String ) =
    factory.getOWLClass( name, getPrefixManager )

  def getObjectProperty( name : String ) =
    factory.getOWLObjectProperty( name, getPrefixManager )

  def getDataProperty( name : String ) =
    factory.getOWLDataProperty( name, getPrefixManager )

  def getIndividual( name : String ) =
    factory.getOWLNamedIndividual( name, getPrefixManager )

  def createInstance(base : OWLClass, individual : OWLIndividual) =
    manager.addAxiom(getOntology, factory.getOWLClassAssertionAxiom(base, individual) )

  def addObjectProperty(property : OWLObjectProperty, individual : OWLIndividual, obj : OWLIndividual) = 
    manager.addAxiom(getOntology, factory.getOWLObjectPropertyAssertionAxiom(property, individual, obj) )

  def addDataProperty(individual : OWLIndividual, property : OWLDataProperty, value : String) =
    manager.addAxiom(getOntology, factory.getOWLDataPropertyAssertionAxiom(property, individual, value) )

  def getAllSubClasses( owlClass : OWLClass ) : List[OWLClass] = {
    val subClasses = owlClass.getSubClasses(getOntology).iterator
    var retVal = List[OWLClass]()
    while (subClasses.hasNext) {
      retVal = subClasses.next.asOWLClass :: retVal
      retVal = retVal ::: getAllSubClasses( retVal.head )
    }
    retVal
  }
}