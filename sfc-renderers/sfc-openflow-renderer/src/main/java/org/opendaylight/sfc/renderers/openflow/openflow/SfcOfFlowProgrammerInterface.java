/*
 * Copyright (c) 2014, 2017 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.openflow.openflow;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.sfc.genius.util.appcoexistence.SfcTableIndexMapper;
import org.opendaylight.sfc.renderers.openflow.sfg.GroupBucketInfo;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

/**
 * An interface to be implemented by concrete classes that will write to
 * OpenFlow or OVS switches.
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @since 2015-02-24
 */
public interface SfcOfFlowProgrammerInterface {

    void shutdown() throws ExecutionException, InterruptedException;

    // These table methods are used for app-coexistence

    short getTableBase();

    void setTableBase(short tableBase);

    short getMaxTableOffset();

    short getTableEgress();

    void setTableEgress(short tableEgress);

    // Set the RSP Id that subsequent flow creations belong to
    void setFlowRspId(Long rspId);

    /**
     * Deletes all flows created for a particular RSP and removes initialization
     * flows from SFFs if the last RSP was removed.
     *
     * @param rspId
     *            the id of the RSP to be deleted
     *
     * @return Node IDs from which initialization flows were removed.
     */
    Set<NodeId> deleteRspFlows(long rspId);

    // Write any buffered flows to the data store
    void flushFlows();

    // Purge any unwritten flows not written yet. This should be called upon
    // errors, when the remaining buffered flows should not be written.
    void purgeFlows();

    // Set FlowWriter implementation
    void setFlowWriter(SfcOfFlowWriterInterface sfcOfFlowWriter);

    //
    // Configure Table 1, Transport Ingress
    //
    void configureIpv4TransportIngressFlow(String sffNodeName);

    void configureVlanTransportIngressFlow(String sffNodeName);

    void configureMacChainingTransportIngressFlow(String sffNodeName);

    // These 2 are work-around flows until the OVS NSH patch is completed
    void configureNshVxgpeSfLoopbackEncapsulatedEgressFlow(String sffNodeName, String sfIp, short vxlanUdpPort,
            long sffPort);

    void configureNshVxgpeSfReturnLoopbackIngressFlow(String sffNodeName, short vxlanUdpPort, long sffPort);

    void configureNshVxgpeTransportIngressFlow(String sffNodeName, long nshNsp, short nshNsi);

    void configureMplsTransportIngressFlow(String sffNodeName);

    void configureArpTransportIngressFlow(String sffNodeName, String mac);

    //
    // Configure Table 2, Path Mapper
    //
    void configureMplsPathMapperFlow(String sffNodeName, long label, long pathId, boolean isSf);

    void configureVlanPathMapperFlow(String sffNodeName, int vlan, long pathId, boolean isSf);
    // PathMapper not needed for NshVxgpe NSH
    // configureNshVxgpePathMapperFlow()

    //
    // Table 3, NextHop
    //
    void configureMacNextHopFlow(String sffNodeName, long sfpId, String srcMac, String dstMac);

    void configureGroupNextHopFlow(String sffNodeName, long sfpId, String srcMac, long groupId, String groupName);

    void configureMacChainingNextHopFlow(String sffNodeName, String vmac, String dstSfMac,
                                         String nextVMac, boolean l2Tranparent);


    void configureNshVxgpeNextHopFlow(String sffNodeName, String dstIp, String nshProxyIp, long nsp, short nsi);

    /**
     * Configure nsh next hop flow.
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
    void configureNshEthNextHopFlow(String sffNodeName, String srcMac, String dstMac, long nsp, short nsi);

    //
    // Table 10, Transport Egress
    //
    void configureVlanSfTransportEgressFlow(String sffNodeName, String srcMac, String dstMac, int dstVlan, String port,
            long pathId, boolean doPktin);

    void configureVlanTransportEgressFlow(String sffNodeName, String srcMac, String dstMac, int dstVlan, String port,
            long pathId);

    void configureMacChainingSfTransportEgressFlow(String sffNodeName, String dstMac, String port, String vmac);

    void configureVlanLastHopTransportEgressFlow(String sffNodeName, String srcMac, String dstMac, int dstVlan,
            String port, long pathId);

    void configureMplsTransportEgressFlow(String sffNodeName, String srcMac, String dstMac, long mplsLabel, String port,
            long pathId);

    void configureMplsLastHopTransportEgressFlow(String sffNodeName, String srcMac, String dstMac, long mplsLabel,
            String port, long pathId);

    void configureNshVxgpeTransportEgressFlow(String sffNodeName, long nshNsp, short nshNsi, String port);

    void configureNshVxgpeAppCoexistTransportEgressFlow(String sffNodeName, long nshNsp, short nshNsi, String sffIp);

    void configureNshVxgpeLastHopTransportEgressFlow(String sffNodeName, long nshNsp, short nshNsi, String port);

    void configureNshNscTransportEgressFlow(String sffNodeName, long nshNsp, short nshNsi, String switchPort);

    /**
     * Configure transport egress flows, using a list of externally provided
     * actions.
     *
     * @param sffOpenFlowNodeName
     *            The openflow identifier for the node on which the flows are to
     *            be written
     * @param nshNsp
     *            NSP to use in the match part
     * @param nshNsi
     *            NSI to use in the match part
     * @param actionList
     *            List of actions to use in the actions part
     * @param flowName
     *            A unique flow name, also used as a flow key
     */
    void configureNshEthTransportEgressFlow(String sffOpenFlowNodeName, long nshNsp, short nshNsi,
            List<Action> actionList, String flowName);

    void configureNshEthTransportEgressFlow(String sffNodeName, long nshNsp, short nshNsi, String port);

    //
    // Configure the MatchAny entry specifying if it should drop or goto the
    // next table
    // Classifier MatchAny will go to Ingress
    // TransportIngress MatchAny will go to PathMapper
    // PathMapper MatchAny will go to PathMapperAcl
    // PathMapperAcl MatchAny will go to NextHop
    // NextHop MatchAny will go to TransportEgress
    //
    void configureClassifierTableMatchAny(String sffNodeName);

    void configureTransportIngressTableMatchAny(String sffNodeName);

    void configureTransportIngressTableMatchAnyResubmit(String sffNodeName, short nextTableId);

    void configurePathMapperTableMatchAny(String sffNodeName);

    void configurePathMapperAclTableMatchAny(String sffNodeName);

    void configureNextHopTableMatchAny(String sffNodeName);

    void configureTransportEgressTableMatchAny(String sffNodeName);

    /**
     * Set the match any flow in the Transport Egress table to resubmit.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nextTableId
     *            - the table to resubmit
     */
    void configureTransportEgressTableMatchAnyResubmit(String sffNodeName, short nextTableId);

    /* OVS DPDK only, these APIs will add extra flows for OVS DPDK */
    void configureClassifierTableDpdkOutput(String sffNodeName, Long outPort);

    void configureClassifierTableDpdkInput(String sffNodeName, Long inPort);

    // group configuration
    void configureGroup(String sffNodeName, String openflowNodeId, String sfgName, long sfgId, int groupType,
            List<GroupBucketInfo> bucketInfos, boolean isAddGroup);

    /**
     * Used by logical sff processor in order to write chain egress flows.
     * The sff ip address is optional, if not provided it is assumed that
     * the sff has no dataplane tunnel endpoints.
     *
     * @param sffNodeName
     *            last openflow node in the chain
     * @param sffIpAddress
     *            the sff ip address
     * @param sfMacAddress
     *            the last sf mac address
     * @param nshNsp
     *            nsp for the match
     * @param nshNsi
     *            nsi for the match
     */
    void configureNshEthLastHopTransportEgressFlow(String sffNodeName, IpAddress sffIpAddress,
                                                   MacAddress sfMacAddress, long nshNsp, short nshNsi);

    /**
     * Setter for the table index mapper (class which provides the tables to use
     * for Genius-based application coexistence).
     *
     * @param tableIndexMapper
     *            The table index mapper
     */
    void setTableIndexMapper(SfcTableIndexMapper tableIndexMapper);
}
