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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.Interface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Actions;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;

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


    private static FlowBuilder createClassifierOutFlow(short tableId, String flow, Match match, SfcNshHeader sfcNshHeader,
            Long outPort) {
        int order = 0;
        Integer priority = 1000;

        String dstIp = sfcNshHeader.getVxlanIpDst().getValue();
        Action setTunIpDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, order++);
        Action setNsp = SfcOpenflowUtils.createActionNxSetNsp(sfcNshHeader.getNshNsp(), order++);
        Action setNsi = SfcOpenflowUtils.createActionNxSetNsi(sfcNshHeader.getNshStartNsi(), order++);
        Action setC1 = SfcOpenflowUtils.createActionNxSetNshc1(sfcNshHeader.getNshMetaC1(), order++);
        Action setC2 = SfcOpenflowUtils.createActionNxSetNshc2(sfcNshHeader.getNshMetaC2(), order++);
        Action setC3 = SfcOpenflowUtils.createActionNxSetNshc3(sfcNshHeader.getNshMetaC3(), order++);
        Action setC4 = SfcOpenflowUtils.createActionNxSetNshc4(sfcNshHeader.getNshMetaC4(), order++);
        Action out = SfcOpenflowUtils.createActionOutPort(outPort.intValue(), order++);

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

    private static FlowBuilder createClassifierInFlow(short tableId, String flow, SfcNshHeader sfcNshHeader, Long port) {
        int order = 0;
        Integer priority = 1000;

        MatchBuilder mb = new MatchBuilder();

        SfcOpenflowUtils.addMatchNshNsp(mb, sfcNshHeader.getNshNsp());
        SfcOpenflowUtils.addMatchNshNsi(mb, sfcNshHeader.getNshEndNsi());

        Action out = SfcOpenflowUtils.createActionOutPort(port.intValue(), order++);

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flow))
            .setTableId(tableId)
            .setKey(new FlowKey(new FlowId(flow)))
            .setPriority(Integer.valueOf(priority))
            .setMatch(mb.build())
            .setInstructions(SfcOpenflowUtils.createInstructionsBuilder(SfcOpenflowUtils
                .createActionsInstructionBuilder(out))
                .build());
        return flowb;
    }

    private static FlowBuilder createClassifierRelayFlow(short tableId, String flow, SfcNshHeader sfcNshHeader, Long port) {
        int order = 0;
        Integer priority = 1000;

        MatchBuilder mb = new MatchBuilder();

        SfcOpenflowUtils.addMatchNshNsp(mb, sfcNshHeader.getNshNsp());
        SfcOpenflowUtils.addMatchNshNsi(mb, sfcNshHeader.getNshEndNsi());

        String dstIp = sfcNshHeader.getVxlanIpDst().getValue();
        Action setTunIpDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, order++);
        Action mvNsp = SfcOpenflowUtils.createActionNxMoveNsp(order++);
        Action mvNsi = SfcOpenflowUtils.createActionNxMoveNsi(order++);
        Action mvC1 = SfcOpenflowUtils.createActionNxMoveNsc1(order++);
        Action mvC2 = SfcOpenflowUtils.createActionNxMoveNsc2(order++);
        Action out = SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++);

        FlowBuilder flowb = new FlowBuilder();
        flowb.setId(new FlowId(flow))
            .setTableId(tableId)
            .setKey(new FlowKey(new FlowId(flow)))
            .setPriority(Integer.valueOf(priority))
            .setMatch(mb.build())
            .setInstructions(SfcOpenflowUtils.createInstructionsBuilder(SfcOpenflowUtils
                .createActionsInstructionBuilder(setTunIpDst, mvNsp, mvNsi, mvC1, mvC2, out))
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
        if (scf == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: scf is null");
            return;
        }

        LOG.debug("\ncreate ServiceFunctionClassifier name: {} ACL: {} SFF: {}", scf.getName(), scf.getAccessList(),
                scf.getSclServiceFunctionForwarder());

        Acl acl = SfcProviderAclAPI.readAccessList(scf.getAccessList());
        if (acl == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: acl is null");
            return;
        }

        String aclName = acl.getAclName();
        if (aclName == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: aclName is null");
            return;
        }

        AccessListEntries accessListEntries = acl.getAccessListEntries();
        if (accessListEntries == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: accessListEntries is null");
            return;
        }

        List<Ace> acesList = accessListEntries.getAce();
        if (acesList == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: acesList is null");
            return;
        }

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: sfflist is null");
            return;
        }

        for (SclServiceFunctionForwarder sclsff : sfflist) {
            SffName sffName = new SffName(sclsff.getName());

            Long inPort = null;

            ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
            if (sff == null) {
                LOG.error("\ncreatedServiceFunctionClassifier: sff is null");
                continue;
            }

            String nodeName = SfcOvsPortUtils.getSffOpenFlowNodeName(sff);
            if (nodeName == null) {
                LOG.error("\ncreatedServiceFunctionClassifier: nodeName is null");
                continue;
            }

            Long outPort = SfcOvsPortUtils.getVxlanOfPort(nodeName);
            initClassifierTable(nodeName);

            if (sclsff.getAttachmentPointType() instanceof Interface) {
                Interface itf = (Interface) sclsff.getAttachmentPointType();
                if (itf == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: attachment point is null");
                    continue;
                }

                String itfName = itf.getInterface();
                if (itf == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: interface is null");
                    continue;
                }
                inPort = SfcOvsPortUtils.getOfPortByName(nodeName, itfName);
                if (inPort == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: port is null");
                    continue;
                }
            }

            for (Ace ace : acesList) {

                String ruleName = ace.getRuleName();
                if (ruleName == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: ruleName is null");
                    continue;
                }

                StringBuffer sb = new StringBuffer();
                sb.append(nodeName).append(":");
                sb.append(String.valueOf(inPort));
                NodeConnectorId port = new NodeConnectorId(sb.toString());

                // Match
                Match match = new SfcMatch()
                                  .setPortMatch(port)
                                  .setAclMatch(ace.getMatches())
                                  .build();

                // Action
                Actions actions = ace.getActions();
                if (ruleName == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: action is null");
                    continue;
                }

                Actions1 a1 = actions.getAugmentation(Actions1.class);
                if (a1 == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: action augment is null");
                    continue;
                }

                AclRenderedServicePath path = (AclRenderedServicePath) a1.getSfcAction();
                if (path == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: sfc action is null");
                    continue;
                }

                RspName rspName = new RspName(path.getRenderedServicePath());
                SfcNshHeader nsh = SfcNshHeader.getSfcNshHeader(rspName);


                if (nsh == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: nsh is null");
                    continue;
                }

                StringBuffer key = new StringBuffer();
                key.append(scf.getName()).append(aclName).append(ruleName).append("out");
                FlowBuilder outFlow = createClassifierOutFlow(TABLE_INDEX_CLASSIFIER, key.toString(), match, nsh, outPort);
                if (outFlow == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: out flow is null");
                    continue;
                }
                writeFlowToConfig(nodeName, outFlow);

                RspName reverseRspName = null;
                if (path.getRenderedServicePath().endsWith("-Reverse")) {
                    reverseRspName = new RspName(path.getRenderedServicePath().replaceFirst("-Reverse", ""));
                } else {
                    reverseRspName = new RspName(path.getRenderedServicePath() + "-Reverse");
                }

                SfcNshHeader reverseNsh = SfcNshHeader.getSfcNshHeader(reverseRspName);

                if (reverseNsh == null) {
                    LOG.debug("\ncreatedServiceFunctionClassifier: reverseNsh is null");
                } else {
                    key = new StringBuffer();
                    key.append(scf.getName()).append(aclName).append(ruleName).append("in");
                    FlowBuilder inFlow = createClassifierInFlow(TABLE_INDEX_CLASSIFIER, key.toString(), reverseNsh, inPort);
                    writeFlowToConfig(nodeName, inFlow);

                    SffName lastSffName = reverseNsh.getSffName();
                    if (lastSffName != null &&
                        !reverseNsh.getSffName().equals(sffName)) {
                        ServiceFunctionForwarder lastSff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(lastSffName);
                        String lastNodeName = SfcOvsPortUtils.getSffOpenFlowNodeName(lastSff);
                        if (lastNodeName == null) {
                            LOG.error("\ncreatedServiceFunctionClassifier: lastNodeName is null");
                        }
                        outPort = SfcOvsPortUtils.getVxlanOfPort(lastNodeName);
                        key = new StringBuffer();
                        key.append(scf.getName()).append(aclName).append(ruleName).append("relay");
                        Ip ip = SfcOvsPortUtils.getSffIpDataLocator(sff, VxlanGpe.class);
                        reverseNsh.setVxlanIpDst(ip.getIp().getIpv4Address());
                        reverseNsh.setVxlanUdpPort(ip.getPort());
                        FlowBuilder relayFlow = createClassifierRelayFlow(TABLE_INDEX_CLASSIFIER, key.toString(), reverseNsh, outPort);
                        writeFlowToConfig(lastNodeName, relayFlow);
                    }
                }
            }
        }
    }

    public void deletedServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        if (scf == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: scf is null");
            return;
        }
        LOG.debug("\ndelete ServiceFunctionClassifier name: {} ACL: {} SFF: {}", scf.getName(), scf.getAccessList(),
                scf.getSclServiceFunctionForwarder());

        Acl acl = SfcProviderAclAPI.readAccessList(scf.getAccessList());
        if (acl == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: acl is null");
            return;
        }

        String aclName = acl.getAclName();
        if (aclName == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: aclName is null");
            return;
        }

        AccessListEntries accessListEntries = acl.getAccessListEntries();
        if (accessListEntries == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: accessListEntries is null");
            return;
        }
        List<Ace> acesList = accessListEntries.getAce();
        if (acesList == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: acesList is null");
            return;
        }

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: sfflist is null");
            return;
        }

        for (SclServiceFunctionForwarder sclsff : sfflist) {
            for (Ace ace : acesList) {

                String ruleName = ace.getRuleName();
                if (ruleName == null) {
                    LOG.error("\ndeletedServiceFunctionClassifier: ruleName is null");
                    continue;
                }

                SffName sffName = new SffName(sclsff.getName());

                ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
                if (sff == null) {
                    LOG.error("\ndeletedServiceFunctionClassifier: sff is null");
                    continue;
                }
                sff = SfcOvsUtil.augmentSffWithOpenFlowNodeId(sff);
                if (sff == null) {
                    LOG.error("\ndeletedServiceFunctionClassifier: sff augment is null");
                    continue;
                }
                String nodeName = SfcOvsPortUtils.getSffOpenFlowNodeName(sff);
                if (nodeName == null) {
                    LOG.error("\ndeletedServiceFunctionClassifier: nodeName is null");
                    continue;
                }

                StringBuffer key = new StringBuffer();
                key.append(scf.getName()).append(aclName).append(ruleName).append(".out");

                removeFlowFromConfig(nodeName, new TableKey(TABLE_INDEX_CLASSIFIER),
                                                            new FlowKey(new FlowId(key.toString())));
                key = new StringBuffer();
                key.append(scf.getName()).append(aclName).append(ruleName).append(".in");

                removeFlowFromConfig(nodeName, new TableKey(TABLE_INDEX_CLASSIFIER),
                                                            new FlowKey(new FlowId(key.toString())));
            }
        }
    }

    public void updatedServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        // TBD
    }
}
