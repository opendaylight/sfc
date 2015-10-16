/*
 * Copyright (c) 2014 ConteXtream Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.scfofrenderer.openflow;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.mpls.action._case.PopMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.mpls.action._case.PushMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.type.action._case.SetDlTypeActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
// Import Nicira extension
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.DstChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpThaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc1CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc2CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNsiCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4DstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpOpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc1NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc2NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc3NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc4NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNsiNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNspNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._1.grouping.NxSetNshc1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._1.grouping.NxSetNshc1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._2.grouping.NxSetNshc2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._2.grouping.NxSetNshc2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._3.grouping.NxSetNshc3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._3.grouping.NxSetNshc3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._4.grouping.NxSetNshc4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._4.grouping.NxSetNshc4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsi.grouping.NxSetNsi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsi.grouping.NxSetNsiBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsp.grouping.NxSetNsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsp.grouping.NxSetNspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMoveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.SrcChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxTunIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshc1CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshc2CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNsiCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfArpTpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfEthSrcCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsp.grouping.NxmNxNspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNsiKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nshc._1.grouping.NxmNxNshc1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsi.grouping.NxmNxNsiBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNshc1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

public class SfcOpenflowUtils {

    public static final short TABLE_INDEX_CLASSIFIER = 0;
    public static final short TABLE_INDEX_INGRESS_TRANSPORT = 1;
    public static final short TABLE_INDEX_PATH_MAPPER = 2;
    public static final short TABLE_INDEX_PATH_MAPPER_ACL = 3;
    public static final short TABLE_INDEX_NEXT_HOP = 4;
    public static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;

    public static final int FLOW_PRIORITY_TRANSPORT_INGRESS = 250;
    public static final int FLOW_PRIORITY_ARP_TRANSPORT_INGRESS = 300;
    public static final int FLOW_PRIORITY_PATH_MAPPER = 350;
    public static final int FLOW_PRIORITY_PATH_MAPPER_ACL = 450;
    public static final int FLOW_PRIORITY_NEXT_HOP = 550;
    public static final int FLOW_PRIORITY_TRANSPORT_EGRESS = 650;
    public static final int FLOW_PRIORITY_MATCH_ANY = 5;

    public static final int ETHERTYPE_IPV4 = 0x0800;
    public static final int ETHERTYPE_VLAN = 0x8100;
    public static final int ETHERTYPE_MPLS_UCAST = 0x8847;
    public static final int ETHERTYPE_ARP = 0x0806;
    public static final short IP_PROTOCOL_TCP = (short) 6;
    public static final short IP_PROTOCOL_UDP = (short) 17;
    public static final int PKT_LENGTH_IP_HEADER = 20+14; // ether + IP header
    public static final int TCP_FLAG_SYN = 0x0002;
    public static final int ARP_REQUEST = 1;
    public static final int ARP_REPLY = 2;

    private static final int COOKIE_BIGINT_INT_RADIX = 10;
    private static AtomicLong flowIdInc = new AtomicLong();

    public static FlowBuilder createFlowBuilder(
            final short table, final int priority, final BigInteger cookieValue,
            final String flowName, MatchBuilder match, InstructionsBuilder isb) {
        FlowBuilder flow = new FlowBuilder();
        String idStr = String.valueOf(flowIdInc.getAndIncrement());
        flow.setId(new FlowId(idStr));
        flow.setKey(new FlowKey(new FlowId(idStr)));
        flow.setTableId(table);
        flow.setFlowName(flowName);
        flow.setCookie(new FlowCookie(cookieValue));
        flow.setCookieMask(new FlowCookie(cookieValue));
        flow.setContainerName(null);
        flow.setStrict(false);
        flow.setMatch(match.build());
        flow.setInstructions(isb.build());
        flow.setPriority(priority);
        flow.setHardTimeout(0);
        flow.setIdleTimeout(0);
        flow.setFlags(new FlowModFlags(false, false, false, false, false));
        if (null == flow.isBarrier()) {
            flow.setBarrier(Boolean.FALSE);
        }

        return flow;
    }

    public static FlowBuilder createFlowBuilder(
            final short table, final int priority, final String flowName, MatchBuilder match, InstructionsBuilder isb) {
        return createFlowBuilder(table, priority, new BigInteger("20", COOKIE_BIGINT_INT_RADIX), flowName, match, isb);
    }


    //
    // Add Match methods
    //

    // If we call multiple ethernet match methods, the MatchBuilder
    // EthernetMatch object gets overwritten each time, when we actually
    // want to set additional fields on the existing EthernetMatch object
    private static EthernetMatch mergeEthernetMatch(MatchBuilder match, EthernetMatchBuilder ethMatchBuilder) {
        EthernetMatch ethMatch = match.getEthernetMatch();
        if(ethMatch == null) {
            return ethMatchBuilder.build();
        }

        if(ethMatch.getEthernetDestination() != null) {
            ethMatchBuilder.setEthernetDestination(ethMatch.getEthernetDestination());
        }

        if(ethMatch.getEthernetSource() != null) {
            ethMatchBuilder.setEthernetSource(ethMatch.getEthernetSource());
        }

        if(ethMatch.getEthernetType() != null) {
            ethMatchBuilder.setEthernetType(ethMatch.getEthernetType());
        }

        return ethMatchBuilder.build();
    }

    // TODO will we need mergeIpMatch() for match.setLayer3Match()

    /**
     * Add an etherType match to an existing MatchBuilder
     * @param match
     * @param etherType
     */
    public static void addMatchEtherType(MatchBuilder match, final long etherType) {
        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(etherType));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());

        match.setEthernetMatch(mergeEthernetMatch(match, ethernetMatch));
    }

    public static void addMatchSrcMac(MatchBuilder match, final String srcMac) {
        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
        ethSourceBuilder.setAddress(new MacAddress(srcMac));
        ethernetMatch.setEthernetSource(ethSourceBuilder.build());

        match.setEthernetMatch(mergeEthernetMatch(match, ethernetMatch));
    }

    public static void addMatchDstMac(MatchBuilder match, final String dstMac) {
        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
        ethDestinationBuilder.setAddress(new MacAddress(dstMac));
        ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());

        match.setEthernetMatch(mergeEthernetMatch(match, ethernetMatch));
    }

    // If we call multiple IpMatch match methods, the MatchBuilder
    // IpMatch object gets overwritten each time, when we actually
    // want to set additional fields on the existing IpMatch object
    private static IpMatch mergeIpMatch(MatchBuilder match, IpMatchBuilder ipMatchBuilder) {
        IpMatch ipMatch = match.getIpMatch();
        if(ipMatch == null) {
            return ipMatchBuilder.build();
        }

        if(ipMatch.getIpDscp() != null) {
            ipMatchBuilder.setIpDscp(ipMatch.getIpDscp());
        }

        if(ipMatch.getIpEcn() != null) {
            ipMatchBuilder.setIpEcn(ipMatch.getIpEcn());
        }

        if(ipMatch.getIpProto() != null) {
            ipMatchBuilder.setIpProto(ipMatch.getIpProto());
        }

        if(ipMatch.getIpProtocol() != null) {
            ipMatchBuilder.setIpProtocol(ipMatch.getIpProtocol());
        }

        return ipMatchBuilder.build();
    }

    public static void addMatchIpProtocol(MatchBuilder match, final short ipProtocol) {
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol(ipProtocol);

        match.setIpMatch(mergeIpMatch(match, ipMatch));
    }

    public static void addMatchTcpSyn(MatchBuilder match) {
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol(IP_PROTOCOL_TCP);
        match.setIpMatch(mergeIpMatch(match, ipMatch));

        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(TCP_FLAG_SYN);
        match.setTcpFlagMatch(tcpFlagMatch.build());
    }

    public static void addMatchDscp(MatchBuilder match, short dscpVal) {
        addMatchEtherType(match, ETHERTYPE_IPV4);

        IpMatchBuilder ipMatch = new IpMatchBuilder();
        Dscp dscp = new Dscp(dscpVal);
        ipMatch.setIpDscp(dscp);

        match.setIpMatch(mergeIpMatch(match, ipMatch));
    }

    public static void addMatchDstUdpPort(MatchBuilder match, PortNumber port) {
        UdpMatchBuilder udpMatch = new UdpMatchBuilder(); //UDP
        udpMatch.setUdpDestinationPort(port);

        match.setLayer4Match(udpMatch.build());
    }

    public static void addMatchMplsLabel(MatchBuilder match, long label) {
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType((long) ETHERTYPE_MPLS_UCAST));

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        eth.setEthernetType(ethTypeBuilder.build());
        match.setEthernetMatch(eth.build());

        ProtocolMatchFieldsBuilder protomatch = new ProtocolMatchFieldsBuilder();
        protomatch.setMplsLabel(label);
        match.setProtocolMatchFields(protomatch.build());
    }

    public static void addMatchVlan(MatchBuilder match, int vlan) {
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        VlanId vlanId = new VlanId(vlan);
        vlanIdBuilder.setVlanId(vlanId);
        vlanIdBuilder.setVlanIdPresent(true);
        vlanMatchBuilder.setVlanId(vlanIdBuilder.build());

        match.setVlanMatch(vlanMatchBuilder.build());
    }

    public static void addMatchArpRequest(MatchBuilder match){
        ArpMatchBuilder arpmatch = new ArpMatchBuilder();
        arpmatch.setArpOp(ARP_REQUEST);
        match.setLayer3Match(arpmatch.build());
    }

    // If we call multiple Layer3 match methods, the MatchBuilder
    // Ipv4Match object gets overwritten each time, when we actually
    // want to set additional fields on the existing Ipv4Match object
    private static Ipv4Match mergeLayer3Match(MatchBuilder match, Ipv4MatchBuilder ipMatchBuilder) {
        Ipv4Match ipv4Match = (Ipv4Match) match.getLayer3Match();
        if(ipv4Match == null) {
            return ipMatchBuilder.build();
        }

        if(ipv4Match.getIpv4Destination() != null) {
            ipMatchBuilder.setIpv4Destination(ipv4Match.getIpv4Destination());
        }

        if(ipv4Match.getIpv4Source() != null) {
            ipMatchBuilder.setIpv4Source(ipv4Match.getIpv4Source());
        }

        return ipMatchBuilder.build();
    }

    public static void addMatchSrcIpv4(MatchBuilder match, String srcIpStr, int netmask) {
        addMatchSrcIpv4(match, new Ipv4Prefix(srcIpStr + "/" + String.valueOf(netmask)));
    }

    public static void addMatchSrcIpv4(MatchBuilder match, Ipv4Prefix srcIp) {
        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Source(srcIp);
        match.setLayer3Match(mergeLayer3Match(match, ipv4match));
    }

    public static void addMatchDstIpv4(MatchBuilder match, String dstIpStr, int netmask) {
        addMatchDstIpv4(match, new Ipv4Prefix(dstIpStr + "/" + String.valueOf(netmask)));
    }

    public static void addMatchDstIpv4(MatchBuilder match, Ipv4Prefix dstIp) {
        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Destination(dstIp);
        match.setLayer3Match(mergeLayer3Match(match, ipv4match));
    }

    public static void addMatchMetada(MatchBuilder match, BigInteger metadataValue, BigInteger metadataMask) {
        MetadataBuilder metadata = new MetadataBuilder();
        metadata.setMetadata(metadataValue);
        metadata.setMetadataMask(metadataMask);

        match.setMetadata(metadata.build());
    }

    private static void addExtension (MatchBuilder match, Class<? extends ExtensionKey> extensionKey, NxAugMatchNodesNodeTableFlow am) {
        GeneralAugMatchNodesNodeTableFlow existingAugmentations = match.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
        List<ExtensionList> extensions = null;
        if (existingAugmentations != null ) {
            extensions = existingAugmentations.getExtensionList();
        }
        if (extensions == null) {
            extensions = Lists.newArrayList();
        }

        extensions.add(new ExtensionListBuilder()
                           .setExtensionKey(extensionKey)
                           .setExtension(new ExtensionBuilder()
                           .addAugmentation(NxAugMatchNodesNodeTableFlow.class, am)
                           .build())
                           .build());

        GeneralAugMatchNodesNodeTableFlow m = new GeneralAugMatchNodesNodeTableFlowBuilder()
        .setExtensionList(extensions)
        .build();
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addMatchNshNsp(MatchBuilder match, long nsp) {
        NxAugMatchNodesNodeTableFlow am =
            new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxNsp(new NxmNxNspBuilder()
                    .setValue(nsp)
                    .build())
                .build();
        addExtension(match, NxmNxNspKey.class, am);
    }

    public static void addMatchNshNsi(MatchBuilder match, short nsi) {
        NxAugMatchNodesNodeTableFlow am =
            new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxNsi(new NxmNxNsiBuilder()
                    .setNsi(nsi)
                    .build())
                .build();
        addExtension(match, NxmNxNsiKey.class, am);
    }

    public static void addMatchNshNsc1(MatchBuilder match, long nsc) {
        NxAugMatchNodesNodeTableFlow am =
            new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxNshc1(new NxmNxNshc1Builder()
                    .setValue(nsc)
                    .build())
                .build();
        addExtension(match, NxmNxNshc1Key.class, am);
    }


    //
    // Create Action methods
    //

    // Internal method to create an ActionBuilder
    private static ActionBuilder createActionBuilder(int order) {
        ActionBuilder ab = new ActionBuilder();
        ab.setOrder(order);
        ab.setKey(new ActionKey(order));

        return ab;
    }

    public static GoToTableBuilder createActionGotoTable(final short toTable) {
        GoToTableBuilder gotoTb = new GoToTableBuilder();
        gotoTb.setTableId(toTable);

        return gotoTb;
    }

    public static GroupActionBuilder createGroupAction(final long groupId) {
        GroupActionBuilder gab = new GroupActionBuilder();
        gab.setGroupId(groupId);

        return gab;
    }

    public static Action createActionOutPort(final int portUri, final int order) {
        return createActionOutPort(String.valueOf(portUri), order);
    }

    public static Action createActionOutPort(final String portUri, final int order) {
        OutputActionBuilder output = new OutputActionBuilder();
        Uri value = new Uri(portUri);
        output.setOutputNodeConnector(value);
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());

        return ab.build();
    }

    public static Action createActionSetEtherType(final long etherType, final int order) {
        SetDlTypeActionBuilder setDlTypeActionBuilder = new SetDlTypeActionBuilder();
        setDlTypeActionBuilder.setDlType(new EtherType(etherType));

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new SetDlTypeActionCaseBuilder().setSetDlTypeAction(setDlTypeActionBuilder.build()).build());

        return ab.build();
    }

    public static Action createActionWriteDscp(short dscpVal, final int order) {
        IpMatchBuilder ipMatch = new IpMatchBuilder();
        Dscp dscp = new Dscp(dscpVal);
        ipMatch.setIpDscp(dscp);

        SetFieldCaseBuilder setFieldCase = new SetFieldCaseBuilder();
        setFieldCase.setSetField(
                new SetFieldBuilder().setIpMatch(ipMatch.build())
                .build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(setFieldCase.build());

        return ab.build();
    }

    public static Action createActionPktIn(final int pktLength, final int order) {
        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(new Integer(0xffff));
        Uri controllerPort = new Uri(OutputPortValues.CONTROLLER.toString());
        output.setOutputNodeConnector(controllerPort);

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());

        return ab.build();
    }

    public static Action createActionSetDlSrc(String srcMac, int order) {
        EthernetSourceBuilder ethSrc = new EthernetSourceBuilder();
        ethSrc.setAddress(new MacAddress(srcMac));

        EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
        ethMatchBuilder.setEthernetSource(ethSrc.build());

        SetFieldCaseBuilder setFieldCase = new SetFieldCaseBuilder();
        setFieldCase.setSetField(
                new SetFieldBuilder().setEthernetMatch(ethMatchBuilder.build())
                .build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(setFieldCase.build());

        return ab.build();
    }

    public static Action createActionSetDlDst(String dstMac, int order) {
        EthernetDestinationBuilder ethDst = new EthernetDestinationBuilder();
        ethDst.setAddress(new MacAddress(dstMac));

        EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
        ethMatchBuilder.setEthernetDestination(ethDst.build());

        SetFieldCaseBuilder setFieldCase = new SetFieldCaseBuilder();
        setFieldCase.setSetField(
                new SetFieldBuilder().setEthernetMatch(ethMatchBuilder.build())
                .build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(setFieldCase.build());

        return ab.build();
    }

    public static Action createActionSetNwDst(String ipStr, int netmask, int order) {
        Ipv4Prefix prefixdst = new Ipv4Prefix(ipStr + "/" + String.valueOf(netmask));
        Ipv4Builder ipdst = new Ipv4Builder();
        ipdst.setIpv4Address(prefixdst);

        SetNwDstActionBuilder nwDstBuilder = new SetNwDstActionBuilder();
        nwDstBuilder.setAddress(ipdst.build());
        SetNwDstActionCaseBuilder nwDstCaseBuilder = new SetNwDstActionCaseBuilder();
        nwDstCaseBuilder.setSetNwDstAction(nwDstBuilder.build());
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nwDstCaseBuilder.build());

        return ab.build();
    }

    public static Action createActionPopVlan(int order) {
        PopVlanActionCaseBuilder popVlanBuilder = new PopVlanActionCaseBuilder();
        popVlanBuilder.setPopVlanAction(
                new PopVlanActionBuilder().build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(popVlanBuilder.build());

        return ab.build();
    }

    public static Action createActionPushVlan(int order) {
        PushVlanActionBuilder pushVlanBuilder = new PushVlanActionBuilder();
        pushVlanBuilder.setEthernetType(ETHERTYPE_VLAN);

        PushVlanActionCaseBuilder pushVlanActionCase = new PushVlanActionCaseBuilder();
        pushVlanActionCase.setPushVlanAction(pushVlanBuilder.build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(pushVlanActionCase.build());

        return ab.build();
    }

    public static Action createActionSetVlanId(int vlan, int order) {
        VlanIdBuilder vlanBuilder = new VlanIdBuilder();
        vlanBuilder.setVlanId(new VlanId(vlan));
        vlanBuilder.setVlanIdPresent(true);

        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        vlanMatchBuilder.setVlanId(vlanBuilder.build());

        SetFieldCaseBuilder setFieldCase = new SetFieldCaseBuilder();
        setFieldCase.setSetField(
                new SetFieldBuilder().setVlanMatch(vlanMatchBuilder.build())
                .build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(setFieldCase.build());

        return ab.build();
    }

    public static Action createActionPushMpls(int order) {
        PushMplsActionBuilder push = new PushMplsActionBuilder();
        push.setEthernetType(new Integer(ETHERTYPE_MPLS_UCAST));

        PushMplsActionCaseBuilder pushMplsCase = new PushMplsActionCaseBuilder();
        pushMplsCase.setPushMplsAction(push.build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(pushMplsCase.build());

        return ab.build();
    }

    public static Action createActionSetMplsLabel(long label, int order) {
        ProtocolMatchFieldsBuilder protomatch = new ProtocolMatchFieldsBuilder();
        protomatch.setMplsLabel(label);

        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();
        setFieldBuilder.setProtocolMatchFields(protomatch.build());

        SetFieldCaseBuilder setFieldCase = new SetFieldCaseBuilder();
        setFieldCase.setSetField(setFieldBuilder.build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(setFieldCase.build());

        return ab.build();
    }

    public static Action createActionPopMpls(int order) {
        PopMplsActionBuilder popMplsActionBuilder = new PopMplsActionBuilder();
        // TODO, is this ethertype correct?
        popMplsActionBuilder.setEthernetType(0XB);

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new PopMplsActionCaseBuilder().setPopMplsAction(popMplsActionBuilder.build()).build());

        return ab.build();
    }

    public static Action createActionDropPacket(int order) {
        DropActionBuilder dab = new DropActionBuilder();
        DropAction dropAction = dab.build();
        DropActionCaseBuilder dac = new DropActionCaseBuilder();
        dac.setDropAction(dropAction);

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(dac.build());

        return ab.build();
    }

    public static Action createActionNxSetNsp(Long nsp, int order) {
        NxSetNspBuilder builder = new NxSetNspBuilder();
        if (nsp != null) {
            builder.setNsp(nsp);
        }
        NxSetNsp r = builder.build();
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new NxActionSetNspNodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNsp(r).build());

        return ab.build();
    }

    public static Action createActionNxSetNsi(Short nsi, int order) {
        NxSetNsiBuilder builder = new NxSetNsiBuilder();
        if (nsi != null) {
            builder.setNsi(nsi);
        }
        NxSetNsi r = builder.build();
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new NxActionSetNsiNodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNsi(r).build());

        return ab.build();
    }

    public static Action createActionNxSetNshc1(Long c1, int order) {
        NxSetNshc1Builder builder = new NxSetNshc1Builder();
        if (c1 != null) {
            builder.setNshc(c1);
        }
        NxSetNshc1 r = builder.build();
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new NxActionSetNshc1NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc1(r).build());

        return ab.build();
    }

    public static Action createActionNxSetNshc2(Long c2, int order) {
        NxSetNshc2Builder builder = new NxSetNshc2Builder();
        if (c2 != null) {
            builder.setNshc(c2);
        }
        NxSetNshc2 r = builder.build();
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new NxActionSetNshc2NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc2(r).build());

        return ab.build();
    }

    public static Action createActionNxSetNshc3(Long c3, int order) {
        NxSetNshc3Builder builder = new NxSetNshc3Builder();
        if (c3 != null) {
            builder.setNshc(c3);
        }
        NxSetNshc3 r = builder.build();
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new NxActionSetNshc3NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc3(r).build());

        return ab.build();
    }

    public static Action createActionNxSetNshc4(Long c4, int order) {
        NxSetNshc4Builder builder = new NxSetNshc4Builder();
        if (c4 != null) {
            builder.setNshc(c4);
        }
        NxSetNshc4 r = builder.build();
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new NxActionSetNshc4NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc4(r).build());

        return ab.build();
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nxLoadRegAction(
            DstChoice dstChoice, BigInteger value, int endOffset, boolean groupBucket) {
        NxRegLoad regLoad = new NxRegLoadBuilder()
            .setDst(new DstBuilder()
            .setDstChoice(dstChoice)
            .setStart(Integer.valueOf(0))
            .setEnd(Integer.valueOf(endOffset))
            .build())
        .setValue(value)
        .build();

        if (groupBucket) {
            return new NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder()
            .setNxRegLoad(regLoad).build();
        } else {
            return new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder()
            .setNxRegLoad(regLoad).build();
        }
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action
    nxMoveRegAction(
            SrcChoice srcChoice, DstChoice dstChoice, int endOffset, boolean groupBucket) {
        NxRegMove r = new NxRegMoveBuilder()
            .setSrc(new SrcBuilder()
                .setSrcChoice(srcChoice)
                .setStart(Integer.valueOf(0))
                .setEnd(Integer.valueOf(endOffset))
                .build())
            .setDst(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder()
                .setDstChoice(dstChoice)
                .setStart(Integer.valueOf(0))
                .setEnd(Integer.valueOf(endOffset))
                .build())
            .build();

        if (groupBucket) {
            return new NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder()
            .setNxRegMove(r).build();
        } else {
            return new NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder()
            .setNxRegMove(r).build();
        }
    }

    // Used by NSH to set the destination tunnel IP when forwarding NSH packets
    public static Action createActionNxSetTunIpv4Dst(String ipStr, int order) {
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(ipStr));
        long ipl = ip & 0xffffffffL;
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
                nxLoadRegAction(new DstNxTunIpv4DstCaseBuilder()
                                    .setNxTunIpv4Dst(Boolean.TRUE).build(),
                               BigInteger.valueOf(ipl),
                               31,
                               false));

        return ab.build();
    }

    // Used by NSH to move the VxLAN Network ID (VNID) from the source to the dest tunnel
    public static Action createActionNxMoveTunIdRegister(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(),
                new DstNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(),
                31,
                false));

        return ab.build();
    }

    public static Action createActionNxMoveNsc1(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxNshc1CaseBuilder().setNxNshc1Dst(Boolean.TRUE).build(),
                new DstNxNshc1CaseBuilder().setNxNshc1Dst(Boolean.TRUE).build(),
                31,
                false));

        return ab.build();
    }

    public static Action createActionNxMoveNsc2(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxNshc2CaseBuilder().setNxNshc2Dst(Boolean.TRUE).build(),
                new DstNxNshc2CaseBuilder().setNxNshc2Dst(Boolean.TRUE).build(),
                31,
                false));

        return ab.build();
    }

    public static Action createActionNxMoveNsi(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxNsiCaseBuilder().setNxNsiDst(Boolean.TRUE).build(),
                new DstNxNsiCaseBuilder().setNxNsiDst(Boolean.TRUE).build(),
                7,  // Service Index is 8 bits, moving bits 0-7
                false));

        return ab.build();
    }

    public static Action createActionNxMoveNsp(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxNspCaseBuilder().setNxNspDst(Boolean.TRUE).build(),
                new DstNxNspCaseBuilder().setNxNspDst(Boolean.TRUE).build(),
                23,  // Service Index is 24 bits, moving bits 0-23
                false));

        return ab.build();
    }

    // Used by NSH to move one of the NSH Context registers (NSC) to
    // the Tunnel Id (VNID) Register. This is for the RSP NSH egress tunnel.
    // GBP will set the Tunnel ID (VNID) in NSC2 and pass it along the
    // chain, and in the last SFF, we will use it to set the VNID
    // This will only work with this patch: https://git.opendaylight.org/gerrit/#/c/19478
    public static Action createActionNxMoveNsc2ToTunIdRegister(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxNshc2CaseBuilder().setNxNshc2Dst(Boolean.TRUE).build(),
                new DstNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(),
                31,
                false));

        return ab.build();
    }

    // This will only work with this patch: https://git.opendaylight.org/gerrit/#/c/19478
    public static Action createActionNxMoveNsc1ToTunIpv4DstRegister(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxNshc1CaseBuilder().setNxNshc1Dst(Boolean.TRUE).build(),
                new DstNxTunIpv4DstCaseBuilder().setNxTunIpv4Dst(Boolean.TRUE).build(),
                31,
                false));

        return ab.build();
    }

    public static Action createActionNxLoadArpOpAction(int value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
                nxLoadRegAction(
                        new DstOfArpOpCaseBuilder().setOfArpOp(Boolean.TRUE).build(),
                        BigInteger.valueOf(value),
                        15,
                        false));

        return ab.build();
    }

    // Used for ARP to move the Source HW Address (Sha) to the Target HW address (Tha)
    public static Action createActionNxMoveArpShaToArpThaAction(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
                nxMoveRegAction(
                        new SrcNxArpShaCaseBuilder().setNxArpSha(Boolean.TRUE).build(),
                        new DstNxArpThaCaseBuilder().setNxArpTha(Boolean.TRUE).build(),
                        47,
                        false));

        return ab.build();
    }

    public static Action createActionNxMoveEthSrcToEthDstAction(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
                nxMoveRegAction(
                        new SrcOfEthSrcCaseBuilder().setOfEthSrc(Boolean.TRUE).build(),
                        new DstOfEthDstCaseBuilder().setOfEthDst(Boolean.TRUE).build(),
                        47,
                        false));

        return ab.build();
    }

    static byte[] bytesFromHexString(String values) {
        String target = "";
        if (values != null) {
            target = values;
        }
        String[] octets = target.split(":");

        byte[] ret = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
        return ret;
    }

    public static Action createActionNxLoadArpShaAction(String mac, int order){
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
                nxLoadRegAction(
                        new DstNxArpShaCaseBuilder().setNxArpSha(Boolean.TRUE).build(),
                        new BigInteger(1, bytesFromHexString(new MacAddress(mac).getValue())),
                        47,
                        false));
        return ab.build();
    }

    public static Action createActionNxMoveArpTpaToRegAction(int order){
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
                nxMoveRegAction(
                        new SrcOfArpTpaCaseBuilder().setOfArpTpa(Boolean.TRUE).build(),
                        new DstNxRegCaseBuilder().setNxReg(NxmNxReg0.class).build(),
                        31,
                        false));

        return ab.build();
    }


    public static Action createActionNxMoveArpSpaToArpTpaAction(int order){
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
                nxMoveRegAction(
                        new SrcOfArpSpaCaseBuilder().setOfArpSpa(Boolean.TRUE).build(),
                        new DstOfArpTpaCaseBuilder().setOfArpTpa(Boolean.TRUE).build(),
                        31,
                        false));
        return ab.build();
    }

    public static Action createActionNxMoveRegToArpSpaAction(int order){
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
                nxMoveRegAction(
                        new SrcNxRegCaseBuilder().setNxReg(NxmNxReg0.class).build(),
                        new DstOfArpSpaCaseBuilder().setOfArpSpa(Boolean.TRUE).build(),
                        31,
                        false));
        return ab.build();
    }

    public static WriteMetadataCase createInstructionMetadata(int order, BigInteger metadataVal, BigInteger metadataMask) {
        WriteMetadataBuilder wmb = new WriteMetadataBuilder();
        wmb.setMetadata(metadataVal);
        wmb.setMetadataMask(metadataMask);
        WriteMetadataCaseBuilder wmcb = new WriteMetadataCaseBuilder().setWriteMetadata(wmb.build());

        return wmcb.build();
    }

    public static InstructionsBuilder createInstructionsBuilder(InstructionBuilder ib) {
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        isb.setInstruction(instructions);

        return isb;
    }

    public static InstructionsBuilder createInstructionsBuilder(InstructionBuilder... ibs) {
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();

        for (InstructionBuilder ib : ibs) {
            instructions.add(ib.build());
        }

        isb.setInstruction(instructions);
        return isb;
    }

    public static InstructionBuilder createActionsInstructionBuilder(Action... actions) {
        InstructionBuilder actionsIb = new InstructionBuilder();
        ArrayList<Action> alist = new ArrayList<>();
        ApplyActionsBuilder aab = new ApplyActionsBuilder();

        for (Action action : actions) {
            alist.add(action);
        }
        aab.setAction(alist);
        actionsIb.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        actionsIb.setKey(new InstructionKey(0));
        actionsIb.setOrder(0);
        return actionsIb;
    }

    // Only configure OpenFlow Capable SFFs
    public static boolean isSffOpenFlowCapable(final String sffName) {
        InstanceIdentifier<FlowCapableNode> nodeInstancIdentifier =
                InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, new NodeKey(new NodeId(sffName)))
                    .augmentation(FlowCapableNode.class)
                    .build();

        // If its not a Flow Capable Node, this should return NULL
        // TODO need to verify this, once SFC can connect to simple OVS nodes that arent flow capable
        FlowCapableNode node = SfcDataStoreAPI.readTransactionAPI(nodeInstancIdentifier, LogicalDatastoreType.OPERATIONAL);
        if(node != null) {
            return true;
        }
        return false;
    }
}
