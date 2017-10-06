/*
 * Copyright (c) 2014, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.openflow;

import com.google.common.net.InetAddresses;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.genius.util.appcoexistence.SfcTableIndexMapper;
import org.opendaylight.sfc.ofrenderer.sfg.GroupBucketInfo;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class writes Openflow Flow Entries to the SFF once an SFF has been configured.
 *
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 *
 * @version 0.1
 *
 * @since 2014-08-07
 */
public class SfcOfFlowProgrammerImpl implements SfcOfFlowProgrammerInterface {

    private static final String OUTPUT_STRING = "output:";

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfFlowProgrammerImpl.class);

    public static final int COOKIE_BIGINT_HEX_RADIX = 16;

    // A common SFC Transport Egress Cookie Base String, allowing
    // all SFC flows to be matched by using cookieStr.startsWith()
    public static final String TRANSPORT_EGRESS_COOKIE_STR_BASE = "BA5EBA11";
    // The 000001** cookies are for NSH VXGPE Transport Egress flows
    public static final String TRANSPORT_EGRESS_NSH_VXGPE_COOKIE = "00000101";
    public static final String TRANSPORT_EGRESS_NSH_VXGPE_NSC_COOKIE = "00000102";
    public static final String TRANSPORT_EGRESS_NSH_VXGPE_LASTHOP_COOKIE = "00000103";
    public static final String TRANSPORT_EGRESS_NSH_VXGPE_APPCOEXIST_COOKIE = "00000104";
    // The 000002** cookies are for NSH Eth Transport Egress flows
    public static final String TRANSPORT_EGRESS_NSH_ETH_COOKIE = "00000201";
    public static final String TRANSPORT_EGRESS_NSH_ETH_LOGICAL_COOKIE = "00000202";
    public static final String TRANSPORT_EGRESS_NSH_ETH_LASTHOP_PIPELINE_COOKIE = "00000203";
    public static final String TRANSPORT_EGRESS_NSH_ETH_LASTHOP_TUNNEL_REMOTE_COOKIE = "00000204";
    public static final String TRANSPORT_EGRESS_NSH_ETH_LASTHOP_TUNNEL_LOCAL_COOKIE = "00000205";
    public static final String TRANSPORT_EGRESS_NSH_ETH_LASTHOP_NSH_REMOTE_COOKIE = "00000206";
    public static final String TRANSPORT_EGRESS_NSH_ETH_LASTHOP_NSH_LOCAL_COOKIE = "00000207";
    // The 000003** cookies are for VXGEP NSH Transport Egress flows
    public static final String TRANSPORT_EGRESS_VLAN_COOKIE = "00000301";
    public static final String TRANSPORT_EGRESS_VLAN_SF_COOKIE = "00000302";
    public static final String TRANSPORT_EGRESS_VLAN_LASTHOP_COOKIE = "00000303";
    // The 000004** cookies are for VXGEP NSH Transport Egress flows
    public static final String TRANSPORT_EGRESS_MPLS_COOKIE = "00000401";
    public static final String TRANSPORT_EGRESS_MPLS_LASTHOP_COOKIE = "00000402";
    public static final String TRANSPORT_EGRESS_MAX_COOKIE = "00000FFF";

    // Flow table names
    public static final String FLOW_NAME_DELIMITER = "_";
    public static final String FLOW_NAME_MATCH_ANY = "matchAny";
    public static final String FLOW_NAME_MATCH_ANY_DROP = "matchAnyDrop";
    public static final String FLOW_NAME_MATCH_ANY_RESUBMIT = "matchAnyResubmit";
    public static final String FLOW_NAME_CLASS_DPDK_OUT = "classifierDpdkOutput";
    public static final String FLOW_NAME_CLASS_DPDK_IN = "classifierDpdkInput";
    public static final String FLOW_NAME_TRANSPORT_INGRESS = "transportIngress";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_ARP = "transportIngressArp";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_MAC = "transportIngressMac";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_MPLS = "transportIngressMpls";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_TCP = "transportIngressTcp";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_UDP = "transportIngressUdp";
    public static final String FLOW_NAME_TRANSPORT_INGRESS_VLAN = "transportIngressVlan";
    public static final String FLOW_NAME_PATH_MAPPER = "pathMapper";
    public static final String FLOW_NAME_PATH_MAPPER_ACL = "pathMapperAcl";
    public static final String FLOW_NAME_NEXT_HOP = "nextHop";
    public static final String FLOW_NAME_TRANSPORT_EGRESS = "transportEgress";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS = "transportEgressLastHop";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_PIPELINE = "transportEgressLastHop_Pipeline";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_TUNNEL_REMOTE = "transportEgressLastHop_TunnelRemote";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_TUNNEL_LOCAL = "transportEgressLastHop_TunnelLocal";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_NSH_REMOTE = "transportEgressLastHop_NshRemote";
    public static final String FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_NSH_LOCAL = "transportEgressLastHop_NshLocal";
    public static final String FLOW_NAME_APPCOEXIST_TRANSPORT_EGRESS = "transportEgressAppCoexist";
    public static final String FLOW_NAME_SF_LOOPBACK_INGRESS = "sfLoopbackIngress";
    public static final String FLOW_NAME_SF_LOOPBACK_EGRESS = "sfLoopbackEgress";

    // The 000005** cookies are for MAC Chaining Transport Egress flows
    public static final String TRANSPORT_EGRESS_MAC_CHAINING_COOKIE = "00000501";

    // Which bits in the metadata field to set, Assuming 4095 PathId's
    public static final BigInteger METADATA_MASK_SFP_MATCH = new BigInteger("FFFFFFFFFFFFFFFF",
            COOKIE_BIGINT_HEX_RADIX);
    public static final short TABLE_INDEX_CLASSIFIER = 0;
    public static final short TABLE_INDEX_TRANSPORT_INGRESS = 1;
    public static final short TABLE_INDEX_PATH_MAPPER = 2;
    public static final short TABLE_INDEX_PATH_MAPPER_ACL = 3;
    public static final short TABLE_INDEX_NEXT_HOP = 4;
    public static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;

    public static final short TABLE_INDEX_MAX_OFFSET = TABLE_INDEX_TRANSPORT_EGRESS;
    public static final int FLOW_PRIORITY_TRANSPORT_INGRESS = 250;
    public static final int FLOW_PRIORITY_ARP_TRANSPORT_INGRESS = 300;
    public static final int FLOW_PRIORITY_PATH_MAPPER = 350;
    public static final int FLOW_PRIORITY_PATH_MAPPER_ACL = 450;
    public static final int FLOW_PRIORITY_NEXT_HOP = 550;
    public static final int FLOW_PRIORITY_TRANSPORT_EGRESS = 650;
    public static final int FLOW_PRIORITY_TRANSPORT_EGRESS_TUNNEL_REMOTE = 650;
    public static final int FLOW_PRIORITY_TRANSPORT_EGRESS_TUNNEL_LOCAL = 660;
    public static final int FLOW_PRIORITY_TRANSPORT_EGRESS_NSH_REMOTE = 670;
    public static final int FLOW_PRIORITY_TRANSPORT_EGRESS_NSH_LOCAL = 680;
    public static final int FLOW_PRIORITY_TRANSPORT_EGRESS_PIPELINE = 680;
    public static final int FLOW_PRIORITY_TRANSPORT_EGRESS_NSH = 660;
    public static final int FLOW_PRIORITY_MATCH_ANY = 5;

    public static final int FLOW_PRIORITY_CLASSIFIER = 1000;
    private static final int PKTIN_IDLE_TIMEOUT = 60;
    private static final String EMPTY_SWITCH_PORT = "";
    public static final short APP_COEXISTENCE_NOT_SET = -1;
    private static final String LOCALHOST_IP = "127.0.0.1";

    // Instance variables
    private short tableBase;
    // Used for app-coexistence
    private short tableEgress;
    private Long flowRspId;
    private SfcOfFlowWriterInterface sfcOfFlowWriter = null;

    private SfcTableIndexMapper tableIndexMapper = null;

    public SfcOfFlowProgrammerImpl() {
        this.tableBase = APP_COEXISTENCE_NOT_SET;
        this.tableEgress = APP_COEXISTENCE_NOT_SET;
        this.flowRspId = 0L;
    }

    public SfcOfFlowProgrammerImpl(SfcOfFlowWriterInterface sfcOfFlowWriter) {
        this.tableBase = APP_COEXISTENCE_NOT_SET;
        this.tableEgress = APP_COEXISTENCE_NOT_SET;
        this.flowRspId = 0L;
        this.sfcOfFlowWriter = sfcOfFlowWriter;
    }

    @Override
    public void setFlowWriter(SfcOfFlowWriterInterface sfcOfFlowWriter) {
        this.sfcOfFlowWriter = sfcOfFlowWriter;
    }

    // This method should only be called by SfcOfRenderer.close()
    @Override
    public void shutdown() throws ExecutionException, InterruptedException {
        this.sfcOfFlowWriter.shutdown();
    }

    //
    // Getters/Setters
    //

    @Override
    public short getTableBase() {
        return tableBase;
    }

    @Override
    public void setTableBase(short tableBase) {
        this.tableBase = tableBase;
    }

    @Override
    public short getTableEgress() {
        return tableEgress;
    }

    @Override
    public void setTableEgress(short tableEgress) {
        this.tableEgress = tableEgress;
    }

    @Override
    public short getMaxTableOffset() {
        return TABLE_INDEX_MAX_OFFSET;
    }

    @Override
    public void setFlowRspId(Long rspId) {
        this.flowRspId = rspId;
    }

    @Override
    public Set<NodeId> deleteRspFlows(final long rspId) {
        sfcOfFlowWriter.deleteRspFlows(rspId);
        Set<NodeId> nodes = sfcOfFlowWriter.clearSffsIfNoRspExists();
        sfcOfFlowWriter.deleteFlowSet();
        return nodes;
    }

    @Override
    public void flushFlows() {
        this.sfcOfFlowWriter.flushFlows();
    }

    @Override
    public void purgeFlows() {
        this.sfcOfFlowWriter.purgeFlows();
    }

    /**
     * Check if the given cookie belongs to the Classification table.
     *
     * @param cookie
     *            - the cookie to compare
     * @return true if the cookie belongs to the Classification table, false
     *         otherwise
     */
    public boolean compareClassificationTableCookie(FlowCookie cookie) {
        return cookie != null && cookie.getValue() != null && cookie.toString().toUpperCase(Locale.getDefault())
                .startsWith(TRANSPORT_EGRESS_COOKIE_STR_BASE);

    }

    //
    // Configure the MatchAny entry specifying if it should drop or goto the
    // next table
    // Classifier MatchAny will go to TransportIngress
    // TransportIngress MatchAny will drop
    // PathMapper MatchAny will go to PathMapperAcl
    // PathMapperAcl MatchAny will go to NextHop
    // NextHop MatchAny will go to TransportEgress
    // TransportEgress MatchAny will drop
    //

    /**
     * Set the match any flow in the Classifier table to go to the Transport
     * Ingress table.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configureClassifierTableMatchAny(final String sffNodeName) {
        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We don't need this flow with App Coexistence.
            return;
        }

        FlowBuilder flowBuilder = configureTableMatchAnyFlow(getTableId(TABLE_INDEX_CLASSIFIER),
                getTableId(TABLE_INDEX_TRANSPORT_INGRESS));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set DPDK output flow in the Classifier table for OVS DPDK.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configureClassifierTableDpdkOutput(final String sffNodeName, Long outPort) {
        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We don't need this flow with App Coexistence.
            return;
        }

        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchInPort(match, new NodeConnectorId(OutputPortValues.LOCAL.toString()));

        int order = 0;

        // Action output
        List<Action> actionList = new ArrayList<>();
        String outPortStr = OUTPUT_STRING + outPort.toString();
        actionList.add(SfcOpenflowUtils.createActionOutPort(outPortStr, order));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_CLASS_DPDK_OUT).add(String.valueOf(outPort));

        // Create and configure the FlowBuilder
        FlowBuilder classifierDpdkOutputFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_CLASSIFIER),
                FLOW_PRIORITY_CLASSIFIER, flowName.toString(), match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, classifierDpdkOutputFlow);
    }

    /**
     * Set DPDK input flow in the Classifier table for OVS DPDK.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configureClassifierTableDpdkInput(final String sffNodeName, Long inPort) {
        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We don't need this flow with App Coexistence.
            return;
        }

        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchInPort(match, new NodeId(sffNodeName), inPort);

        int order = 0;

        // Action NORMAL
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNormal(order));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_CLASS_DPDK_IN).add(String.valueOf(inPort));

        // Create and configure the FlowBuilder
        FlowBuilder classifierDpdkInputFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_CLASSIFIER),
                FLOW_PRIORITY_CLASSIFIER, flowName.toString(), match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, classifierDpdkInputFlow);
    }

    /**
     * Set the match any flow in the Transport Ingress table to drop.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configureTransportIngressTableMatchAny(final String sffNodeName) {
        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We don't need this flow with App Coexistence.
            return;
        }

        FlowBuilder flowBuilder = configureTableMatchAnyDropFlow(getTableId(TABLE_INDEX_TRANSPORT_INGRESS));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Transport Ingress table to resubmit.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nextTableId
     *            - the table to resubmit
     */
    @Override
    public void configureTransportIngressTableMatchAnyResubmit(final String sffNodeName, short nextTableId) {
        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We don't need this flow with App Coexistence.
            return;
        }

        FlowBuilder flowBuilder = configureTableMatchAnyFlowResubmit(getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                 nextTableId);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Path Mapper table to go to the Path Mapper
     * ACL table.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configurePathMapperTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder = configureTableMatchAnyFlow(getTableId(TABLE_INDEX_PATH_MAPPER),
                getTableId(TABLE_INDEX_PATH_MAPPER_ACL));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Path Mapper ACL table to go to the Next Hop
     * table.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configurePathMapperAclTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder = configureTableMatchAnyFlow(getTableId(TABLE_INDEX_PATH_MAPPER_ACL),
                getTableId(TABLE_INDEX_NEXT_HOP));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Next Hop table to go to the Transport
     * Egress table.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configureNextHopTableMatchAny(final String sffNodeName) {
        FlowBuilder flowBuilder = configureTableMatchAnyFlow(getTableId(TABLE_INDEX_NEXT_HOP),
                getTableId(TABLE_INDEX_TRANSPORT_EGRESS));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Transport Egress table to drop.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configureTransportEgressTableMatchAny(final String sffNodeName) {
        // This is the last table, can't set next table AND doDrop should be
        // false
        FlowBuilder flowBuilder = configureTableMatchAnyDropFlow(getTableId(TABLE_INDEX_TRANSPORT_EGRESS));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    @Override
    public void configureTransportEgressTableMatchAnyResubmit(final String sffNodeName, short nextTableId) {

        FlowBuilder flowBuilder = configureTableMatchAnyFlowResubmit(getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                nextTableId);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Internal util method to create the Match Any Drop flow.
     *
     * @param tableId
     *            - the table to write to
     *
     * @return the created flow
     */
    private FlowBuilder configureTableMatchAnyDropFlow(short tableId) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTableMatchAnyDropFlow tableId [{}]", tableId);

        // Add our drop action to a list
        List<Action> actionList = new ArrayList<Action>() {
            {
                add(SfcOpenflowUtils.createActionDropPacket(0));
            }
        };

        // Match any
        MatchBuilder match = new MatchBuilder();

        // Finish up the instructions
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_MATCH_ANY_DROP).add(String.valueOf(tableId));

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(tableId, FLOW_PRIORITY_MATCH_ANY, flowName.toString(), match, isb);
    }

    /**
     * Internal util method to create the Match Any flow.
     *
     * @param tableId
     *            - the table to write to
     * @param nextTableId
     *            - the next table to go to
     *
     * @return the created flow
     */
    private FlowBuilder configureTableMatchAnyFlow(short tableId, short nextTableId) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTableMatchAnyFlow, tableId [{}] nextTableId [{}]", tableId,
                nextTableId);

        // Match any
        MatchBuilder match = new MatchBuilder();

        InstructionsBuilder isb = SfcOpenflowUtils.appendGotoTableInstruction(new InstructionsBuilder(), nextTableId);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_MATCH_ANY).add(String.valueOf(tableId)).add(String.valueOf(nextTableId));

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(tableId, FLOW_PRIORITY_MATCH_ANY, flowName.toString(), match, isb);
    }

    /**
     * Internal util method to create the Match Any flow and resubmit packets.
     *
     * @param tableId
     *            - the table to write to
     * @param nextTableId
     *            - the next table to resubmit
     *
     * @return the created flow
     */
    private FlowBuilder configureTableMatchAnyFlowResubmit(short tableId, short nextTableId) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTableMatchAnyFlowResubmit, tableId [{}] nextTableId [{}]", tableId,
                nextTableId);

        // Match any
        MatchBuilder match = new MatchBuilder();

        // Resubmit is an action not an instruction
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionResubmitTable(nextTableId, actionList.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_MATCH_ANY_RESUBMIT).add(String.valueOf(tableId)).add(String.valueOf(nextTableId));

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(tableId, FLOW_PRIORITY_MATCH_ANY, flowName.toString(), match, isb);
    }

    //
    // Configure Table 1, Transport Ingress
    //

    /**
     * Configure IPv4 Transport Ingress flows. 2 flows will be created, one for
     * TCP and another for UDP.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configureIpv4TransportIngressFlow(final String sffNodeName) {
        FlowBuilder transportIngressFlowTcp = configureTransportIngressFlow(SfcOpenflowUtils.ETHERTYPE_IPV4,
                SfcOpenflowUtils.IP_PROTOCOL_TCP, FLOW_NAME_TRANSPORT_INGRESS_TCP);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlowTcp);

        FlowBuilder transportIngressFlowUdp = configureTransportIngressFlow(SfcOpenflowUtils.ETHERTYPE_IPV4,
                SfcOpenflowUtils.IP_PROTOCOL_UDP, FLOW_NAME_TRANSPORT_INGRESS_UDP);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlowUdp);
    }

    /**
     * Configure a VLAN Transport Ingress flow.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configureVlanTransportIngressFlow(final String sffNodeName) {
        // vlan match
        // For some reason it didn't match setting etherType=0x8100
        VlanMatchBuilder vlanBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        vlanIdBuilder.setVlanIdPresent(true);
        vlanBuilder.setVlanId(vlanIdBuilder.build());

        MatchBuilder match = new MatchBuilder();
        match.setVlanMatch(vlanBuilder.build());

        FlowBuilder transportIngressFlow = configureTransportIngressFlow(match, FLOW_NAME_TRANSPORT_INGRESS_VLAN);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlow);
    }

    /**
     * Configure a Transport Ingress flow.
     *
     * @param sffNodeName - the SFF to write the flow to
     */
    @Override
    public void configureMacChainingTransportIngressFlow(final String sffNodeName) {

        MatchBuilder match = new MatchBuilder();

        FlowBuilder transportIngressFlow = configureTransportIngressFlow(match, FLOW_NAME_TRANSPORT_INGRESS_MAC);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlow);
    }

    /**
     * Configure a NshVxgpe Transport Ingress flow, by matching on EtherType
     * IPv4.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configureNshVxgpeTransportIngressFlow(final String sffNodeName, final long nshNsp, final short nshNsi) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_TRANSPORT_INGRESS).add(String.valueOf(nshNsi)).add(String.valueOf(nshNsp));

        FlowBuilder transportIngressFlow =
                configureTransportIngressFlow(match, getTableId(TABLE_INDEX_NEXT_HOP), flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlow);
    }

    /**
     * Configure an MPLS Transport Ingress flow, by matching on EtherType MPLS
     * Ucast.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    @Override
    public void configureMplsTransportIngressFlow(final String sffNodeName) {
        FlowBuilder transportIngressFlow = configureTransportIngressFlow(
                SfcOpenflowUtils.ETHERTYPE_MPLS_UCAST, FLOW_NAME_TRANSPORT_INGRESS_MPLS);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportIngressFlow);
    }

    /**
     * Simple pass through with default args for ipProtocol and nextTable.
     *
     * @param etherType
     *            - the etherType protocol to set in the match
     *
     * @return a FlowBuilder with the created Transport Ingress flow
     */
    private FlowBuilder configureTransportIngressFlow(long etherType, String flowName) {
        return configureTransportIngressFlow(etherType, (short) -1, getTableId(TABLE_INDEX_PATH_MAPPER), flowName);
    }

    /**
     * Simple pass through with default args for nextTable.
     *
     * @param etherType
     *            - the etherType protocol to set in the match
     * @param ipProtocol
     *            - the IP protocol to set in the match
     *
     * @return a FlowBuilder with the created Transport Ingress flow
     */
    private FlowBuilder configureTransportIngressFlow(long etherType, short ipProtocol, String flowName) {
        return configureTransportIngressFlow(etherType, ipProtocol, getTableId(TABLE_INDEX_PATH_MAPPER), flowName);
    }

    private FlowBuilder configureTransportIngressFlow(
            long etherType, short ipProtocol, short nextTable, String flowName) {
        MatchBuilder match = new MatchBuilder();
        if (ipProtocol > 0) {
            SfcOpenflowUtils.addMatchIpProtocol(match, ipProtocol);
        }
        SfcOpenflowUtils.addMatchEtherType(match, etherType);

        return configureTransportIngressFlow(match, nextTable, flowName);
    }

    private FlowBuilder configureTransportIngressFlow(MatchBuilder match, String flowName) {
        return configureTransportIngressFlow(match, getTableId(TABLE_INDEX_PATH_MAPPER), flowName);
    }

    /**
     * Internal util method used by the previously defined
     * configureTransportIngressFlow() methods.
     *
     * @param match
     *            - the MatchBuilder object which will create the matches
     * @param nextTable
     *            - the nextTable to jump to upon matching
     *
     * @return a FlowBuilder with the created Transport Ingress flow
     */
    private FlowBuilder configureTransportIngressFlow(MatchBuilder match, short nextTable, String flowName) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportIngressFlow");

        InstructionsBuilder isb = SfcOpenflowUtils.appendGotoTableInstruction(new InstructionsBuilder(), nextTable);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                FLOW_PRIORITY_TRANSPORT_INGRESS, flowName, match, isb);
    }

    /**
     * Create an ARP responder flow in the Transport Ingress table. This flow is
     * intended to respond to SF ARP messages, and is only created for SFs of
     * type TCP-Proxy.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param mac
     *            - the SFF mac
     */
    @Override
    public void configureArpTransportIngressFlow(final String sffNodeName, final String mac) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportArpIngressThread, sff [{}] mac [{}]", sffNodeName, mac);

        // Create the matching criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_ARP);
        SfcOpenflowUtils.addMatchArpRequest(match);

        int order = 0;
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNxMoveEthSrcToEthDstAction(order++));
        actionList.add(SfcOpenflowUtils.createActionSetDlSrc(mac, order++));
        actionList.add(SfcOpenflowUtils.createActionNxLoadArpOpAction(SfcOpenflowUtils.ARP_REPLY, order++));
        actionList.add(SfcOpenflowUtils.createActionNxLoadArpShaAction(mac, order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpShaToArpThaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpTpaToRegAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveArpSpaToArpTpaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionNxMoveRegToArpSpaAction(order++));
        actionList.add(SfcOpenflowUtils.createActionOutPort(OutputPortValues.INPORT.toString(), order++));

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_TRANSPORT_INGRESS_ARP).add(mac);

        // Create and configure the FlowBuilder
        FlowBuilder arpTransportIngressFlow = SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_TRANSPORT_INGRESS), FLOW_PRIORITY_ARP_TRANSPORT_INGRESS,
                flowName.toString(), match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, arpTransportIngressFlow);
    }

    @Override
    public void configureNshVxgpeSfLoopbackEncapsulatedEgressFlow(final String sffNodeName, final String sfIp,
            final short vxlanUdpPort, final long sffPort) {

        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
        SfcOpenflowUtils.addMatchIpProtocol(match, SfcOpenflowUtils.IP_PROTOCOL_UDP);
        SfcOpenflowUtils.addMatchDstIpv4(match, sfIp, 32);
        SfcOpenflowUtils.addMatchDstUdpPort(match, vxlanUdpPort);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionOutPort((int) sffPort, 0));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_SF_LOOPBACK_EGRESS).add(String.valueOf(vxlanUdpPort)).add(String.valueOf(sffPort));

        // Create and configure the FlowBuilder
        FlowBuilder sfFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                FLOW_PRIORITY_ARP_TRANSPORT_INGRESS, flowName.toString(), match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, sfFlow);
    }

    @Override
    public void configureNshVxgpeSfReturnLoopbackIngressFlow(final String sffNodeName, final short vxlanUdpPort,
            final long sffPort) {
        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
        SfcOpenflowUtils.addMatchIpProtocol(match, SfcOpenflowUtils.IP_PROTOCOL_UDP);
        SfcOpenflowUtils.addMatchDstUdpPort(match, vxlanUdpPort);
        SfcOpenflowUtils.addMatchInPort(match, new NodeId(sffNodeName), sffPort);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionOutPort(OutputPortValues.LOCAL.toString(), 0));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_SF_LOOPBACK_INGRESS).add(String.valueOf(vxlanUdpPort)).add(String.valueOf(sffPort));

        // Create and configure the FlowBuilder
        FlowBuilder sfFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                FLOW_PRIORITY_ARP_TRANSPORT_INGRESS, flowName.toString(), match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, sfFlow);
    }

    //
    // Configure Table 2, PathMapper
    //

    /**
     * Create an MPLS Path Mapper flow. This flow will match on the MPLS label,
     * and set the RSP ID in the metadata.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param mplsLabel
     *            - the mplsLabel to match on
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param isSf
     *            - if the flow is for an SF or SFF
     */
    @Override
    public void configureMplsPathMapperFlow(final String sffNodeName, final long mplsLabel, long pathId, boolean isSf) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMplsLabel(match, mplsLabel);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionPopMpls(0));

        FlowBuilder pathMapperFlow;
        if (isSf) {
            pathMapperFlow = configurePathMapperSfFlow(pathId, match, actionList);
        } else {
            pathMapperFlow = configurePathMapperFlow(pathId, match, actionList);
        }
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, pathMapperFlow);
    }

    /**
     * Create a VLAN Path Mapper flow. This flow will match on the VLAN tag, and
     * set the RSP ID in the metadata.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param vlan
     *            - the vlan tag to match on
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param isSf
     *            - if the flow is for an SF or SFF
     */
    @Override
    public void configureVlanPathMapperFlow(final String sffNodeName, final int vlan, long pathId, boolean isSf) {
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchVlan(match, vlan);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionPopVlan(0));

        FlowBuilder pathMapperFlow;
        if (isSf) {
            pathMapperFlow = configurePathMapperSfFlow(pathId, match, actionList);
        } else {
            pathMapperFlow = configurePathMapperFlow(pathId, match, actionList);
        }
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, pathMapperFlow);
    }

    /**
     * Simple pass through for SF Path Mapper flows.
     *
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configurePathMapperSfFlow(final long pathId, MatchBuilder match, List<Action> actionList) {
        SfcOpenflowUtils.addMatchDscp(match, (short) pathId);
        return configurePathMapperFlow(pathId, match, actionList, FLOW_PRIORITY_PATH_MAPPER + 10);
    }

    /**
     * Simple pass through with default arg for flowPriority.
     *
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configurePathMapperFlow(final long pathId, MatchBuilder match, List<Action> actionList) {
        return configurePathMapperFlow(pathId, match, actionList, FLOW_PRIORITY_PATH_MAPPER);
    }

    /**
     * Internal util method used by the previously defined
     * configurePathMapperFlow() methods.
     *
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configurePathMapperFlow(final long pathId, MatchBuilder match, List<Action> actionList,
            int flowPriority) {
        LOG.debug("SfcProviderSffFlowWriter.configurePathMapperFlow sff [{}] pathId [{}]", pathId);

        // Apply actions take actions instantly - so, does not matter which
        // order they take - let's assume first
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);
        SfcOpenflowUtils.appendMetadataInstruction(isb, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);
        SfcOpenflowUtils.appendGotoTableInstruction(isb, getTableId(TABLE_INDEX_NEXT_HOP));

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_PATH_MAPPER).add(String.valueOf(pathId)).add(match.toString());

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_PATH_MAPPER), flowPriority,
                flowName.toString(), match, isb);
    }

    //
    // Table 3, PathMapper ACL
    //

    /**
     * This table is populated as a result of PktIn for TCP Proxy SFs. It
     * matches on Src/Dst IP and writes the path ID to the metadata.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param pktSrcIpStr
     *            - Src IP to match on
     * @param pktDstIpStr
     *            - Src IP to match on
     * @param pathId
     *            - the RSP ID to write to the metadata
     */
    public void configurePathMapperAclFlow(final String sffNodeName, final String pktSrcIpStr, final String pktDstIpStr,
            short pathId) {
        LOG.debug("SfcProviderSffFlowWriter.configurePathMapperAclFlow sff [{}] srcIp [{}] dstIp [{}] pathId [{}]",
                sffNodeName, pktSrcIpStr, pktDstIpStr, pathId);

        // Match on the Src and Dst IPs
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);
        SfcOpenflowUtils.addMatchSrcIpv4(match, pktSrcIpStr, 32);
        SfcOpenflowUtils.addMatchDstIpv4(match, pktDstIpStr, 32);

        InstructionsBuilder isb = SfcOpenflowUtils.appendMetadataInstruction(new InstructionsBuilder(),
                getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);
        SfcOpenflowUtils.appendGotoTableInstruction(isb, getTableId(TABLE_INDEX_NEXT_HOP));

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_PATH_MAPPER_ACL).add(pktSrcIpStr).add(pktDstIpStr).add(String.valueOf(pathId));

        // Create and configure the FlowBuilder
        FlowBuilder ingressFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_PATH_MAPPER_ACL),
                FLOW_PRIORITY_PATH_MAPPER_ACL, flowName.toString(), match, isb);
        // Set an idle timeout on this flow
        ingressFlow.setIdleTimeout(PKTIN_IDLE_TIMEOUT);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, ingressFlow);
    }

    //
    // Table 4, NextHop
    //

    /**
     * Configure the MAC/VLAN Next Hop by matching on the SrcMac and on the
     * pathId stored in the metadata.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param pathId
     *            - the RSP ID to write to the metadata
     * @param srcMac
     *            - Src mac to match on
     * @param dstMac
     *            - Dst mac to set on the packet
     */
    @Override
    public void configureMacNextHopFlow(final String sffNodeName, final long pathId, final String srcMac,
            final String dstMac) {
        int flowPriority = FLOW_PRIORITY_NEXT_HOP;
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);
        if (srcMac != null) {
            SfcOpenflowUtils.addMatchSrcMac(match, srcMac);
        } else {
            // If the srcMac is null, then the packet is entering SFC and we
            // dont know
            // from where. Make it a lower priority, and only match on the
            // pathId
            flowPriority -= 10;
        }

        List<Action> actionList = new ArrayList<>();
        if (dstMac != null) {
            // Set the DL (Data Link) Dest Mac Address
            actionList.add(SfcOpenflowUtils.createActionSetDlDst(dstMac, actionList.size()));
        }

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_NEXT_HOP).add(String.valueOf(pathId)).add(srcMac).add(dstMac);

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList, flowPriority, flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

    /**
     * Configure the Next Hop by matching on the virtual MAC address containing path and hop ID
     * for MAC chaining encapsulation.
     *
     * @param sffNodeName
     *              the SFF to write the flow to
     * @param vmac
     *              the virtual mac for this hop
     * @param dstSfMac
     *              destination mac of next SF
     * @param nextVMac
     *              next hop virtual mac
     * @param l2Transparent
     *              indicates if the SF do not swap MAC addresses (l2 transparent)
     */
    @Override
    public void configureMacChainingNextHopFlow(final String sffNodeName, final String vmac, final String dstSfMac,
                                                final String nextVMac, final boolean l2Transparent) {
        MatchBuilder match = new MatchBuilder();
        // for L2 transparent, MAC addres are not touched so match in the src MAC
        if (l2Transparent) {
            SfcOpenflowUtils.addMatchSrcMac(match, vmac);

        // for non L2 transparent, MAC addres are reversed so match in the dest MAC
        } else {
            SfcOpenflowUtils.addMatchDstMac(match, vmac);
        }

        List<Action> actionList = new ArrayList<>();
        // If the packet is going to other SFF we do not change vMAC because the hop is the same
        if (nextVMac != null) {
            actionList.add(SfcOpenflowUtils.createActionSetDlSrc(nextVMac, actionList.size()));
        }

        if (dstSfMac != null) {
            actionList.add(SfcOpenflowUtils.createActionSetDlDst(dstSfMac, actionList.size()));
        }

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_NEXT_HOP).add(vmac).add(nextVMac).add(dstSfMac);

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList, FLOW_PRIORITY_NEXT_HOP, flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

    /**
     * Configure the NshVxgpe NSH Next Hop by matching on the NSH pathId and
     * index stored in the NSH header.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param dstIp
     *            - the VxLan GPE tunnel destination IP
     * @param nshNsp
     *            - NSH Service Path to match on
     * @param nshNsi
     *            - NSH Index to match on
     */
    @Override
    public void configureNshVxgpeNextHopFlow(final String sffNodeName, final String dstIp, final String nshProxyIp,
            final long nshNsp,
            final short nshNsi) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);

        List<Action> actionList = new ArrayList<>();
        if (nshProxyIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(nshProxyIp, actionList.size());
            actionList.add(actionSetNwDst);
            if (dstIp != null) {
                int ip = InetAddresses.coerceToInteger(InetAddresses.forString(dstIp));
                long ipl = ip & 0xffffffffL;
                Action actionSetNshC3 = SfcOpenflowUtils.createActionNxLoadNshc3(ipl, actionList.size());
                actionList.add(actionSetNshC3);
            }
        } else if (dstIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionNxSetTunIpv4Dst(dstIp, actionList.size());
            actionList.add(actionSetNwDst);
        }

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_NEXT_HOP).add(String.valueOf(nshNsi)).add(String.valueOf(nshNsp));

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList, flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

    /**
     * Configure the NshEth Next Hop by matching on the NSH pathId and index
     * stored in the NSH header.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source Mac
     * @param dstMac
     *            - the destination Mac
     * @param nsp
     *            - NSH Service Path to match on
     * @param nsi
     *            - NSH Index to match on
     */
    @Override
    public void configureNshEthNextHopFlow(String sffNodeName, String srcMac, String dstMac, long nsp, short nsi) {

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNxLoadEncapEthSrc(srcMac, actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxLoadEncapEthDst(dstMac, actionList.size()));

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_NEXT_HOP).add(String.valueOf(nsi)).add(String.valueOf(nsp));

        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nsp, nsi);

        FlowBuilder nextHopFlow = configureNextHopFlow(match, actionList, flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

    /**
     * Simple pass through with default arg for flowPriority.
     *
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configureNextHopFlow(MatchBuilder match, List<Action> actionList, String flowName) {
        return configureNextHopFlow(match, actionList, FLOW_PRIORITY_NEXT_HOP, flowName);
    }

    /**
     * Internal util method used by the previously defined
     * configureNextHopFlow() methods.
     *
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     * @param flowPriority
     *            - the priority to set on the flow
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configureNextHopFlow(MatchBuilder match, List<Action> actionList, int flowPriority,
            String flowName) {
        LOG.debug("SfcProviderSffFlowWriter.configureNextHopFlow");

        // this apply actions has an 'order' of 0 - first one executed
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);
        SfcOpenflowUtils.appendGotoTableInstruction(isb, getTableId(TABLE_INDEX_TRANSPORT_EGRESS));

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_NEXT_HOP), flowPriority,
                flowName, match, isb);
    }

    //
    // Table 10, Transport Egress
    //

    @Override
    public void configureMacChainingSfTransportEgressFlow(final String sffNodeName, final String dstMac,
                                                          String port, final String vmac) {

        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchDstMac(match, dstMac);

        List<Action> actionList = new ArrayList<>();

        if (vmac != null) {
            // when packets are forwarded to other SFFs we write back the virtual MAC, because it was overwritten to
            // keep state between tables
            actionList.add(SfcOpenflowUtils.createActionSetDlDst(vmac, actionList.size()));
        }

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_TRANSPORT_EGRESS).add(dstMac).add(vmac).add(port);

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(
                match, actionList, port, FLOW_PRIORITY_TRANSPORT_EGRESS, TRANSPORT_EGRESS_MAC_CHAINING_COOKIE, flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    @Override
    public void configureVlanSfTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, String port, final long pathId, boolean doPktin) {
        // Match on the metadata pathId
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);
        // In order to set the IP DSCP, we need to match IPv4
        SfcOpenflowUtils.addMatchEtherType(match, SfcOpenflowUtils.ETHERTYPE_IPV4);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionWriteDscp((short) pathId, actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionPushVlan(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionSetVlanId(dstVlan, actionList.size()));

        if (doPktin) {
            // Notice TCP SYN matching is only supported in OpenFlow 1.5
            SfcOpenflowUtils.addMatchTcpSyn(match);
            actionList
                    .add(SfcOpenflowUtils.createActionPktIn(SfcOpenflowUtils.PKT_LENGTH_IP_HEADER, actionList.size()));
        }

        FlowBuilder transportEgressFlow = configureMacTransportEgressFlow(match, actionList, port, pathId, srcMac,
                dstMac, TRANSPORT_EGRESS_VLAN_SF_COOKIE);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Configure the VLAN LastHop Transport Egress flow by matching on the RSP
     * path ID in the metadata. The only difference between this method and
     * configureVlanTransportEgressFlow() is that this method checks for App
     * Coexistence.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the destination MAC to match on
     * @param dstVlan
     *            - the VLAN tag to write to the packet
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     */
    @Override
    public void configureVlanLastHopTransportEgressFlow(final String sffNodeName, final String srcMac,
            final String dstMac, final int dstVlan, final String port, final long pathId) {

        // App coexistence
        String switchPort = port;
        if (getTableEgress() > APP_COEXISTENCE_NOT_SET) {
            switchPort = EMPTY_SWITCH_PORT;
        }

        configureVlanTransportEgressFlow(sffNodeName, srcMac, dstMac, dstVlan, switchPort, pathId,
                TRANSPORT_EGRESS_VLAN_LASTHOP_COOKIE);
    }

    /**
     * Configure the VLAN Transport Egress flow by matching on the RSP path ID
     * in the metadata.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the destination MAC to match on
     * @param dstVlan
     *            - the VLAN tag to write to the packet
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     */
    @Override
    public void configureVlanTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, String port, final long pathId) {
        configureVlanTransportEgressFlow(sffNodeName, srcMac, dstMac, dstVlan, port, pathId,
                TRANSPORT_EGRESS_VLAN_COOKIE);
    }

    /**
     * Internal Util method used for above VLAN Transport Egress methods.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the destination MAC to match on
     * @param dstVlan
     *            - the VLAN tag to write to the packet
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     * @param cookieStr
     *            - The cookie to use
     */
    public void configureVlanTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final int dstVlan, String port, final long pathId, final String cookieStr) {

        // Match on the metadata pathId
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionPushVlan(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionSetVlanId(dstVlan, actionList.size()));

        FlowBuilder transportEgressFlow = configureMacTransportEgressFlow(match, actionList, port, pathId, srcMac,
                dstMac, cookieStr);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Configure the MPLS Last Hop Transport Egress flow by matching on the RSP
     * path ID in the metadata. The only difference between this method and
     * configureMplsTransportEgressFlow() is that this method checks for App
     * Coexistence.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the destination MAC to match on
     * @param mplsLabel
     *            - the MPLS label tag to write to the packet
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     */
    @Override
    public void configureMplsLastHopTransportEgressFlow(final String sffNodeName, final String srcMac,
            final String dstMac, final long mplsLabel, final String port, final long pathId) {

        // App coexistence
        String switchPort = port;
        if (getTableEgress() > APP_COEXISTENCE_NOT_SET) {
            switchPort = EMPTY_SWITCH_PORT;
        }

        configureMplsTransportEgressFlow(sffNodeName, srcMac, dstMac, mplsLabel, switchPort, pathId,
                TRANSPORT_EGRESS_MPLS_LASTHOP_COOKIE);
    }

    /**
     * Configure the MPLS Transport Egress flow by matching on the RSP path ID
     * in the metadata.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the destination MAC to match on
     * @param mplsLabel
     *            - the MPLS label tag to write to the packet
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     */
    @Override
    public void configureMplsTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, String port, final long pathId) {
        configureMplsTransportEgressFlow(sffNodeName, srcMac, dstMac, mplsLabel, port, pathId,
                TRANSPORT_EGRESS_MPLS_COOKIE);
    }

    /**
     * Internal Util method used for above MPLS Transport Egress methods.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the destination MAC to match on
     * @param mplsLabel
     *            - the mpls label to write to the packet
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     * @param cookieStr
     *            - the cookie to set on the flow
     */
    public void configureMplsTransportEgressFlow(final String sffNodeName, final String srcMac, final String dstMac,
            final long mplsLabel, String port, final long pathId, final String cookieStr) {
        // Match on the metadata pathId
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(pathId), METADATA_MASK_SFP_MATCH);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionPushMpls(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionSetMplsLabel(mplsLabel, actionList.size()));

        FlowBuilder transportEgressFlow = configureMacTransportEgressFlow(match, actionList, port, pathId, srcMac,
                dstMac, cookieStr);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Configure the VxLAN GPE NSH Transport Egress flow by matching on the NSP
     * and NSI.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     * @param port
     *            - the switch port to send the packet out on
     */
    @Override
    public void configureNshVxgpeLastHopTransportEgressFlow(final String sffNodeName, final long nshNsp,
            final short nshNsi, String port) {
        // When outputing to an outport, if inport==outport, then according to
        // the openflow spec, the packet will be dropped. To avoid this, outport
        // must be set to INPORT. This method writes 2 flows to avoid this
        // situation:
        // flow1:
        // if inport==port, actions=output:INPORT (higher priority than flow2)
        // flow2:
        // actions=output:port (flow2 is basically the else condition)

        Long vxgpePort = SfcOvsUtil.getVxlanGpeOfPort(sffNodeName);
        String inportStr = OutputPortValues.INPORT.toString();

        if (vxgpePort != null) {
            String vxgpePortStr = OUTPUT_STRING + vxgpePort.toString();
            configureNshVxgpeLastHopTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, vxgpePortStr, inportStr);
            configureNshVxgpeLastHopTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, inportStr, vxgpePortStr);
        } else {
            configureNshVxgpeLastHopTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, port, port);
        }
    }

    /**
     * Simple call through for configureNshVxgpeLastHopTransportEgressFlow().
     */
    private void configureNshVxgpeLastHopTransportEgressFlowPorts(final String sffNodeName, final long nshNsp,
            final short nshNsi, String inport, String outport) {
        int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;
        String theOutPortToSet = outport;
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);

        if (!inport.startsWith(OutputPortValues.INPORT.toString())) {
            // if we output to a port that's the same as the import, the packet
            // will be dropped
            SfcOpenflowUtils.addMatchInPort(match, new NodeConnectorId(inport));
            theOutPortToSet = OutputPortValues.INPORT.toString();
            flowPriority += 5;
        }

        // On the last hop Copy/Move Nsi, Nsp, Nsc1=>TunIpv4Dst, and
        // Nsc2=>TunId(Vnid)
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshNp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsi(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1ToTunIpv4DstRegister(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2ToTunIdRegister(actionList.size()));

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        actionList
                .add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(OpenflowConstants.TUN_GPE_NP_NSH, actionList.size()));

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_TRANSPORT_EGRESS)
            .add(String.valueOf(nshNsi)).add(String.valueOf(nshNsp)).add(inport).add(outport);

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(match, actionList, theOutPortToSet, flowPriority,
                TRANSPORT_EGRESS_NSH_VXGPE_LASTHOP_COOKIE, flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Configure the last hop VxLAN GPE NSH Transport Egress flow by matching
     * on the NSP and NSI, an optionally NSH C1 not set. Assume a different
     * local service will be able to handle the encapsulated packet. This
     * service is assumed to provide connectivity from the last SF to the
     * destination.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param sfMacAddress
     *            - the SF mac address
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     */
    private void configureNshEthLastHopTransportEgressToPipelineService(final String sffNodeName,
                                                                        final MacAddress sfMacAddress,
                                                                        final long nshNsp, final short nshNsi) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);
        SfcOpenflowUtils.addMatchNshNsc1(match, 0L);

        // On the last hop:
        // 1. remove nsh header
        // 2. Change src mac to the mac of the last SF
        // 3. resubmit to the ingress dispatcher table
        List<Action> actionList = new ArrayList<>();

        // Pop NSH
        actionList.add(SfcOpenflowUtils.createActionNxPopNsh(actionList.size()));

        // Change source mac address
        actionList.add(SfcOpenflowUtils.createActionSetDlSrc(sfMacAddress.getValue(), actionList.size()));

        // Proceed with other services
        actionList.add(SfcOpenflowUtils.createActionResubmitTable(
                NwConstants.LPORT_DISPATCHER_TABLE, actionList.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER)
                .add(FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_PIPELINE)
                .add(String.valueOf(nshNsi))
                .add(String.valueOf(nshNsp))
                .add(sfMacAddress.getValue());

        // Make the cookie
        BigInteger cookie = new BigInteger(
                TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_PIPELINE_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);

        // Create and return the flow
        FlowBuilder fb = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                FLOW_PRIORITY_TRANSPORT_EGRESS_PIPELINE, cookie, flowName.toString(), match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, fb);
    }

    @Override
    public void configureNshEthLastHopTransportEgressFlow(final String sffNodeName, final IpAddress sffIpAddress,
                                                          final MacAddress sfMacAddress, final long nshNsp,
                                                          final short nshNsi) {
        final Long vxgpePort = SfcOvsUtil.getVxlanGpeOfPort(sffNodeName);
        if (vxgpePort != null) {
            // If a VXLAN GPE openflow based port is available,
            // support egress scenarios that forward the traffic
            // to remote locations via IP provided through NSH C1.
            final String vxgpePortStr = OUTPUT_STRING + vxgpePort.toString();
            configureNshEthLastHopTransportEgressToRemoteTunnelService(sffNodeName, nshNsp, nshNsi, vxgpePortStr);
            configureNshEthLastHopTransportEgressToRemoteNshService(sffNodeName, nshNsp, nshNsi, vxgpePortStr);
        }

        final String inputSffIpAddress = Optional.ofNullable(sffIpAddress)
                .map(IpAddress::getIpv4Address)
                .map(Ipv4Address::getValue)
                .orElse(null);
        final boolean hasTunnelEndpoint = inputSffIpAddress != null && !inputSffIpAddress.isEmpty();
        final String usedSffIpAddress = hasTunnelEndpoint ? inputSffIpAddress : LOCALHOST_IP;
        configureNshEthLastHopTransportEgressToLocalTunnelService(sffNodeName, usedSffIpAddress, nshNsp, nshNsi);
        configureNshEthLastHopTransportEgressToLocalNshService(sffNodeName, usedSffIpAddress, nshNsp, nshNsi);

        // Egress based on connectivity between last SF and destination
        // provided by a different service
        configureNshEthLastHopTransportEgressToPipelineService(sffNodeName, sfMacAddress, nshNsp, nshNsi);
    }

    /**
     * Configure the last hop VxLAN GPE NSH Transport Egress flow by matching
     * on the NSP and NSI with both NSH C1, C2 set. In such a case, assume a
     * different remote service will be able to handle the encapsulated packet.
     * This service will use the tunnel id metadata obtained from C2 to handle
     * it.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     * @param outport
     *            - the outport to output on
     */
    private void configureNshEthLastHopTransportEgressToRemoteTunnelService(final String sffNodeName, final long nshNsp,
                                                                            final short nshNsi, final String outport) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);

        // On the last hop:
        // 1. Write C1 to Ipv4TunDst
        // 2. Write C2 to Vnid
        // 3. remove nsh header
        // 4. Egress to the specified port
        List<Action> actionList = new ArrayList<>();

        // Write C1 to Ipv4TunDst
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1ToTunIpv4DstRegister(actionList.size()));

        // Write C2 to Vnid
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2ToTunIdRegister(actionList.size()));

        // Pop NSH
        actionList.add(SfcOpenflowUtils.createActionNxPopNsh(actionList.size()));

        // Egress to port
        actionList.add(SfcOpenflowUtils.createActionOutPort(outport, actionList.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER)
                .add(FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_TUNNEL_REMOTE)
                .add(String.valueOf(nshNsi))
                .add(String.valueOf(nshNsp))
                .add(outport);

        // Make the cookie
        BigInteger cookie = new BigInteger(
                TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_TUNNEL_REMOTE_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);

        // Create and return the flow
        FlowBuilder fb = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                FLOW_PRIORITY_TRANSPORT_EGRESS_TUNNEL_REMOTE, cookie, flowName.toString(), match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, fb);
    }

    /**
     * As {@link #configureNshEthLastHopTransportEgressToRemoteTunnelService}
     * but handle the special case that such remote service is not really
     * remote, but local.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param sffIpAddress
     *            - the SFF IP address
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     */
    private void configureNshEthLastHopTransportEgressToLocalTunnelService(final String sffNodeName,
                                                                           final String sffIpAddress,
                                                                           final long nshNsp, final short nshNsi) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);

        // Create a match checking if C1 is set to this SFF
        // Assuming IPv4
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(sffIpAddress));
        long ipl = ip & 0xffffffffL;
        SfcOpenflowUtils.addMatchNshNsc1(match, ipl);

        // On the last hop:
        // 1. Write C1 to Ipv4TunDst
        // 2. Write C2 to Vnid
        // 4. Resubmit to internal tunnel table
        List<Action> actionList = new ArrayList<>();

        // Write C1 to Ipv4TunDst
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1ToTunIpv4DstRegister(actionList.size()));

        // Write C2 to Vnid
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2ToTunIdRegister(actionList.size()));

        // Pop NSH
        actionList.add(SfcOpenflowUtils.createActionNxPopNsh(actionList.size()));

        // Resubmit to tunnel table
        actionList.add(SfcOpenflowUtils.createActionResubmitTable(
                NwConstants.INTERNAL_TUNNEL_TABLE, actionList.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER)
                .add(FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_TUNNEL_LOCAL)
                .add(String.valueOf(nshNsi))
                .add(String.valueOf(nshNsp))
                .add(sffIpAddress);

        // Make the cookie
        BigInteger cookie = new BigInteger(
                TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_TUNNEL_LOCAL_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);

        // Create and return the flow
        FlowBuilder fb = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                FLOW_PRIORITY_TRANSPORT_EGRESS_TUNNEL_LOCAL, cookie, flowName.toString(), match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, fb);
    }

    /**
     * As {@link #configureNshEthLastHopTransportEgressToRemoteNshService}
     * but handle the special case that such remote service is not really
     * remote, but local.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param sffIpAddress
     *            - the SFF IP address
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     */
    private void configureNshEthLastHopTransportEgressToLocalNshService(final String sffNodeName,
                                                                        final String sffIpAddress,
                                                                        final long nshNsp, final short nshNsi) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);

        // Create a match checking if C1 is set to this SFF
        // Assuming IPv4
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(sffIpAddress));
        long ipl = ip & 0xffffffffL;
        SfcOpenflowUtils.addMatchNshNsc1(match, ipl);
        SfcOpenflowUtils.addMatchNshNsc2(match, 0L);

        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionResubmitTable(
                NwConstants.LPORT_DISPATCHER_TABLE, actionList.size()));
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER)
                .add(FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_NSH_LOCAL)
                .add(String.valueOf(nshNsi))
                .add(String.valueOf(nshNsp))
                .add(sffIpAddress);

        // Make the cookie
        BigInteger cookie = new BigInteger(
                TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_NSH_LOCAL_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);

        // Create and return the flow
        FlowBuilder fb = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                FLOW_PRIORITY_TRANSPORT_EGRESS_NSH_LOCAL, cookie, flowName.toString(), match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, fb);
    }

    /**
     * Configure the last hop VxLAN GPE NSH Transport Egress flow by matching
     * on the NSP and NSI with both NSH C1 but no C2 set. In such a case,
     * assume a different remote service will be able to handle the NSH packet.
     * This service will use the remaining NSH metadata to handle the
     * encapsulated packet.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     * @param outport
     *            - the outport to output on
     */
    private void configureNshEthLastHopTransportEgressToRemoteNshService(final String sffNodeName, final long nshNsp,
                                                                         final short nshNsi, final String outport) {
        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);
        SfcOpenflowUtils.addMatchNshNsc2(match, 0L);

        // On the last hop:
        // 1. Write C1 to Ipv4TunDst
        // 2. Write C2 to Vnid
        // 4. Egress to the specified port
        List<Action> actionList = new ArrayList<>();

        // Write C1 to Ipv4TunDst
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1ToTunIpv4DstRegister(actionList.size()));

        // Write C2 to Vnid
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2ToTunIdRegister(actionList.size()));

        // Egress to port
        actionList.add(SfcOpenflowUtils.createActionOutPort(outport, actionList.size()));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER)
                .add(FLOW_NAME_LASTHOP_TRANSPORT_EGRESS_NSH_REMOTE)
                .add(String.valueOf(nshNsi))
                .add(String.valueOf(nshNsp))
                .add(outport);

        // Make the cookie
        BigInteger cookie = new BigInteger(
                TRANSPORT_EGRESS_COOKIE_STR_BASE + TRANSPORT_EGRESS_NSH_ETH_LASTHOP_NSH_REMOTE_COOKIE,
                COOKIE_BIGINT_HEX_RADIX);

        // Create and return the flow
        FlowBuilder fb = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_EGRESS),
                FLOW_PRIORITY_TRANSPORT_EGRESS_NSH_REMOTE, cookie, flowName.toString(), match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, fb);
    }

    @Override
    public void configureNshVxgpeTransportEgressFlow(final String sffNodeName, final long nshNsp, final short nshNsi,
            String port) {

        // When outputing to an outport, if inport==outport, then according to
        // the openflow spec, the packet will be dropped. To avoid this, outport
        // must be set to INPORT. This method writes 2 flows to avoid this
        // situation:
        // flow1:
        // if inport==port, actions=output:INPORT (higher priority than flow2)
        // flow2:
        // actions=output:port (flow2 is basically the else condition)

        Long vxgpePort = SfcOvsUtil.getVxlanGpeOfPort(sffNodeName);
        String inportStr = OutputPortValues.INPORT.toString();

        if (vxgpePort != null) {
            String vxgpePortStr = OUTPUT_STRING + vxgpePort.toString();
            configureNshVxgpeTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, vxgpePortStr, inportStr);
            configureNshVxgpeTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, inportStr, vxgpePortStr);
        } else {
            configureNshVxgpeTransportEgressFlowPorts(sffNodeName, nshNsp, nshNsi, port, port);
        }
    }

    /**
     * Simple call through for configureNshVxgpeTransportEgressFlow().
     */
    private void configureNshVxgpeTransportEgressFlowPorts(final String sffNodeName, final long nshNsp,
            final short nshNsi, String inport, String outport) {
        int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;
        String theOutPortToSet = outport;

        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);
        if (!inport.startsWith(OutputPortValues.INPORT.toString())) {
            // if we output to a port that's the same as the import, the packet
            // will be dropped
            SfcOpenflowUtils.addMatchInPort(match, new NodeConnectorId(inport));
            theOutPortToSet = OutputPortValues.INPORT.toString();
            flowPriority += 5;
        }

        List<Action> actionList = new ArrayList<>();
        // Copy/Move Nsc1/Nsc2 to the next hop
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshNp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc3(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc4(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveTunIdRegister(actionList.size()));

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        actionList
                .add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(OpenflowConstants.TUN_GPE_NP_NSH, actionList.size()));

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_TRANSPORT_EGRESS)
            .add(String.valueOf(nshNsi)).add(String.valueOf(nshNsp)).add(inport).add(outport);

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(match, actionList, theOutPortToSet, flowPriority,
                TRANSPORT_EGRESS_NSH_VXGPE_COOKIE, flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * For NSH, Return the packet to INPORT if the NSH Nsc1 Register is not
     * present (==0) If it is present, it will be handled by the flow created in
     * ConfigureTransportEgressFlowThread() This flow will have a higher
     * priority than the flow created in ConfigureTransportEgressFlowThread().
     * The NSH Nsc1 Register is usually set by the ingress classifier to
     * communicate how to reach the egress classifier.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     * @param port
     *            - the switch port to send the packet out on
     */
    @Override
    public void configureNshNscTransportEgressFlow(final String sffNodeName, final long nshNsp, final short nshNsi,
            String port) {
        LOG.debug("ConfigureNshNscTransportEgressFlowThread, sff [{}] nsp [{}] nsi [{}] port [{}]", sffNodeName, nshNsp,
                nshNsi, port);

        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);
        SfcOpenflowUtils.addMatchNshNsc1(match, 0L);

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        List<Action> actionList = new ArrayList<>();
        actionList
                .add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(OpenflowConstants.TUN_GPE_NP_NSH, actionList.size()));

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_TRANSPORT_EGRESS).add(String.valueOf(nshNsi)).add(String.valueOf(nshNsp)).add(port);

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(match, actionList, port,
                FLOW_PRIORITY_TRANSPORT_EGRESS + 10, TRANSPORT_EGRESS_NSH_VXGPE_NSC_COOKIE, flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    @Override
    public void configureNshVxgpeAppCoexistTransportEgressFlow(final String sffNodeName, final long nshNsp,
            final short nshNsi, final String sffIp) {

        // This flow only needs to be created if App Coexistence is being used
        if (getTableEgress() == APP_COEXISTENCE_NOT_SET) {
            LOG.debug("configureNshVxgpeAppCoexistTransportEgressFlow NO AppCoexistence configured, skipping flow");
            return;
        }

        // Create a match checking if C1 is set to this SFF
        // Assuming IPv4
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(sffIp));
        long ipl = ip & 0xffffffffL;

        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);
        SfcOpenflowUtils.addMatchNshNsc1(match, ipl);

        // Copy/Move Nsi, Nsp, Nsc1=>TunIpv4Dst, and Nsc2=>TunId(Vnid)
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshNp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsi(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1ToTunIpv4DstRegister(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2ToTunIdRegister(actionList.size()));

        /* Need to set TUN_GPE_NP for VxLAN-gpe port */
        actionList
                .add(SfcOpenflowUtils.createActionNxLoadTunGpeNp(OpenflowConstants.TUN_GPE_NP_NSH, actionList.size()));

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_APPCOEXIST_TRANSPORT_EGRESS)
            .add(String.valueOf(nshNsi)).add(String.valueOf(nshNsp)).add(sffIp);

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(match, actionList, EMPTY_SWITCH_PORT,
                FLOW_PRIORITY_TRANSPORT_EGRESS + 10, TRANSPORT_EGRESS_NSH_VXGPE_APPCOEXIST_COOKIE, flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    /**
     * Configure the NSH Ethernet Transport Egress flow by matching on the NSP
     * and NSI.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     * @param nshNsp
     *            - the NSH Service Path to match on
     * @param nshNsi
     *            - the NSH Service Index to match on
     * @param port
     *            - the switch port to send the packet out on
     */
    @Override
    public void configureNshEthTransportEgressFlow(String sffNodeName, long nshNsp, short nshNsi, String port) {
        List<Action> actionList = new ArrayList<>();
        // Copy/Move Nsc1/Nsc2/Nsi/Nsp to the next hop
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc1(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsc2(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsi(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNsp(actionList.size()));
        actionList.add(SfcOpenflowUtils.createActionNxMoveNshMdtype(actionList.size()));

        // Dont need to set Ethernet EtherType

        // Set NSH NextProtocol to Ethernet
        actionList.add(SfcOpenflowUtils.createActionNxLoadNshNp(OpenflowConstants.NSH_NP_ETH, actionList.size()));

        // Ethernet encap is performed in configureNshEthNextHopFlow()
        // while setting the next hop outer MAC addresses

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_TRANSPORT_EGRESS).add(String.valueOf(nshNsi)).add(String.valueOf(nshNsp)).add(port);

        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);

        FlowBuilder transportEgressFlow = configureTransportEgressFlow(match, actionList, port,
                FLOW_PRIORITY_TRANSPORT_EGRESS, TRANSPORT_EGRESS_NSH_ETH_COOKIE, flowName.toString());
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, transportEgressFlow);
    }

    @Override
    public void configureNshEthTransportEgressFlow(String sffOpenflowNodeName, long nsp, short nsi,
            List<Action> actionList, String flowName) {

        configureTransportEgressFlow(sffOpenflowNodeName, nsp, nsi, actionList, FLOW_PRIORITY_TRANSPORT_EGRESS,
                TRANSPORT_EGRESS_NSH_ETH_LOGICAL_COOKIE, flowName);
    }

    @Override
    public void setTableIndexMapper(SfcTableIndexMapper tableIndexMapper) {
        this.tableIndexMapper = tableIndexMapper;
    }

    /**
     * Simple pass through with logic for src/dstMac.
     *
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     * @param port
     *            - the switch port to send the packet out on
     * @param pathId
     *            - the RSP path id to match on
     * @param srcMac
     *            - the source MAC to write to the packet
     * @param dstMac
     *            - the dest MAC to match against
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configureMacTransportEgressFlow(MatchBuilder match, List<Action> actionList, String port,
            final long pathId, final String srcMac, final String dstMac, String cookieStr) {

        // Optionally match on the dstMac
        int flowPriority = FLOW_PRIORITY_TRANSPORT_EGRESS;
        if (dstMac != null) {
            SfcOpenflowUtils.addMatchDstMac(match, dstMac);
            // If the dstMac is null, then the packet is leaving SFC and
            // we dont know to where. Make it a lower priority, and only
            // match on the pathId
            flowPriority += 10;
        }

        // Set the macSrc
        if (srcMac != null) {
            actionList.add(SfcOpenflowUtils.createActionSetDlSrc(srcMac, actionList.size()));
        }

        StringJoiner flowName = new StringJoiner(FLOW_NAME_DELIMITER);
        flowName.add(FLOW_NAME_TRANSPORT_EGRESS).add(port).add(String.valueOf(pathId)).add(srcMac).add(dstMac);

        return configureTransportEgressFlow(match, actionList, port, flowPriority, cookieStr, flowName.toString());
    }

    /**
     * Internal util method used by the previously defined
     * configureTransportEgressFlow() methods.
     *
     * @param match
     *            -already created matches
     * @param actionList
     *            - a list of actions already created
     * @param port
     *            - the switch port to send the packet out on
     * @param flowPriority
     *            - the priority to set on the flow
     *
     * @return a FlowBuilder with the created Path Mapper flow
     */
    private FlowBuilder configureTransportEgressFlow(MatchBuilder match, List<Action> actionList, String port,
            int flowPriority, String cookieStr, String flowName) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportEgressFlow");

        if (port.equals(EMPTY_SWITCH_PORT) && getTableEgress() > APP_COEXISTENCE_NOT_SET) {
            // Application Coexistence:
            // Instead of egressing the packet out a port, send it to
            // a different application pipeline on this same switch
            actionList.add(SfcOpenflowUtils.createActionResubmitTable(getTableEgress(), actionList.size()));

        } else {
            actionList.add(SfcOpenflowUtils.createActionOutPort(port, actionList.size()));
        }

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Make the cookie
        BigInteger cookie = new BigInteger(TRANSPORT_EGRESS_COOKIE_STR_BASE + cookieStr, COOKIE_BIGINT_HEX_RADIX);

        // Create and return the flow
        return SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_EGRESS), flowPriority, cookie,
                flowName, match, isb);
    }

    private void configureTransportEgressFlow(String openflowName, long nshNsp, short nshNsi, List<Action> actionList,
            int flowPriority, String cookieStr, String flowName) {

        LOG.debug("configureTransportEgressFlow:starting. ofname:{} instructions: {}", openflowName, actionList);

        MatchBuilder match = SfcOpenflowUtils.getNshMatches(nshNsp, nshNsi);
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Make the cookie
        BigInteger cookie = new BigInteger(TRANSPORT_EGRESS_COOKIE_STR_BASE + cookieStr, COOKIE_BIGINT_HEX_RADIX);

        FlowBuilder transportEgressFlowBuilder = SfcOpenflowUtils.createFlowBuilder(
                getTableId(TABLE_INDEX_TRANSPORT_EGRESS), flowPriority, cookie, flowName, match, isb);
        sfcOfFlowWriter.writeFlow(flowRspId, openflowName, transportEgressFlowBuilder);
    }

    @Override
    public void configureGroup(String sffNodeName, String openflowNodeId, String sfgName, long sfgId, int groupType,
            List<GroupBucketInfo> bucketInfos, boolean isAddGroup) {

        LOG.debug("configuring group: sffName {}, groupName {}, ofNodeId {}, id {}, type {}", sffNodeName, sfgName,
                openflowNodeId, sfgId, groupType);
        GroupBuilder gb = new GroupBuilder();
        gb.setBarrier(true);
        gb.setGroupType(GroupTypes.forValue(groupType));
        gb.setGroupName(sfgName);
        gb.setGroupId(new GroupId(sfgId));

        List<Bucket> buckets = new ArrayList<>();
        BucketBuilder bb = new BucketBuilder();
        for (GroupBucketInfo bucketInfo : bucketInfos) {
            LOG.debug("building bucket {}", bucketInfo);
            buckets.add(buildBucket(bb, bucketInfo));
        }
        BucketsBuilder bbs = new BucketsBuilder();
        bbs.setBucket(buckets);
        gb.setBuckets(bbs.build());
        String nodeName = openflowNodeId != null ? openflowNodeId : sffNodeName;
        sfcOfFlowWriter.writeGroupToDataStore(nodeName, gb, isAddGroup);
    }

    private Bucket buildBucket(BucketBuilder bb, GroupBucketInfo bucketInfo) {
        int order = 0;
        BucketId bucketId = new BucketId((long) bucketInfo.getIndex());
        bb.setBucketId(bucketId);
        bb.setKey(new BucketKey(bucketId));
        String sfMac = bucketInfo.getSfMac();
        String sfIp = bucketInfo.getSfIp();
        List<Action> actionList = new ArrayList<>();
        if (sfMac != null) {
            // Set the DL (Data Link) Dest Mac Address
            Action actionDstMac = SfcOpenflowUtils.createActionSetDlDst(sfMac, order);
            order++;
            actionList.add(actionDstMac);
        }

        if (sfIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionSetNwDst(sfIp, 32, order);
            order++;
            actionList.add(actionSetNwDst);
        }
        Action actionOutPort = SfcOpenflowUtils.createActionOutPort(bucketInfo.getOutPort(), order);
        actionList.add(actionOutPort);

        bb.setAction(actionList);
        return bb.build();
    }

    @Override
    public void configureGroupNextHopFlow(String sffNodeName, long sfpId, String srcMac, long groupId,
            String groupName) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureGroupNextHopFlow sffName [{}] sfpId [{}] srcMac [{}] groupId[{}]",
                sffNodeName, sfpId, srcMac, groupId);

        int flowPriority = FLOW_PRIORITY_NEXT_HOP;

        //
        // Create the matching criteria
        MatchBuilder match = new MatchBuilder();

        // Match on the either the metadata sfpId or the NSH NSP and NSI
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(sfpId), METADATA_MASK_SFP_MATCH);

        // match on the src mac
        if (srcMac != null) {
            SfcOpenflowUtils.addMatchSrcMac(match, srcMac);
        } else {
            // If the srcMac is null, then the packet is entering SFC and we
            // dont know
            // from where. Make it a lower priority, and only match on the
            // pathId
            flowPriority -= 10;
        }

        GroupActionBuilder groupActionBuilder = new GroupActionBuilder();
        groupActionBuilder.setGroupId(groupId);
        groupActionBuilder.setGroup(groupName);

        GroupActionCaseBuilder groupActionCaseBuilder = new GroupActionCaseBuilder();
        groupActionCaseBuilder.setGroupAction(groupActionBuilder.build());

        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setAction(groupActionCaseBuilder.build());
        actionBuilder.setOrder(0);
        actionBuilder.setKey(new ActionKey(0));
        Action groupAction = actionBuilder.build();

        // Create the Actions
        List<Action> actionList = new ArrayList<>();
        actionList.add(groupAction);

        // Create an Apply Action
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        FlowBuilder nextHopFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_NEXT_HOP), flowPriority,
                FLOW_NAME_NEXT_HOP, match, isb);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

    private static BigInteger getMetadataSFP(long sfpId) {
        return BigInteger.valueOf(sfpId).and(METADATA_MASK_SFP_MATCH);
    }

    /**
     * getTableId Having a TableBase allows us to "offset" the SFF tables by
     * this.tableBase tables. This is used for App Coexistence. When a
     * {@link SfcTableIndexMapper} has been provided, it is used (this is
     * another way of performing App coexistence)
     *
     * @param tableIndex
     *            - the table to offset
     * @return the resulting table id
     */
    private short getTableId(short tableIndex) {

        // A transport processor can provide a table index mapper in order
        // to retrieve table positions
        if (tableIndexMapper != null && tableIndexMapper.getTableIndex(tableIndex).isPresent()) {
            return tableIndexMapper.getTableIndex(tableIndex).get();
        }

        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // App Coexistence
            if (tableIndex == TABLE_INDEX_TRANSPORT_INGRESS) {
                // With AppCoexistence the TransportIngress table is now table 0
                return 0;
            } else {
                // Need to subtract 2 to compensate for:
                // - TABLE_INDEX_CLASSIFIER=0 - which is not used for
                // AppCoexistence
                // - TABLE_INDEX_TRANSPORT_INGRESS=1 - which is table 0 for
                // AppCoexistence
                // Example: tableBase=20, TABLE_INDEX_PATH_MAPPER=2, should
                // return 20
                return (short) (getTableBase() + tableIndex - 2);
            }
        } else {
            return tableIndex;
        }
    }
}
