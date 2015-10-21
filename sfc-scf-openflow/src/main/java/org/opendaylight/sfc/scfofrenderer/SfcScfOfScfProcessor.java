/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.Interface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcScfOfScfProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfScfProcessor.class);

    private static final short TABLE_INDEX_CLASSIFIER = 0;
    private static final short TABLE_INDEX_INGRESS_TRANSPORT = 1;
    private static final short TABLE_INDEX_PATH_MAPPER = 2;
    private static final short TABLE_INDEX_PATH_MAPPER_ACL = 3;
    private static final short TABLE_INDEX_NEXT_HOP = 4;
    private static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;

    private static final int FLOW_PRIORITY_TRANSPORT_INGRESS = 250;
    private static final int FLOW_PRIORITY_ARP_TRANSPORT_INGRESS = 300;
    private static final int FLOW_PRIORITY_PATH_MAPPER = 350;
    private static final int FLOW_PRIORITY_PATH_MAPPER_ACL = 450;
    private static final int FLOW_PRIORITY_NEXT_HOP = 550;
    private static final int FLOW_PRIORITY_TRANSPORT_EGRESS = 650;
    private static final int FLOW_PRIORITY_MATCH_ANY = 5;

    public SfcScfOfScfProcessor() {}

    private void removeFlowFromConfig(final String nodeName, TableKey tableKey, FlowKey flowKey) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(nodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeBuilder.getKey())
            .augmentation(FlowCapableNode.class)
            .child(Table.class, tableKey)
            .child(Flow.class, flowKey)
            .build();

        if (!SfcDataStoreAPI.deleteTransactionAPI(flowInstanceId, LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("Failed to remove Flow on node: {}", nodeName);
        }
    }

    private void writeFlowToConfig(final String nodeName, FlowBuilder flow) {
        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(nodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        // Create the flow path, which will include the Node, Table, and Flow
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeBuilder.getKey())
            .augmentation(FlowCapableNode.class)
            .child(Table.class, new TableKey(flow.getTableId()))
            .child(Flow.class, flow.getKey())
            .build();

        if (!SfcDataStoreAPI.writeMergeTransactionAPI(flowInstanceId, flow.build(),
                LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("Failed to create Flow on node: {}", nodeName);
        }
    }

    private static Match getMatch(NodeConnectorId inPort, Matches matches) {
        MatchBuilder mb = new MatchBuilder();

        mb.setInPort(inPort);

        if (matches.getAceType() instanceof AceEth) {
            AceEth eth = (AceEth) matches.getAceType();

            // don't support mac mask

            if (eth.getSourceMacAddress() != null) {
                SfcOpenflowUtils.addMatchSrcMac(mb, eth.getSourceMacAddress().getValue());
            }
            if (eth.getDestinationMacAddress() != null) {
                SfcOpenflowUtils.addMatchDstMac(mb, eth.getDestinationMacAddress().getValue());
            }

        } else if (matches.getAceType() instanceof AceIp) {
            AceIp aceip = (AceIp) matches.getAceType();

            if (aceip.getDscp() != null) {
                SfcOpenflowUtils.addMatchDscp(mb, aceip.getDscp().getValue());
            }

            if (aceip.getProtocol() != null) {
                SfcOpenflowUtils.addMatchIpProtocol(mb, aceip.getProtocol());

                Integer srcPort = null;
                Integer dstPort = null;

                if (aceip.getSourcePortRange() != null &&
                    aceip.getSourcePortRange().getLowerPort() != null &&
                    aceip.getSourcePortRange().getLowerPort().getValue() != null) {
                    srcPort = aceip.getSourcePortRange().getLowerPort().getValue();
                }
                if (aceip.getDestinationPortRange() != null &&
                    aceip.getDestinationPortRange().getLowerPort() != null &&
                    aceip.getDestinationPortRange().getLowerPort().getValue() != null) {
                    dstPort = aceip.getDestinationPortRange().getLowerPort().getValue();
                }

                // don't support port range
                switch (aceip.getProtocol()) {
                    case SfcOpenflowUtils.IP_PROTOCOL_UDP:

                        if (srcPort != null) {
                            SfcOpenflowUtils.addMatchSrcUdpPort(mb, srcPort.intValue());
                        }
                        if (dstPort != null) {
                            SfcOpenflowUtils.addMatchDstUdpPort(mb, dstPort.intValue());
                        }
                        break;

                    case SfcOpenflowUtils.IP_PROTOCOL_TCP:

                        if (srcPort != null) {
                            SfcOpenflowUtils.addMatchSrcTcpPort(mb, srcPort.intValue());
                        }
                        if (dstPort != null) {
                            SfcOpenflowUtils.addMatchDstTcpPort(mb, dstPort.intValue());
                        }
                        break;

                    case SfcOpenflowUtils.IP_PROTOCOL_SCTP:
                        if (srcPort != null) {
                            SfcOpenflowUtils.addMatchSrcSctpPort(mb, srcPort.intValue());
                        }
                        if (dstPort != null) {
                            SfcOpenflowUtils.addMatchDstSctpPort(mb, dstPort.intValue());
                        }
                        break;
                }
            }

            if (aceip.getAceIpVersion() instanceof AceIpv4) {
                AceIpv4 ipv4 = (AceIpv4) aceip.getAceIpVersion();
                SfcOpenflowUtils.addMatchEtherType(mb, SfcOpenflowUtils.ETHERTYPE_IPV4);

                Ipv4Prefix src = ipv4.getSourceIpv4Network();
                if (src != null) {
                    String s[] = src.getValue().split("/");
                    SfcOpenflowUtils.addMatchSrcIpv4(mb, s[0], Integer.valueOf(s[1]).intValue());
                }

                Ipv4Prefix dst = ipv4.getDestinationIpv4Network();
                if (dst != null) {
                    String d[] = dst.getValue().split("/");
                    SfcOpenflowUtils.addMatchDstIpv4(mb, d[0], Integer.valueOf(d[1]).intValue());
                }
            }
            if (aceip.getAceIpVersion() instanceof AceIpv6) {
                AceIpv6 ipv6 = (AceIpv6) aceip.getAceIpVersion();
                SfcOpenflowUtils.addMatchEtherType(mb, SfcOpenflowUtils.ETHERTYPE_IPV6);

                Ipv6Prefix src = ipv6.getSourceIpv6Network();
                if (src != null) {
                    String s[] = src.getValue().split("/");
                    SfcOpenflowUtils.addMatchSrcIpv6(mb, s[0], Integer.valueOf(s[1]).intValue());
                }

                Ipv6Prefix dst = ipv6.getDestinationIpv6Network();
                if (dst != null ) {
                    String d[] = dst.getValue().split("/");
                    SfcOpenflowUtils.addMatchDstIpv6(mb, d[0], Integer.valueOf(d[1]).intValue());
                }
            }
        }
        return mb.build();
    }

    private static FlowBuilder createClassifierFlow(short tableId, String flow, Match match, SfcNshHeader sfcNshHeader,
            int outPort) {
        int order = 0;
        Integer priority = 1000;

        String dstIp = sfcNshHeader.getVxlanIpDst().getValue();
        Action setTunIpDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, order++);
        Action setNsp = SfcOpenflowUtils.createActionNxSetNsp(sfcNshHeader.getNshNsp(), order++);
        Action setNsi = SfcOpenflowUtils.createActionNxSetNsi(sfcNshHeader.getNshNsi(), order++);
        Action setC1 = SfcOpenflowUtils.createActionNxSetNshc1(sfcNshHeader.getNshMetaC1(), order++);
        Action setC2 = SfcOpenflowUtils.createActionNxSetNshc2(sfcNshHeader.getNshMetaC2(), order++);
        Action setC3 = SfcOpenflowUtils.createActionNxSetNshc3(sfcNshHeader.getNshMetaC3(), order++);
        Action setC4 = SfcOpenflowUtils.createActionNxSetNshc4(sfcNshHeader.getNshMetaC4(), order++);
        Action out = SfcOpenflowUtils.createActionOutPort(outPort, order++);

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flow))
            .setTableId(tableId)
            .setKey(new FlowKey(new FlowId(flow)))
            .setPriority(Integer.valueOf(priority))
            .setMatch(match)
            .setInstructions(SfcOpenflowUtils.createInstructionsBuilder(SfcOpenflowUtils
                .createActionsInstructionBuilder(setTunIpDst, setNsp, setNsi, setC1, setC2, setC3, setC4, out))
                .build());
        return flowb;
    }

    private void initClassifierTable(String nodeName) {

        int order = 0;

        List<Instruction> instructions = new ArrayList<Instruction>();
        GoToTableBuilder gotoIngress = SfcOpenflowUtils.createActionGotoTable(TABLE_INDEX_INGRESS_TRANSPORT);

        InstructionBuilder ib = new InstructionBuilder();
        ib.setKey(new InstructionKey(order));
        ib.setOrder(order++);
        ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoIngress.build()).build());
        instructions.add(ib.build());

        MatchBuilder match = new MatchBuilder();

        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);

        FlowBuilder fb = SfcOpenflowUtils.createFlowBuilder(TABLE_INDEX_CLASSIFIER, FLOW_PRIORITY_MATCH_ANY, "MatchAny",
                match, isb);

        writeFlowToConfig(nodeName, fb);
    }

    public void createdServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        LOG.debug("\ncreate ServiceFunctionClassifier name: {} ACL: {} SFF: {}", scf.getName(), scf.getAccessList(),
                scf.getSclServiceFunctionForwarder());

        Acl acl = SfcProviderAclAPI.readAccessList(scf.getAccessList());
        String aclName = acl.getAclName();
        AccessListEntries accessListEntries = acl.getAccessListEntries();
        List<Ace> acesList = accessListEntries.getAce();

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();

        for (SclServiceFunctionForwarder sclsff : sfflist) {
            SffName sffName = new SffName(sclsff.getName());
            NodeConnectorId inPort = null;

            ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
            sff = SfcOvsUtil.augmentSffWithOpenFlowNodeId(sff);
            String nodeName = SfcOvsPortUtils.getSffOpenFlowNodeName(sff);
            int outPort = SfcOvsPortUtils.getVxlanOfPort(nodeName);
            initClassifierTable(nodeName);

            if (sclsff.getAttachmentPointType() instanceof Interface) {
                Interface itf = (Interface) sclsff.getAttachmentPointType();
                String itfName = itf.getInterface();
                inPort = SfcOvsPortUtils.getOfPortByName(nodeName, itfName);
            }

            for (Ace ace : acesList) {
                String ruleName = ace.getRuleName();
                Matches matches = ace.getMatches();
                Actions actions = ace.getActions();

                // Match
                Match match = getMatch(inPort, matches);

                // Action
                Actions1 a1 = actions.getAugmentation(Actions1.class);
                AclRenderedServicePath path = (AclRenderedServicePath) a1.getSfcAction();
                RspName rspName = new RspName(path.getRenderedServicePath());
                SfcNshHeader nsh = SfcNshHeader.getSfcNshHeader(rspName);

                StringBuffer key = new StringBuffer();
                key.append(scf.getName()).append(aclName).append(ruleName);

                FlowBuilder flow = createClassifierFlow(TABLE_INDEX_CLASSIFIER, key.toString(), match, nsh, outPort);
                writeFlowToConfig(nodeName, flow);
            }
        }
    }

    public void deletedServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        LOG.debug("\ndelete ServiceFunctionClassifier name: {} ACL: {} SFF: {}", scf.getName(), scf.getAccessList(),
                scf.getSclServiceFunctionForwarder());

        Acl acl = SfcProviderAclAPI.readAccessList(scf.getAccessList());
        String aclName = acl.getAclName();
        AccessListEntries accessListEntries = acl.getAccessListEntries();
        List<Ace> acesList = accessListEntries.getAce();

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();

        for (SclServiceFunctionForwarder sclsff : sfflist) {
            for (Ace ace : acesList) {

                String ruleName = ace.getRuleName();
                SffName sffName = new SffName(sclsff.getName());

                ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
                sff = SfcOvsUtil.augmentSffWithOpenFlowNodeId(sff);
                String nodeName = SfcOvsPortUtils.getSffOpenFlowNodeName(sff);

                StringBuffer key = new StringBuffer();
                key.append(scf.getName()).append(aclName).append(ruleName);

                removeFlowFromConfig(nodeName, new TableKey(TABLE_INDEX_CLASSIFIER),
                                                            new FlowKey(new FlowId(key.toString())));
            }
        }
    }

    public void updatedServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        // TBD
    }
}
