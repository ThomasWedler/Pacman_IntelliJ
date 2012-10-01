package siris.components.remote

//Global Types
import siris.ontology.{Symbols, types => gt}

import siris.core.component.ComponentConfiguration
import siris.components.eventhandling.EventDescription
import siris.core.entity.description.{SVal, NamedSValList, Semantics, SValList}

/*
* User: martin
* Date: 6/10/11
* Time: 10:23 AM
*/

/**
 *  Used to configure the RemoteEventPassingComponent
 *
 * @param localName   The "network name" of the RemoteEventPassingComponent
 * @param localPort   The port on which the RemoteEventPassingComponent is listening.
 * @param remoteName  The "network name" to which the RemoteEventPassingComponent connects.
 * @param remoteIp    The ip to which the RemoteEventPassingComponent connects.
 * @param remotePort  The port to which the RemoteEventPassingComponent connects.
 * @param events      Descriptions of Events that shall be passed to the remote component.
 */
case class RemoteEventPassingConfiguration( localName: Symbol, localPort: Int,
                                            remoteName: Symbol, remoteIp: String, remotePort: Int,
                                            events: EventDescription*) extends ComponentConfiguration {

  def toConfigurationParams: SValList =
    SValList(
      Seq[SVal[_]](
        gt.NamedContainer(NamedSValList(Symbols.local, gt.Identifier(localName), gt.Port(localPort))),
        gt.NamedContainer(NamedSValList(Symbols.remote, gt.Identifier(remoteName), gt.Ip(remoteIp), gt.Port(remotePort)))
      ).union(events.map(gt.EventDescription(_)).toSeq):_*
    )

  def targetComponentType = Symbols.network
}
