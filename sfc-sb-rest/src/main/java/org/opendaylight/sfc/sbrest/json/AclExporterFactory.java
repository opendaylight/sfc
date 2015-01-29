package org.opendaylight.sfc.sbrest.json;

import java.util.List;

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
    @Override
    public Exporter getExporter() {
        return new AclExporter();
    }
}

class AclExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(AclExporter.class);

    @Override
    public String exportJson(DataObject dataObject) {

        String ret;
        if (dataObject instanceof AccessList) {
            AccessList acl = (AccessList) dataObject;

            ObjectNode aclNode = mapper.createObjectNode();
            aclNode.put("acl-name", acl.getAclName());

            ArrayNode aceArrayNode = mapper.createArrayNode();
            List<AccessListEntries> aceList = acl.getAccessListEntries();
            if (aceList != null) {
                for (AccessListEntries ace : aceList) {
                    ObjectNode aceNode = mapper.createObjectNode();
                    aceNode.put("rule-name", ace.getRuleName());
                    aceNode.put("matches", this.getMatchesObjectNode(ace.getMatches()));
                    aceNode.put("actions", this.getActionsObjectNode(ace.getActions()));
                    aceNode.put("ace-oper-data", this.getAceOperDataObjectNode(ace.getAceOperData()));

                    aceArrayNode.add(aceNode);
                }
                aclNode.putArray("access-list-entries").addAll(aceArrayNode);
            }

            aclNode.put("default-actions", this.getDefaultActionsObjectNode(acl.getDefaultActions()));

            ret = "{ \"access-list\" : " + aclNode.toString() + " }";
            LOG.debug("Created Access List JSON: {}", ret);

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

            aclNode.put("acl-name", acl.getAclName());

            ret = "{ \"access-list\" : " + aclNode.toString() + " }";
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
                case "AceIp":
                    AceIp aceIp = (AceIp) matches.getAceType();
                    if (aceIp.getDscp() != null) {
                        matchesNode.put("dscp", aceIp.getDscp().getValue());
                    }
                    matchesNode.put("ip-protocol", aceIp.getIpProtocol());
                    matchesNode.put("source-port-range", this.getSourcePortRangeObjectNode(aceIp));
                    matchesNode.put("destination-port-range", this.getDestinationPortRangeObjectNode(aceIp));
                    matchesNode = this.getAceIpVersionObjectNode(aceIp, matchesNode);
                    break;
                case "AceEth":
                    AceEth aceEth = (AceEth) matches.getAceType();
                    if (aceEth.getDestinationMacAddress() != null) {
                        matchesNode.put("destination-mac-address", aceEth.getDestinationMacAddress().getValue());
                    }
                    if (aceEth.getDestinationMacAddressMask() != null) {
                        matchesNode.put("destination-mac-address-mask", aceEth.getDestinationMacAddressMask().getValue());
                    }
                    if (aceEth.getSourceMacAddress() != null) {
                        matchesNode.put("source-mac-address", aceEth.getSourceMacAddress().getValue());
                    }
                    if (aceEth.getSourceMacAddressMask() != null) {
                        matchesNode.put("source-mac-address-mask", aceEth.getSourceMacAddressMask().getValue());
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

        ObjectNode sourcePortRangeNode = mapper.createObjectNode();

        SourcePortRange sourcePortRange = aceIp.getSourcePortRange();
        if (sourcePortRange != null) {
            if (sourcePortRange.getLowerPort() != null) {
                sourcePortRangeNode.put("lower-port", sourcePortRange.getLowerPort().getValue());
            }
            if (sourcePortRange.getUpperPort() != null) {
                sourcePortRangeNode.put("upper-port", sourcePortRange.getUpperPort().getValue());
            }
        }

        return sourcePortRangeNode;
    }

    private ObjectNode getDestinationPortRangeObjectNode(AceIp aceIp) {
        if (aceIp == null) {
            return null;
        }

        ObjectNode destinationPortRangeNode = mapper.createObjectNode();

        DestinationPortRange destinationPortRange = aceIp.getDestinationPortRange();
        if (destinationPortRange != null) {
            if (destinationPortRange.getLowerPort() != null) {
                destinationPortRangeNode.put("lower-port", destinationPortRange.getLowerPort().getValue());
            }
            if (destinationPortRange.getUpperPort() != null) {
                destinationPortRangeNode.put("upper-port", destinationPortRange.getUpperPort().getValue());
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
                case "AceIpv4":
                    AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                    if (aceIpv4.getDestinationIpv4Address() != null) {
                        matchesNode.put("destination-ipv4-address", aceIpv4.getDestinationIpv4Address().getValue());
                    }
                    if (aceIpv4.getSourceIpv4Address() != null) {
                        matchesNode.put("source-ipv4-address", aceIpv4.getSourceIpv4Address().getValue());
                    }
                    break;
                case "AceIpv6":
                    AceIpv6 aceIpv6 = (AceIpv6) aceIp.getAceIpVersion();
                    if (aceIpv6.getDestinationIpv6Address() != null) {
                        matchesNode.put("destination-ipv6-address", aceIpv6.getDestinationIpv6Address().getValue());
                    }
                    if (aceIpv6.getSourceIpv6Address() != null) {
                        matchesNode.put("source-ipv6-address", aceIpv6.getSourceIpv6Address().getValue());
                    }
                    if (aceIpv6.getFlowLabel() != null) {
                        matchesNode.put("flow-label", aceIpv6.getFlowLabel().getValue());
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
                case "deny":
                    actionsNode.put("deny", "");
                    break;
                case "permit":
                    actionsNode.put("permit", "");
                    break;
                default:
                    actionsNode.put("deny", "");
                    break;
            }
        }

        Actions1 actions1 = actions.getAugmentation(Actions1.class);
        if (actions1 != null) {
            SfcAction sfcAction = actions1.getSfcAction();

            if (sfcAction != null) {
                String sfcActionType = sfcAction.getImplementedInterface().getSimpleName();

                switch (sfcActionType) {
                    case "AclRenderedServicePath":
                        AclRenderedServicePath aclRenderedServicePath = (AclRenderedServicePath) sfcAction;
                        actionsNode.put("rendered-service-path", aclRenderedServicePath.getRenderedServicePath());
                        break;
                }
            }
        }

        return actionsNode;
    }

    private ObjectNode getAceOperDataObjectNode(AceOperData aceOperData) {
        if (aceOperData == null) {
            return null;
        }

        ObjectNode aceOperDataNode = mapper.createObjectNode();

        if (aceOperData.getMatchCounter() != null) {
            aceOperDataNode.put("match-counter", aceOperData.getMatchCounter().getValue().longValue());
        }

        return aceOperDataNode;
    }

    private ObjectNode getDefaultActionsObjectNode(DefaultActions defaultActions) {
        if (defaultActions == null) {
            return null;
        }

        ObjectNode defaultActionsNode = mapper.createObjectNode();

        defaultActionsNode.put("deny", defaultActions.isDeny());

        return defaultActionsNode;
    }
}

