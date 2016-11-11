/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.openflow;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.sfc.genius.util.appcoexistence.SfcTableIndexMapper;
import org.opendaylight.sfc.ofrenderer.sfg.GroupBucketInfo;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;


/**
 * An interface to be implemented by concrete classes that will write to OpenFlow or OVS switches.
 * <p>
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @since 2015-02-24
 */
public interface SfcOfFlowProgrammerInterface {

    public void shutdown() throws ExecutionException, InterruptedException;

    // These table methods are used for app-coexistence

    public short getTableBase();

    public void setTableBase(short tableBase);

    public short getMaxTableOffset();

    public short getTableEgress();

    public void setTableEgress(short tableEgress);

    // Set the RSP Id that subsequent flow creations belong to
    public void setFlowRspId(Long rspId);

    /**
     * Deletes all flows created for a particular RSP and removes
     * initialization flows from SFFs if the last RSP was removed.
     *
     * @param rspId the id of the RSP to be deleted
     *
     * @return Node IDs from which initialization flows were removed.
     */
    public Set<NodeId> deleteRspFlows(final long rspId);

    // Write any buffered flows to the data store
    public void flushFlows();

    // Purge any unwritten flows not written yet. This should be called upon
    // errors, when the remaining buffered flows should not be written.
    public void purgeFlows();

    //Set FlowWriter implementation
    public void setFlowWriter(SfcOfFlowWriterInterface sfcOfFlowWriter);

    //
    // Congfigure Table 1, Transport Ingress
    //
    public void configureIpv4TransportIngressFlow(final String sffNodeName);

    public void configureVlanTransportIngressFlow(final String sffNodeName);

    // These 2 are work-around flows until the OVS NSH patch is completed
    public void configureNshVxgpeSfLoopbackEncapsulatedEgressFlow(final String sffNodeName, final String sfIp, final short vxlanUdpPort, final long sffPort);
    public void configureNshVxgpeSfReturnLoopbackIngressFlow(final String sffNodeName, final short vxlanUdpPort, final long sffPort);

    public void configureNshVxgpeTransportIngressFlow(final String sffNodeName, final long nshNsp, final short nshNsi);

    public void configureMplsTransportIngressFlow(final String sffNodeName);

    public void configureArpTransportIngressFlow(final String sffNodeName, final String mac);

    //
    // Configure Table 2, Path Mapper
    //
    public void configureMplsPathMapperFlow(final String sffNodeName, final long label, long pathId, boolean isSf);

    public void configureVlanPathMapperFlow(final String sffNodeName, final int vlan, long pathId, boolean isSf);
    // PathMapper not needed for NshVxgpe NSH
    //configureNshVxgpePathMapperFlow()

    //
    // Table 3, NextHop
    //
    public void configureMacNextHopFlow(final String sffNodeName, final long sfpId, final String srcMac,
            final String dstMac);

    public void configureGroupNextHopFlow(final String sffNodeName, final long sfpId, final String srcMac,
            final long groupId, final String groupName);

    public void configureNshVxgpeNextHopFlow(final String sffNodeName, final String dstIp, final long nsp,
            final short nsi);

    /**
     * Configure nsh next hop flow
     *
     * @param sffNodeName
     *            The openflow node name
     * @param srcMac
     *            MAC address used by the openflow port to which the SF is
     *            connected
     * @param dstMac
     *            MAC address used by the SF
     * @param nsp
     *            the NSH NSP
     * @param nsi
     *            the NSH NSI
     */
    public void configureNshEthNextHopFlow(final String sffNodeName,
            final String srcMac, final String dstMac, final long nsp,
            final short nsi);

    //
    // Table 10, Transport Egress
    //
    public void configureVlanSfTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, final String port, final long pathId, final boolean doPktin);
    public void configureVlanTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, final String port, final long pathId);
    public void configureVlanLastHopTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, final String port, final long pathId);

    public void configureMplsTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, final String port, final long pathId);
    public void configureMplsLastHopTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, final String port, final long pathId);

    public void configureNshVxgpeTransportEgressFlow(
            final String sffNodeName, final long nshNsp, final short nshNsi, final String port);

    public void configureNshVxgpeAppCoexistTransportEgressFlow(
            final String sffNodeName, final long nshNsp, final short nshNsi, final String sffIp);

    public void configureNshVxgpeLastHopTransportEgressFlow(
            final String sffNodeName, final long nshNsp, final short nshNsi, final String port);

    public void configureNshNscTransportEgressFlow(
            String sffNodeName, final long nshNsp, final short nshNsi, String switchPort);

    public void configureNshEthTransportEgressFlow(
            String sffNodeName, final long nshNsp, final short nshNsi, final String port);

    //
    // Configure the MatchAny entry specifying if it should drop or goto the next table
    // Classifier MatchAny will go to Ingress
    // TransportIngress MatchAny will go to PathMapper
    // PathMapper MatchAny will go to PathMapperAcl
    // PathMapperAcl MatchAny will go to NextHop
    // NextHop MatchAny will go to TransportEgress
    //
    public void configureClassifierTableMatchAny(final String sffNodeName);

    public void configureTransportIngressTableMatchAny(final String sffNodeName);

    public void configurePathMapperTableMatchAny(final String sffNodeName);

    public void configurePathMapperAclTableMatchAny(final String sffNodeName);

    public void configureNextHopTableMatchAny(final String sffNodeName);

    public void configureTransportEgressTableMatchAny(final String sffNodeName);

    /* OVS DPDK only, these APIs will add extra flows for OVS DPDK */
    public void configureClassifierTableDpdkOutput(final String sffNodeName, Long outPort);
    public void configureClassifierTableDpdkInput(final String sffNodeName, Long inPort);

    // group configuration
    public void configureGroup(final String sffNodeName, final String openflowNodeId, final String sfgName,
            final long sfgId, int groupType, List<GroupBucketInfo> bucketInfos, final boolean isAddGroup);

    /**
     * Used by logical sff processor in order to write chain egress flows
     *
     * @param sffNodeName
     *            last openflow node in the chain
     * @param nshNsp
     *            nsp for the match
     * @param nshNsi
     *            nsi for the match
     * @param macAddress
     *            the mac address to set as source address at chain egress time
     *            (if not set, the src mac address after decapsulation would be
     *            the one set before the chain was executed (at classification
     *            time), and the packet would be dropped at subsequent pipeline
     *            processing)
     */
    public void configureNshEthLastHopTransportEgressFlow(String sffNodeName,
            long nshNsp, short nshNsi, MacAddress macAddress);

    /**
     * Configure transport egress flows, using a list of externally provided actions
     * @param sffOpenFlowNodeName  The openflow identifier for the node on which the flows are to be written
     * @param nshNsp         NSP to use in the match part
     * @param nshNsi         NSI to use in the match part
     * @param actionList     List of actions to use in the actions part
     */
    public void configureNshEthTransportEgressFlow(String sffOpenFlowNodeName,
            long nshNsp, short nshNsi, List<Action> actionList);

    /**
     * Setter for the table index mapper (class which provides the tables to use
     * for Genius-based application coexistence)
     *
     * @param tableIndexMapper
     *            The table index mapper
     */
    public void setTableIndexMapper(SfcTableIndexMapper tableIndexMapper);
}
