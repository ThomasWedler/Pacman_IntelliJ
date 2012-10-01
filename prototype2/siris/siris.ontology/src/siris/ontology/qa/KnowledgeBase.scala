package siris.ontology.qa

import actors.Actor

/**
 * User: dwiebusch
 * Date: 31.03.11
 * Time: 15:33
 */

object KnowledgeBase extends OWLInterface{
  def ask( q : Question ) : Answer =
    new Answer(q, Set())

  def ask( q : Question, sender : Actor ) : Unit =
    sender ! AnswerMsg(new Answer(q, Set()))
}