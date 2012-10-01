package siris.components.renderer.types

import siris.ontology.{SVarDescription, Symbols}



object ManipulatorList extends SVarDescription[List[siris.components.renderer.createparameter.ElementManipulator], scala.collection.immutable.List[_]]( siris.ontology.types.AnyList as Symbols.manipulatorList createdBy(Nil) )