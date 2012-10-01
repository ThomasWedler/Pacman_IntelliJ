package siris.core.svaractor

/**
 * Created by IntelliJ IDEA.
 * User: fire
 * Date: Aug 3, 2010
 * Time: 4:05:14 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 *  This Exception is thrown when the assumption of a method, to be
 *        called form within a SVarActor, is not fullfilled.
 */
class SVarExceptions

/*
 * @todo DOCUMENT THIS FILE!
 */
case object NotSVarActorException extends java.lang.Throwable
case object NotSVarOwnerException extends java.lang.Throwable
case object InvalidWeakRefException extends java.lang.Throwable
case object OwnerChangeNotInProgressException extends java.lang.Throwable