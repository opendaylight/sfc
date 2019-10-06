/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Matches1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.SfcAction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.AceOperData;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.SourcePortRange;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract exporter.
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-12-02
 */
class AclExporter extends AbstractExporter implements Exporter {

    public static final String ACL = "acl";
    public static final String ACL_NAME = "acl-name";
    public static final String ACCESS_LIST_ENTRIES = "access-list-entries";
    public static final String ACE = "ace";
    public static final String RULE_NAME = "rule-name";
    public static final String MATCHES = "matches";
    public static final String ACTIONS = "actions";
    public static final String ACE_OPER_DATA = "ace-oper-data";
    public static final String INPUT_INTERFACE = "input-interface";
    public static final String DSCP = "dscp";
    public static final String PROTOCOL = "protocol";
    public static final String SOURCE_PORT_RANGE = "source-port-range";
    public static final String DESTINATION_PORT_RANGE = "destination-port-range";
    public static final String DESTINATION_MAC_ADDRESS = "destination-mac-address";
    public static final String DESTINATION_MAC_ADDRESS_MASK = "destination-mac-address-mask";
    public static final String SOURCE_MAC_ADDRESS = "source-mac-address";
    public static final String SOURCE_MAC_ADDRESS_MASK = "source-mac-address-mask";
    public static final String LOWER_PORT = "lower-port";
    public static final String UPPER_PORT = "upper-port";
    public static final String DESTINATION_IPV4_NETWORK = "destination-ipv4-network";
    public static final String SOURCE_IPV4_NETWORK = "source-ipv4-network";
    public static final String DESTINATION_IPV6_NETWORK = "destination-ipv6-network";
    public static final String SOURCE_IPV6_NETWORK = "source-ipv6-network";
    public static final String FLOW_LABEL = "flow-label";
    public static final String SERVICE_FUNCTION_ACL_RENDERED_SERVICE_PATH =
            "service-function-acl:rendered-service-path";
    public static final String MATCH_COUNTER = "match-counter";
    public static final String ACE_APPLICATIONIDS = "service-function-acl:application-id";

    public static final String ACE_IP = "AceIp";
    public static final String ACE_ETH = "AceEth";
    public static final String ACE_IPV4 = "AceIpv4";
    public static final String ACE_IPV6 = "AceIpv6";
    public static final String DENY = "deny";
    public static final String PERMIT = "permit";
    public static final String ACL_RENDERED_SERVICE_PATH = "AclRenderedServicePath";

    private static final Logger LOG = LoggerFactory.getLogger(AclExporter.class);

    @Override
    public String exportJson(DataObject dataObject) {
        String ret = null;
        if (dataObject instanceof Acl) {
            Acl acl = (Acl) dataObject;

            ArrayNode aclArray = mapper.createArrayNode();

            ObjectNode aclNode = mapper.createObjectNode();
            aclNode.put(ACL_NAME, acl.getAclName());

            if (acl.getAccessListEntries() != null) {
                ObjectNode acessListEntriesNode = mapper.createObjectNode();
                List<Ace> aceList = acl.getAccessListEntries().getAce();

                if (aceList != null) {
                    ArrayNode aceArrayNode = mapper.createArrayNode();
                    for (Ace ace : aceList) {
                        ObjectNode aceNode = mapper.createObjectNode();
                        aceNode.put(RULE_NAME, ace.getRuleName());
                        aceNode.put(MATCHES, this.getMatchesObjectNode(ace.getMatches()));
                        aceNode.put(ACTIONS, this.getActionsObjectNode(ace.getActions()));
                        aceNode.put(ACE_OPER_DATA, this.getAceOperDataObjectNode(ace.getAceOperData()));

                        aceArrayNode.add(aceNode);
                    }

                    acessListEntriesNode.putArray(ACE).addAll(aceArrayNode);
                }

                aclNode.put(ACCESS_LIST_ENTRIES, acessListEntriesNode);
            }

            aclArray.add(aclNode);
            try {
                Object aclObject = mapper.treeToValue(aclArray, Object.class);
                ret = mapper.writeValueAsString(aclObject);
                ret = "{\"" + ACL + "\":" + ret + "}";
                LOG.debug("Created Access List JSON: {}", ret);
            } catch (JsonProcessingException e) {
                LOG.error("Error during creation of JSON for Access List {}", acl.getAclName());
            }
        } else {
            throw new IllegalArgumentException("Argument is not an instance of Access List");
        }

        return ret;
    }

    @Override
    public String exportJsonNameOnly(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof Acl) {
            Acl acl = (Acl) dataObject;

            ObjectNode aclNode = mapper.createObjectNode();
            aclNode.put(ACL_NAME, acl.getAclName());
            ArrayNode aclArray = mapper.createArrayNode();
            aclArray.add(aclNode);

            ret = "{\"" + ACL + "\":" + aclArray.toString() + "}";
            LOG.debug("Created Access List JSON: {}", ret);

        } else {
            throw new IllegalArgumentException("Argument is not an instance of Access List");
        }

        return ret;
    }

    private ObjectNode getMatchesObjectNode(Matches matches) {
        if (matches == null) {
            return null;
        }

        ObjectNode matchesNode = mapper.createObjectNode();

        if (matches.getInputInterface() != null) {
            matchesNode.put(INPUT_INTERFACE, matches.getInputInterface());
        }

        if (matches.getAceType() != null) {
            String aceType = matches.getAceType().implementedInterface().getSimpleName();

            switch (aceType) {
                case ACE_IP:
                    AceIp aceIp = (AceIp) matches.getAceType();
                    if (aceIp.getDscp() != null) {
                        matchesNode.put(DSCP, aceIp.getDscp().getValue().toJava());
                    }
                    matchesNode.put(PROTOCOL, aceIp.getProtocol().toJava());
                    matchesNode.put(SOURCE_PORT_RANGE, this.getSourcePortRangeObjectNode(aceIp));
                    matchesNode.put(DESTINATION_PORT_RANGE, this.getDestinationPortRangeObjectNode(aceIp));
                    matchesNode = this.getAceIpVersionObjectNode(aceIp, matchesNode);
                    break;
                case ACE_ETH:
                    AceEth aceEth = (AceEth) matches.getAceType();
                    if (aceEth.getDestinationMacAddress() != null) {
                        matchesNode.put(DESTINATION_MAC_ADDRESS, aceEth.getDestinationMacAddress().getValue());
                    }
                    if (aceEth.getDestinationMacAddressMask() != null) {
                        matchesNode.put(DESTINATION_MAC_ADDRESS_MASK, aceEth.getDestinationMacAddressMask().getValue());
                    }
                    if (aceEth.getSourceMacAddress() != null) {
                        matchesNode.put(SOURCE_MAC_ADDRESS, aceEth.getSourceMacAddress().getValue());
                    }
                    if (aceEth.getSourceMacAddressMask() != null) {
                        matchesNode.put(SOURCE_MAC_ADDRESS_MASK, aceEth.getSourceMacAddressMask().getValue());
                    }
                    break;
                default:
                    break;
            }
        }

        Matches1 matches1 = matches.augmentation(Matches1.class);
        if (matches1 != null) {
            List<String> appIds = matches1.getApplicationId();

            if (appIds != null) {
                ArrayNode an = matchesNode.putArray(ACE_APPLICATIONIDS);
                for (String appId : appIds) {
                    an.add(appId);
                }
            }
        }

        return matchesNode;
    }

    private ObjectNode getSourcePortRangeObjectNode(AceIp aceIp) {
        if (aceIp == null) {
            return null;
        }

        ObjectNode sourcePortRangeNode = null;

        SourcePortRange sourcePortRange = aceIp.getSourcePortRange();
        if (sourcePortRange != null) {
            sourcePortRangeNode = mapper.createObjectNode();
            if (sourcePortRange.getLowerPort() != null) {
                sourcePortRangeNode.put(LOWER_PORT, sourcePortRange.getLowerPort().getValue().toJava());
            }
            if (sourcePortRange.getUpperPort() != null) {
                sourcePortRangeNode.put(UPPER_PORT, sourcePortRange.getUpperPort().getValue().toJava());
            }
        }

        return sourcePortRangeNode;
    }

    private ObjectNode getDestinationPortRangeObjectNode(AceIp aceIp) {
        if (aceIp == null) {
            return null;
        }

        ObjectNode destinationPortRangeNode = null;

        DestinationPortRange destinationPortRange = aceIp.getDestinationPortRange();
        if (destinationPortRange != null) {
            destinationPortRangeNode = mapper.createObjectNode();
            if (destinationPortRange.getLowerPort() != null) {
                destinationPortRangeNode.put(LOWER_PORT, destinationPortRange.getLowerPort().getValue().toJava());
            }
            if (destinationPortRange.getUpperPort() != null) {
                destinationPortRangeNode.put(UPPER_PORT, destinationPortRange.getUpperPort().getValue().toJava());
            }
        }

        return destinationPortRangeNode;
    }

    private ObjectNode getAceIpVersionObjectNode(AceIp aceIp, ObjectNode matchesNode) {
        if (aceIp == null) {
            return matchesNode;
        }

        if (aceIp.getAceIpVersion() != null) {
            String aceIpVersion = aceIp.getAceIpVersion().implementedInterface().getSimpleName();

            switch (aceIpVersion) {
                case ACE_IPV4:
                    AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                    if (aceIpv4.getDestinationIpv4Network() != null) {
                        matchesNode.put(DESTINATION_IPV4_NETWORK, aceIpv4.getDestinationIpv4Network().getValue());
                    }
                    if (aceIpv4.getSourceIpv4Network() != null) {
                        matchesNode.put(SOURCE_IPV4_NETWORK, aceIpv4.getSourceIpv4Network().getValue());
                    }
                    break;
                case ACE_IPV6:
                    AceIpv6 aceIpv6 = (AceIpv6) aceIp.getAceIpVersion();
                    if (aceIpv6.getDestinationIpv6Network() != null) {
                        matchesNode.put(DESTINATION_IPV6_NETWORK, aceIpv6.getDestinationIpv6Network().getValue());
                    }
                    if (aceIpv6.getSourceIpv6Network() != null) {
                        matchesNode.put(SOURCE_IPV6_NETWORK, aceIpv6.getSourceIpv6Network().getValue());
                    }
                    if (aceIpv6.getFlowLabel() != null) {
                        matchesNode.put(FLOW_LABEL, aceIpv6.getFlowLabel().getValue().toJava());
                    }
                    break;
                default:
                    break;
            }
        }

        return matchesNode;
    }

    private ObjectNode getActionsObjectNode(Actions actions) {
        if (actions == null) {
            return null;
        }

        ObjectNode actionsNode = mapper.createObjectNode();

        if (actions.getPacketHandling() != null) {
            String actionType = actions.getPacketHandling().implementedInterface().getSimpleName();

            switch (actionType) {
                case PERMIT:
                    actionsNode.put(PERMIT, "");
                    break;
                case DENY:
                default:
                    actionsNode.put(DENY, "");
                    break;
            }
        }

        Actions1 actions1 = actions.augmentation(Actions1.class);
        if (actions1 != null) {
            SfcAction sfcAction = actions1.getSfcAction();

            if (sfcAction != null) {
                String sfcActionType = sfcAction.implementedInterface().getSimpleName();

                switch (sfcActionType) {
                    case ACL_RENDERED_SERVICE_PATH:
                        AclRenderedServicePath aclRenderedServicePath = (AclRenderedServicePath) sfcAction;
                        actionsNode.put(SERVICE_FUNCTION_ACL_RENDERED_SERVICE_PATH,
                                aclRenderedServicePath.getRenderedServicePath());
                        break;
                    default:
                        break;
                }
            }
        }

        if (actionsNode.size() == 0) {
            return null;
        }

        return actionsNode;
    }

    private ObjectNode getAceOperDataObjectNode(AceOperData aceOperData) {
        if (aceOperData == null) {
            return null;
        }

        ObjectNode aceOperDataNode = null;

        if (aceOperData.getMatchCounter() != null) {
            aceOperDataNode = mapper.createObjectNode();
            aceOperDataNode.put(MATCH_COUNTER, aceOperData.getMatchCounter().getValue().longValue());
        }

        return aceOperDataNode;
    }
}
