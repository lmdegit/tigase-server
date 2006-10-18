/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.xmpp.impl;

import java.util.Queue;
import java.util.logging.Logger;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.XMPPServiceCollector;
import tigase.util.JID;
import tigase.util.ElementUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Implementation of JEP-030.
 *
 *
 * Created: Mon Mar 27 20:45:36 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ServiceDiscovery extends XMPPProcessor
	implements XMPPProcessorIfc {

	private static final Logger log =
    Logger.getLogger("tigase.xmpp.impl.ServiceDiscovery");

	protected static final String ID = "disco";
  protected static final String[] ELEMENTS =
	{"query", "query", Command.GETDISCO.toString()};
  protected static final String[] XMLNSS = {
    XMPPServiceCollector.INFO_XMLNS,
		XMPPServiceCollector.ITEMS_XMLNS,
		Command.XMLNS
	};
  protected static final String[] DISCO_FEATURES = {
    XMPPServiceCollector.INFO_XMLNS,
		XMPPServiceCollector.ITEMS_XMLNS
	};

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

  public String[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

	public void process(final Packet packet, final XMPPResourceConnection session,
		final Queue<Packet> results) {

		if (session == null) {
			return;
		} // end of if (session == null)

		try {
			// Maybe it is message to admininstrator:
			String nodeId = null;
			if (packet.getElemTo() != null) {
				nodeId = JID.getNodeID(packet.getElemTo());
			} // end of if (packet.getElemTo() != null)

			if (packet.isCommand() && packet.getCommand() == Command.GETDISCO
				&& packet.getType() == StanzaType.result) {
				// Send it back to user.
				Element query = packet.getElement().getChild("query");
				Element iq =
					ElementUtils.createIqQuery(session.getDomain(), session.getJID(),
						StanzaType.result, packet.getElemId(), query);
				Packet result = new Packet(iq);
				result.setTo(session.getConnectionId());
				results.offer(result);
				return;
			} // end of if (packet.isCommand()
				// && packet.getCommand() == Command.GETSTATS
				// && packet.getType() == StanzaType.result)


			// If ID part of user account contains only host name
			// and this is local domain it is message to admin
			if (nodeId == null || nodeId.equals("")
				|| nodeId.equalsIgnoreCase(session.getDomain())) {
				Element query = packet.getElement().getChild("query");
				Packet discoCommand = Command.GETDISCO.getPacket(session.getJID(),
					session.getDomain(), StanzaType.get, packet.getElemId());
				Element commandElement = discoCommand.getElement();
				commandElement.setAttribute("query", query.getXMLNS());
				if (query.getAttribute("node") != null) {
					commandElement.setAttribute("node", query.getAttribute("node"));
				} // end of if (query.getAttribute("node") != null)
				results.offer(discoCommand);
				return;
			}

			if (nodeId.equals(session.getUserId())) {
				// Yes this is message to 'this' client
				Element elem = (Element)packet.getElement().clone();
				Packet result = new Packet(elem);
				result.setTo(session.getConnectionId());
				result.setFrom(packet.getTo());
				results.offer(result);
			} else {
				// This is message to some other client so I need to
				// set proper 'from' attribute whatever it is set to now.
				// Actually processor should not modify request but in this
				// case it is absolutely safe and recommended to set 'from'
				// attribute
				Element result = (Element)packet.getElement().clone();
				// According to spec we must set proper FROM attribute
				result.setAttribute("from", session.getJID());
				results.offer(new Packet(result));
			} // end of else
		} catch (NotAuthorizedException e) {
      log.warning(
				"Received stats request but user session is not authorized yet: " +
        packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch
	}

} // ServiceDiscovery
