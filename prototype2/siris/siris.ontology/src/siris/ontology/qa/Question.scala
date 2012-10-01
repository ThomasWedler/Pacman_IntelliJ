package siris.ontology.qa

import siris.ontology.SVarDescription
import actors.Actor

/**
 * User: dwiebusch
 * Date: 31.03.11
 * Time: 15:19
 */

/**
 * wrapper for questions (to provide autocompletion)
 */
object Question{
  def isSubclassOf(o : SVarDescription[_, _]) =
    new Question("isSubclassOf", Some(o))
}

class Question( s : String, e : Option[SVarDescription[_, _]] = None ){
  def ask() =
    KnowledgeBase ask this

  def ask( sender : Actor ) =
    KnowledgeBase.ask(this, sender)
}