/*
 * Copyright (c) 2014 ConteXtream Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.util.openflow;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.openflowplugin.extension.api.path.ActionPath;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagsMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.action.rev140421.action.container.action.choice.ActionResubmitBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.action.rev140421.ofj.nx.action.resubmit.grouping.NxActionResubmitBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg0Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.reg.grouping.NxmNxRegBuilder;

// Import Nicira extension
import org.opendaylight.openflowplugin.extension.vendor.nicira.convertor.action.ActionUtil;
import org.opendaylight.openflowplugin.extension.vendor.nicira.convertor.action.ResubmitConvertor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.DstChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpThaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc1CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc2CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc3CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNsiCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4DstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpOpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxEncapEthSrcCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxEncapEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshMdtypeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshNpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunGpeNpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionPushNshNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionPopNshNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.push.nsh.grouping.NxPushNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.push.nsh.grouping.NxPushNshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.pop.nsh.grouping.NxPopNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.pop.nsh.grouping.NxPopNshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.resubmit.grouping.NxResubmit;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.resubmit.grouping.NxResubmitBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMoveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.SrcChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshMdtypeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshNpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxTunGpeNpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxTunIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxTunIpv4DstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshc1CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshc2CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshc3CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshc4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNsiCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfArpTpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfEthSrcCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsp.grouping.NxmNxNspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNsiKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nshc._1.grouping.NxmNxNshc1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsi.grouping.NxmNxNsiBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNshc1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxEncapEthTypeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.encap.eth.type.grouping.NxmNxEncapEthTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxEncapEthSrcKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.encap.eth.src.grouping.NxmNxEncapEthSrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxEncapEthDstKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.encap.eth.dst.grouping.NxmNxEncapEthDstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNshMdtypeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsh.mdtype.grouping.NxmNxNshMdtypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNshNpKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsh.np.grouping.NxmNxNshNpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxTunGpeNpKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.tun.gpe.np.grouping.NxmNxTunGpeNpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;


// Import Nicira extension

public class SfcOpenflowUtils {
    public static final int ETHERTYPE_IPV4 = 0x0800;
    public static final int ETHERTYPE_VLAN = 0x8100;
    public static final int ETHERTYPE_IPV6 = 0x86dd;
    public static final int ETHERTYPE_MPLS_UCAST = 0x8847;
    public static final int ETHERTYPE_MPLS_MCAST = 0x8848;
    public static final int ETHERTYPE_ARP = 0x0806;
    public static final int ETHERTYPE_NSH = 0x894f;
    public static final short IP_PROTOCOL_ICMP = (short) 1;
    public static final short IP_PROTOCOL_TCP = (short) 6;
    public static final short IP_PROTOCOL_UDP = (short) 17;
    public static final short IP_PROTOCOL_SCTP = (short) 132;
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

        TcpFlagsMatchBuilder tcpFlagMatch = new TcpFlagsMatchBuilder();
        tcpFlagMatch.setTcpFlags(TCP_FLAG_SYN);
        match.setTcpFlagsMatch(tcpFlagMatch.build());
    }

    public static void addMatchDscp(MatchBuilder match, short dscpVal) {
        addMatchEtherType(match, ETHERTYPE_IPV4);

        IpMatchBuilder ipMatch = new IpMatchBuilder();
        Dscp dscp = new Dscp(dscpVal);
        ipMatch.setIpDscp(dscp);

        match.setIpMatch(mergeIpMatch(match, ipMatch));
    }

    public static void addMatchSrcUdpPort(MatchBuilder match, int portNum) {
        PortNumber port = new PortNumber(portNum);
        UdpMatchBuilder udpMatch = new UdpMatchBuilder();
        udpMatch.setUdpSourcePort(port);

        match.setLayer4Match(udpMatch.build());
    }

    public static void addMatchDstUdpPort(MatchBuilder match, int portNum) {
        PortNumber port = new PortNumber(portNum);
        UdpMatchBuilder udpMatch = new UdpMatchBuilder();
        udpMatch.setUdpDestinationPort(port);

        match.setLayer4Match(udpMatch.build());
    }

    public static void addMatchSrcTcpPort(MatchBuilder match, int portNum) {
        PortNumber port = new PortNumber(portNum);
        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
        tcpMatch.setTcpSourcePort(port);

        match.setLayer4Match(tcpMatch.build());
    }

    public static void addMatchDstTcpPort(MatchBuilder match, int portNum) {
        PortNumber port = new PortNumber(portNum);
        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
        tcpMatch.setTcpDestinationPort(port);

        match.setLayer4Match(tcpMatch.build());
    }

    public static void addMatchSrcSctpPort(MatchBuilder match, int portNum) {
        PortNumber port = new PortNumber(portNum);
        SctpMatchBuilder sctpMatch = new SctpMatchBuilder();
        sctpMatch.setSctpSourcePort(port);

        match.setLayer4Match(sctpMatch.build());
    }

    public static void addMatchDstSctpPort(MatchBuilder match, int portNum) {
        PortNumber port = new PortNumber(portNum);
        SctpMatchBuilder sctpMatch = new SctpMatchBuilder();
        sctpMatch.setSctpDestinationPort(port);

        match.setLayer4Match(sctpMatch.build());
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

    public static void addMatchArpRequestAndTpa(MatchBuilder match, String requestedIp){
        ArpMatchBuilder arpmatch = new ArpMatchBuilder();
        arpmatch.setArpOp(ARP_REQUEST);
        arpmatch.setArpTargetTransportAddress(new Ipv4Prefix(requestedIp + "/32"));
        match.setLayer3Match(arpmatch.build());
    }

    // If we call multiple Layer3 match methods, the MatchBuilder
    // Ipv4Match object gets overwritten each time, when we actually
    // want to set additional fields on the existing Ipv4Match object
    private static Ipv4Match mergeIpv4Match(MatchBuilder match, Ipv4MatchBuilder ipMatchBuilder) {
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
        match.setLayer3Match(mergeIpv4Match(match, ipv4match));
    }

    public static void addMatchDstIpv4(MatchBuilder match, String dstIpStr, int netmask) {
        addMatchDstIpv4(match, new Ipv4Prefix(dstIpStr + "/" + String.valueOf(netmask)));
    }

    public static void addMatchDstIpv4(MatchBuilder match, Ipv4Prefix dstIp) {
        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Destination(dstIp);
        match.setLayer3Match(mergeIpv4Match(match, ipv4match));
    }

    // If we call multiple Layer3 match methods, the MatchBuilder
    // Ipv6Match object gets overwritten each time, when we actually
    // want to set additional fields on the existing Ipv6Match object
    private static Ipv6Match mergeIpv6Match(MatchBuilder match, Ipv6MatchBuilder ipMatchBuilder) {
        Ipv6Match ipv6Match = (Ipv6Match) match.getLayer3Match();
        if(ipv6Match == null) {
            return ipMatchBuilder.build();
        }

        if(ipv6Match.getIpv6Destination() != null) {
            ipMatchBuilder.setIpv6Destination(ipv6Match.getIpv6Destination());
        }

        if(ipv6Match.getIpv6Source() != null) {
            ipMatchBuilder.setIpv6Source(ipv6Match.getIpv6Source());
        }

        return ipMatchBuilder.build();
    }

    public static void addMatchSrcIpv6(MatchBuilder match, String srcIpStr, int netmask) {
        addMatchSrcIpv6(match, new Ipv6Prefix(srcIpStr + "/" + String.valueOf(netmask)));
    }

    public static void addMatchSrcIpv6(MatchBuilder match, Ipv6Prefix srcIp) {
        Ipv6MatchBuilder ipv6match = new Ipv6MatchBuilder();
        ipv6match.setIpv6Source(srcIp);
        match.setLayer3Match(mergeIpv6Match(match, ipv6match));
    }

    public static void addMatchDstIpv6(MatchBuilder match, String dstIpStr, int netmask) {
        addMatchDstIpv6(match, new Ipv6Prefix(dstIpStr + "/" + String.valueOf(netmask)));
    }

    public static void addMatchDstIpv6(MatchBuilder match, Ipv6Prefix dstIp) {
        Ipv6MatchBuilder ipv6match = new Ipv6MatchBuilder();
        ipv6match.setIpv6Destination(dstIp);
        match.setLayer3Match(mergeIpv6Match(match, ipv6match));
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

    public static void addMatchInPort(MatchBuilder match, NodeId nodeId, long inPort) {
        match.setInPort(new NodeConnectorId(nodeId + ":" + inPort));
    }

    public static void addMatchInPort(MatchBuilder match, NodeConnectorId nodeConnId) {
        match.setInPort(nodeConnId);
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

    public static Action createActionResubmitTable(final short toTable, int order) {
        NxActionResubmitBuilder resubmit = new NxActionResubmitBuilder();
        resubmit.setTable(toTable);

        ActionResubmitBuilder actionResubmitBuilder = new ActionResubmitBuilder();
        actionResubmitBuilder.setNxActionResubmit(resubmit.build());

        ResubmitConvertor convertor = new ResubmitConvertor();
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(convertor.convert(
                ActionUtil.createAction(actionResubmitBuilder.build()),
                ActionPath.NODES_NODE_TABLE_FLOW_INSTRUCTIONS_INSTRUCTION_WRITEACTIONSCASE_WRITEACTIONS_ACTION_ACTION_EXTENSIONLIST_EXTENSION));

        return ab.build();
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

    public static Action createActionDecNwTtl(int order) {
        DecNwTtlBuilder builder = new DecNwTtlBuilder();
        DecNwTtlCaseBuilder caseBuilder = new DecNwTtlCaseBuilder();
        caseBuilder.setDecNwTtl(builder.build());

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(caseBuilder.build());

        return ab.build();
    }

    public static Action createActionNxPushNsh(int order) {
        NxPushNshBuilder builder = new NxPushNshBuilder();
        NxPushNsh r = builder.build();

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new NxActionPushNshNodesNodeTableFlowApplyActionsCaseBuilder().setNxPushNsh(r).build());

        return ab.build();
    }

    public static Action createActionNxPopNsh(int order) {
        NxPopNshBuilder builder = new NxPopNshBuilder();
        NxPopNsh r = builder.build();

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new NxActionPopNshNodesNodeTableFlowApplyActionsCaseBuilder().setNxPopNsh(r).build());

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

    public static Action createActionNxMoveTunIpv4Dst(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxMoveRegAction(
                new SrcNxTunIpv4DstCaseBuilder().setNxTunIpv4Dst(Boolean.TRUE).build(),
                new DstNxTunIpv4DstCaseBuilder().setNxTunIpv4Dst(Boolean.TRUE).build(),
                31,
                false
            )
        );

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

    /**
     * create action to move NSH C3
     * @param order
     */
    public static Action createActionNxMoveNsc3(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxNshc3CaseBuilder().setNxNshc3Dst(Boolean.TRUE).build(),
                new DstNxNshc3CaseBuilder().setNxNshc3Dst(Boolean.TRUE).build(),
                31,
                false));

        return ab.build();
    }

    /**
     * create action to move NSH C4
     * @param order
     */
    public static Action createActionNxMoveNsc4(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxNshc4CaseBuilder().setNxNshc4Dst(Boolean.TRUE).build(),
                new DstNxNshc4CaseBuilder().setNxNshc4Dst(Boolean.TRUE).build(),
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

    public static Action createActionNxMoveTunGpeNp(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxTunGpeNpCaseBuilder().setNxTunGpeNp(Boolean.TRUE).build(),
                new DstNxTunGpeNpCaseBuilder().setNxTunGpeNp(Boolean.TRUE).build(),
                7,
                false));

        return ab.build();
    }

    public static Action createActionNxMoveNshMdtype(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxNshMdtypeCaseBuilder().setNxNshMdtype(Boolean.TRUE).build(),
                new DstNxNshMdtypeCaseBuilder().setNxNshMdtype(Boolean.TRUE).build(),
                7,
                false));

        return ab.build();
    }

    public static Action createActionNxMoveNshNp(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(nxMoveRegAction(
                new SrcNxNshNpCaseBuilder().setNxNshNp(Boolean.TRUE).build(),
                new DstNxNshNpCaseBuilder().setNxNshNp(Boolean.TRUE).build(),
                7,
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

    public static Action createActionNxLoadNshMdtype(short value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxNshMdtypeCaseBuilder().setNxNshMdtype(Boolean.TRUE).build(),
                BigInteger.valueOf(value),
                7,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadNshNp(short value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxNshNpCaseBuilder().setNxNshNp(Boolean.TRUE).build(),
                BigInteger.valueOf(value),
                7,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadTunGpeNp(short value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxTunGpeNpCaseBuilder().setNxTunGpeNp(Boolean.TRUE).build(),
                BigInteger.valueOf(value),
                7,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadNsp(int value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxNspCaseBuilder().setNxNspDst(Boolean.TRUE).build(),
                BigInteger.valueOf(value),
                23,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadNsi(short value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxNsiCaseBuilder().setNxNsiDst(Boolean.TRUE).build(),
                BigInteger.valueOf(value),
                7,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadNshc1(long value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxNshc1CaseBuilder().setNxNshc1Dst(Boolean.TRUE).build(),
                BigInteger.valueOf(value),
                31,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadNshc2(long value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxNshc2CaseBuilder().setNxNshc2Dst(Boolean.TRUE).build(),
                BigInteger.valueOf(value),
                31,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadNshc3(long value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxNshc3CaseBuilder().setNxNshc3Dst(Boolean.TRUE).build(),
                BigInteger.valueOf(value),
                31,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadNshc4(long value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxNshc4CaseBuilder().setNxNshc4Dst(Boolean.TRUE).build(),
                BigInteger.valueOf(value),
                31,
                false
            )
        );

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

    static byte[] IpToBytes(String ip) {
        String[] ipStr = ip.split("\\.");
        byte[] bytes = new byte[ipStr.length];
        for (int i = 0; i < ipStr.length; i++) {
            bytes[i] = Integer.valueOf(ipStr[i], 10).byteValue();
        }
        return bytes;
    }

    public static Action createActionNxLoadEncapEthSrc(String value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxEncapEthSrcCaseBuilder().setNxEncapEthSrc(Boolean.TRUE).build(),
                new BigInteger(1, bytesFromHexString(new MacAddress(value).getValue())),
                47,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadEncapEthDst(String mac, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxEncapEthDstCaseBuilder().setNxEncapEthDst(Boolean.TRUE).build(),
                new BigInteger(1, bytesFromHexString(new MacAddress(mac).getValue())),
                47,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadTunId(long value, int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(),
                BigInteger.valueOf(value),
                63,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxLoadTunIpv4Dst(String ipStr, int order) {
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(ipStr));
        long ipl = ip & 0xffffffffL;
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxTunIpv4DstCaseBuilder().setNxTunIpv4Dst(Boolean.TRUE).build(),
                BigInteger.valueOf(ipl),
                31,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxResubmit(int inPort, short table, int order) {
        NxResubmitBuilder nxResubmitBuilder = new NxResubmitBuilder();
        nxResubmitBuilder.setTable(Short.valueOf(table));
        if (inPort >= 0) {
            nxResubmitBuilder.setInPort(Integer.valueOf(inPort));
        }
        NxResubmit nxResubmit = nxResubmitBuilder.build();

        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(new NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder().setNxResubmit(nxResubmit).build());
        return ab.build();
    }

    public static Action createActionNxMoveEthSrcToEncapEthSrc(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxMoveRegAction(
                new SrcOfEthSrcCaseBuilder().setOfEthSrc(Boolean.TRUE).build(),
                new DstNxEncapEthSrcCaseBuilder().setNxEncapEthSrc(Boolean.TRUE).build(),
                47,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxMoveEthDstToEncapEthDst(int order) {
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxMoveRegAction(
                new SrcOfEthDstCaseBuilder().setOfEthDst(Boolean.TRUE).build(),
                new DstNxEncapEthDstCaseBuilder().setNxEncapEthDst(Boolean.TRUE).build(),
                47,
                false
            )
        );

        return ab.build();
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

    public static Action createActionNxLoadArpSpaAction(String ip, int order){
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstOfArpSpaCaseBuilder().setOfArpSpa(Boolean.TRUE).build(),
                new BigInteger(1, IpToBytes(ip)),
                31,
                false
            )
        );

        return ab.build();
    }

    public static void addMatchEncapEthType(MatchBuilder match, int ethType) {
        NxAugMatchNodesNodeTableFlow am =
            new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxEncapEthType(new NxmNxEncapEthTypeBuilder()
                    .setValue(Integer.valueOf(ethType))
                    .build())
                .build();
        addExtension(match, NxmNxEncapEthTypeKey.class, am);
    }

    public static void addMatchEncapEthSrc(MatchBuilder match, String mac) {
        NxAugMatchNodesNodeTableFlow am =
            new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxEncapEthSrc(new NxmNxEncapEthSrcBuilder()
                    .setMacAddress(new MacAddress(mac))
                    .build())
                .build();
        addExtension(match, NxmNxEncapEthSrcKey.class, am);
    }

    public static void addMatchEncapEthDst(MatchBuilder match, String mac) {
        NxAugMatchNodesNodeTableFlow am =
            new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxEncapEthDst(new NxmNxEncapEthDstBuilder()
                    .setMacAddress(new MacAddress(mac))
                    .build())
                .build();
        addExtension(match, NxmNxEncapEthDstKey.class, am);
    }

    public static void addMatchNshMdtype(MatchBuilder match, short nshMdtype) {
        NxAugMatchNodesNodeTableFlow am =
            new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxNshMdtype(new NxmNxNshMdtypeBuilder()
                    .setValue(Short.valueOf(nshMdtype))
                    .build())
                .build();
        addExtension(match, NxmNxNshMdtypeKey.class, am);
    }

    public static void addMatchNshNp(MatchBuilder match, short nshNp) {
        NxAugMatchNodesNodeTableFlow am =
            new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxNshNp(new NxmNxNshNpBuilder()
                    .setValue(Short.valueOf(nshNp))
                    .build())
                .build();
        addExtension(match, NxmNxNshNpKey.class, am);
    }

    public static void addMatchTunGpeNp(MatchBuilder match, short tunGpeNp) {
        NxAugMatchNodesNodeTableFlow am =
            new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxTunGpeNp(new NxmNxTunGpeNpBuilder()
                    .setValue(Short.valueOf(tunGpeNp))
                    .build())
                .build();
        addExtension(match, NxmNxTunGpeNpKey.class, am);
    }

    public static void addMatchReg0(MatchBuilder match, int value) {
        NxAugMatchNodesNodeTableFlow am =
            new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxReg(new NxmNxRegBuilder()
                    .setReg(NxmNxReg0.class)
                    .setValue(Long.valueOf(value))
                    .build())
                .build();
        addExtension(match, NxmNxReg0Key.class, am);
    }

    public static void addMatchInPort(MatchBuilder match, String nodeName, int value) {
       match.setInPort(new NodeConnectorId(nodeName + ":" + String.valueOf(value)));
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

    public static Action createActionNxLoadReg0(int value, int order){
        ActionBuilder ab = createActionBuilder(order);
        ab.setAction(
            nxLoadRegAction(
                new DstNxRegCaseBuilder().setNxReg(NxmNxReg0.class).build(),
                BigInteger.valueOf(value),
                31,
                false
            )
        );

        return ab.build();
    }

    public static Action createActionNxSetNsp(Long nsp, int order) {
        return createActionNxLoadNsp(nsp.intValue(), order);
    }

    public static Action createActionNxSetNsi(Short nsi, int order) {
        return createActionNxLoadNsi(nsi.shortValue(), order);
    }

    public static Action createActionNxSetNshc1(Long c1, int order) {
        return createActionNxLoadNshc1(c1.longValue(), order);
    }

    public static Action createActionNxSetNshc2(Long c2, int order) {
        return createActionNxLoadNshc2(c2.longValue(), order);
    }

    public static Action createActionNxSetNshc3(Long c3, int order) {
        return createActionNxLoadNshc3(c3.longValue(), order);
    }

    public static Action createActionNxSetNshc4(Long c4, int order) {
        return createActionNxLoadNshc4(c4.longValue(), order);
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
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(ib.build());
        isb.setInstruction(instructions);

        return isb;
    }

    public static InstructionsBuilder createInstructionsBuilder(InstructionBuilder... ibs) {
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<>();

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

   /**
     * Write a flow to the DataStore
     *
     * @param nodeName - which node to write the flow to
     * @param flow - details of the flow to be written
     */
    public static boolean writeFlowToDataStore(final String nodeName, FlowBuilder flow) {
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

        return SfcDataStoreAPI.writePutTransactionAPI(flowInstanceId, flow.build(),
                LogicalDatastoreType.CONFIGURATION);
    }


   /**
     * remove a flow from the DataStore
     *
     * @param nodeName - which node to write the flow to
     * @param tableKey - table Key
     * @param flowKey  - flow key
     */
    public static boolean removeFlowFromDataStore(final String nodeName, TableKey tableKey, FlowKey flowKey) {
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

        return SfcDataStoreAPI.deleteTransactionAPI(flowInstanceId, LogicalDatastoreType.CONFIGURATION);
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

    /**
     * Creates an Instance Identifier (path) for node with specified id
     *
     * @param nodeId the ID of the node
     * @return the {@link InstanceIdentifier}
     */
    public static final InstanceIdentifier<Node> createNodePath(final NodeId nodeId) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nodeId)).build();
    }

    /**
     * Creates a table path from a node ID and table ID
     *
     * @param nodeId the ID of the node
     * @param tableId the ID of the table
     * @return the {@link InstanceIdentifier}
     */
    public static final InstanceIdentifier<Table> createTablePath(final NodeId nodeId, final short tableId) {
        return createNodePath(nodeId).builder()
            .augmentation(FlowCapableNode.class)
            .child(Table.class, new TableKey(tableId))
            .build();
    }

    /**
     * Creates a path for particular flow, by appending flow-specific information
     * to table path.
     *
     * @param table the table iid
     * @param flowKey the flow key
     * @return the {@link InstanceIdentifier}
     */
    public static InstanceIdentifier<Flow> createFlowPath(final InstanceIdentifier<Table> table, final FlowKey flowKey) {
        return table.child(Flow.class, flowKey);
    }

    /**
     * Creates a path for particular flow, by appending flow-specific information
     * to table path.
     *
     * @param table the table iid
     * @param flowId the flow id
     * @return the {@link InstanceIdentifier}
     */
    public static InstanceIdentifier<Flow> createFlowPath(final InstanceIdentifier<Table> table, final FlowId flowId) {
        return createFlowPath(table, new FlowKey(flowId));
    }

}
