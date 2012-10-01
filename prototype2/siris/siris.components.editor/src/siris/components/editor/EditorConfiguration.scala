package siris.components.editor

//Global Types
import siris.ontology.{Symbols, types => gt}

import siris.core.component.ComponentConfiguration
import siris.core.entity.description.{NamedSValList, Semantics, SValList}

/*
* User: martin
* Date: 6/10/11
* Time: 10:23 AM
*/

/**
 *  Used to configure the Editor
 *
 * @param name    The editor's name.
 * @param appName The application's name.
 */
case class EditorConfiguration(appName: String = null, name: String = null) extends ComponentConfiguration {

  def toConfigurationParams: SValList = {
    val result = new SValList()
    if(name != null) result += gt.Name(name)
    if(appName != null) result += gt.NamedContainer(new NamedSValList(Symbols.application, gt.Name(appName)))
    result
  }

  def targetComponentType = Symbols.editor
}