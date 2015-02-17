/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.SfcAction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.DefaultActions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.AceOperData;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.SourcePortRange;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AclExporterFactory implements ExporterFactory {
    public static final String _ACCESS_LIST = "access-list";
    public static final String _ACL_NAME = "acl-name";

    @Override
    public Exporter getExporter() {
        return new AclExporter();
    }
}

class AclExporter extends AbstractExporter implements Exporter {

    public static final String _ACCESS_LIST = "access-list";
    public static final String _ACL_NAME = "acl-name";
    public static final String _ACCESS_LIST_ENTRIES = "access-list-entries";
    public static final String _RULE_NAME = "rule-name";
    public static final String _MATCHES = "matches";
    public static final String _ACTIONS = "actions";
    public static final String _ACE_OPER_DATA = "ace-oper-data";
    public static final String _DEFAULT_ACTIONS = "default-actions";
    public static final String _DSCP = "dscp";
    public static final String _IP_PROTOCOL = "ip-protocol";
    public static final String _SOURCE_PORT_RANGE = "source-port-range";
    public static final String _DESTINATION_PORT_RANGE = "destination-port-range";
    public static final String _DESTINATION_MAC_ADDRESS = "destination-mac-address";
    public static final String _DESTINATION_MAC_ADDRESS_MASK = "destination-mac-address-mask";
    public static final String _SOURCE_MAC_ADDRESS = "source-mac-address";
    public static final String _SOURCE_MAC_ADDRESS_MASK = "source-mac-address-mask";
    public static final String _LOWER_PORT = "lower-port";
    public static final String _UPPER_PORT = "upper-port";
    public static final String _DESTINATION_IPV4_ADDRESS = "destination-ipv4-address";
    public static final String _SOURCE_IPV4_ADDRESS = "source-ipv4-address";
    public static final String _DESTINATION_IPV6_ADDRESS = "destination-ipv6-address";
    public static final String _SOURCE_IPV6_ADDRESS = "source-ipv6-address";
    public static final String _FLOW_LABEL = "flow-label";
    public static final String _PERMIT = "permit";
    public static final String _DENY = "deny";
    public static final String _SERVICE_FUNCTION_ACL_RENDERED_SERVICE_PATH = "service-function-acl:rendered-service-path";
    public static final String _MATCH_COUNTER = "match-counter";

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
        if (dataObject instanceof AccessList) {
            AccessList acl = (AccessList) dataObject;

            ArrayNode aclArray = mapper.createArrayNode();

            ObjectNode aclNode = mapper.createObjectNode();
            aclNode.put(_ACL_NAME, acl.getAclName());

            ArrayNode aceArrayNode = mapper.createArrayNode();
            List<AccessListEntries> aceList = acl.getAccessListEntries();
            if (aceList != null) {
                for (AccessListEntries ace : aceList) {
                    ObjectNode aceNode = mapper.createObjectNode();
                    aceNode.put(_RULE_NAME, ace.getRuleName());
                    aceNode.put(_MATCHES, this.getMatchesObjectNode(ace.getMatches()));
                    aceNode.put(_ACTIONS, this.getActionsObjectNode(ace.getActions()));
                    aceNode.put(_ACE_OPER_DATA, this.getAceOperDataObjectNode(ace.getAceOperData()));

                    aceArrayNode.add(aceNode);
                }
                aclNode.putArray(_ACCESS_LIST_ENTRIES).addAll(aceArrayNode);
            }

            aclNode.put(_DEFAULT_ACTIONS, this.getDefaultActionsObjectNode(acl.getDefaultActions()));

            aclArray.add(aclNode);
            try {
                Object aclObject = mapper.treeToValue(aclArray, Object.class);
                ret = mapper.writeValueAsString(aclObject);
                ret = "{\"" + _ACCESS_LIST + "\":" + ret + "}";
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
        if (dataObject instanceof AccessList) {
            AccessList acl = (AccessList) dataObject;

            ObjectNode aclNode = mapper.createObjectNode();
            aclNode.put(_ACL_NAME, acl.getAclName());
            ArrayNode aclArray = mapper.createArrayNode();
            aclArray.add(aclNode);

            ret = "{\"" + _ACCESS_LIST + "\":" + aclArray.toString() + "}";
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

        if (matches.getAceType() != null) {
            String aceType = matches.getAceType().getImplementedInterface().getSimpleName();

            switch (aceType) {
                case ACE_IP:
                    AceIp aceIp = (AceIp) matches.getAceType();
                    if (aceIp.getDscp() != null) {
                        matchesNode.put(_DSCP, aceIp.getDscp().getValue());
                    }
                    matchesNode.put(_IP_PROTOCOL, aceIp.getIpProtocol());
                    matchesNode.put(_SOURCE_PORT_RANGE, this.getSourcePortRangeObjectNode(aceIp));
                    matchesNode.put(_DESTINATION_PORT_RANGE, this.getDestinationPortRangeObjectNode(aceIp));
                    matchesNode = this.getAceIpVersionObjectNode(aceIp, matchesNode);
                    break;
                case ACE_ETH:
                    AceEth aceEth = (AceEth) matches.getAceType();
                    if (aceEth.getDestinationMacAddress() != null) {
                        matchesNode.put(_DESTINATION_MAC_ADDRESS, aceEth.getDestinationMacAddress().getValue());
                    }
                    if (aceEth.getDestinationMacAddressMask() != null) {
                        matchesNode.put(_DESTINATION_MAC_ADDRESS_MASK, aceEth.getDestinationMacAddressMask().getValue());
                    }
                    if (aceEth.getSourceMacAddress() != null) {
                        matchesNode.put(_SOURCE_MAC_ADDRESS, aceEth.getSourceMacAddress().getValue());
                    }
                    if (aceEth.getSourceMacAddressMask() != null) {
                        matchesNode.put(_SOURCE_MAC_ADDRESS_MASK, aceEth.getSourceMacAddressMask().getValue());
                    }
                    break;
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
                sourcePortRangeNode.put(_LOWER_PORT, sourcePortRange.getLowerPort().getValue());
            }
            if (sourcePortRange.getUpperPort() != null) {
                sourcePortRangeNode.put(_UPPER_PORT, sourcePortRange.getUpperPort().getValue());
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
                destinationPortRangeNode.put(_LOWER_PORT, destinationPortRange.getLowerPort().getValue());
            }
            if (destinationPortRange.getUpperPort() != null) {
                destinationPortRangeNode.put(_UPPER_PORT, destinationPortRange.getUpperPort().getValue());
            }
        }

        return destinationPortRangeNode;
    }

    private ObjectNode getAceIpVersionObjectNode(AceIp aceIp, ObjectNode matchesNode) {
        if (aceIp == null) {
            return matchesNode;
        }

        if (aceIp.getAceIpVersion() != null) {
            String aceIpVersion = aceIp.getAceIpVersion().getImplementedInterface().getSimpleName();

            switch (aceIpVersion) {
                case ACE_IPV4:
                    AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                    if (aceIpv4.getDestinationIpv4Address() != null) {
                        matchesNode.put(_DESTINATION_IPV4_ADDRESS, aceIpv4.getDestinationIpv4Address().getValue());
                    }
                    if (aceIpv4.getSourceIpv4Address() != null) {
                        matchesNode.put(_SOURCE_IPV4_ADDRESS, aceIpv4.getSourceIpv4Address().getValue());
                    }
                    break;
                case ACE_IPV6:
                    AceIpv6 aceIpv6 = (AceIpv6) aceIp.getAceIpVersion();
                    if (aceIpv6.getDestinationIpv6Address() != null) {
                        matchesNode.put(_DESTINATION_IPV6_ADDRESS, aceIpv6.getDestinationIpv6Address().getValue());
                    }
                    if (aceIpv6.getSourceIpv6Address() != null) {
                        matchesNode.put(_SOURCE_IPV6_ADDRESS, aceIpv6.getSourceIpv6Address().getValue());
                    }
                    if (aceIpv6.getFlowLabel() != null) {
                        matchesNode.put(_FLOW_LABEL, aceIpv6.getFlowLabel().getValue());
                    }
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
            String actionType = actions.getPacketHandling().getImplementedInterface().getSimpleName();

            switch (actionType) {
                case DENY:
                    actionsNode.put(_DENY, "");
                    break;
                case PERMIT:
                    actionsNode.put(_PERMIT, "");
                    break;
                default:
                    actionsNode.put(_DENY, "");
                    break;
            }
        }

        Actions1 actions1 = actions.getAugmentation(Actions1.class);
        if (actions1 != null) {
            SfcAction sfcAction = actions1.getSfcAction();

            if (sfcAction != null) {
                String sfcActionType = sfcAction.getImplementedInterface().getSimpleName();

                switch (sfcActionType) {
                    case ACL_RENDERED_SERVICE_PATH:
                        AclRenderedServicePath aclRenderedServicePath = (AclRenderedServicePath) sfcAction;
                        actionsNode.put(_SERVICE_FUNCTION_ACL_RENDERED_SERVICE_PATH,
                                aclRenderedServicePath.getRenderedServicePath());
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
            aceOperDataNode.put(_MATCH_COUNTER, aceOperData.getMatchCounter().getValue().longValue());
        }

        return aceOperDataNode;
    }

    private ObjectNode getDefaultActionsObjectNode(DefaultActions defaultActions) {
        if (defaultActions == null) {
            return null;
        }

        ObjectNode defaultActionsNode = mapper.createObjectNode();

        defaultActionsNode.put(_DENY, defaultActions.isDeny());

        return defaultActionsNode;
    }
}

