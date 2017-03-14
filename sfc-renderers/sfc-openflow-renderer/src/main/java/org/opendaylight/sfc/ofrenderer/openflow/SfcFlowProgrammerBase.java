/*
 * Copyright (c) 2014, 2017 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.openflow;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.sfc.genius.util.appcoexistence.SfcTableIndexMapper;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interface to be implemented by concrete classes that will write to
 * OpenFlow or OVS switches.
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @since 2015-02-24
 */
public abstract class SfcFlowProgrammerBase {

    public static final short TABLE_INDEX_CLASSIFIER = 0;
    public static final short TABLE_INDEX_TRANSPORT_INGRESS = 1;
    public static final short TABLE_INDEX_PATH_MAPPER = 2;
    public static final short TABLE_INDEX_PATH_MAPPER_ACL = 3;
    public static final short TABLE_INDEX_NEXT_HOP = 4;
    public static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;
    public static final short TABLE_INDEX_MAX_OFFSET = TABLE_INDEX_TRANSPORT_EGRESS;

    protected static final int FLOW_PRIORITY_TRANSPORT_INGRESS = 250;
    protected static final int FLOW_PRIORITY_ARP_TRANSPORT_INGRESS = 300;
    protected static final int FLOW_PRIORITY_PATH_MAPPER = 350;
    protected static final int FLOW_PRIORITY_PATH_MAPPER_ACL = 450;
    protected static final int FLOW_PRIORITY_NEXT_HOP = 550;
    protected static final int FLOW_PRIORITY_TRANSPORT_EGRESS = 650;
    protected static final int FLOW_PRIORITY_MATCH_ANY = 5;
    protected static final int FLOW_PRIORITY_CLASSIFIER = 1000;
    protected static final int COOKIE_BIGINT_HEX_RADIX = 16;
    protected static final short APP_COEXISTENCE_NOT_SET = -1;
    protected static final String NEXT_HOP_FLOW_NAME_LITERAL = "nextHop";
    protected static final String EMPTY_SWITCH_PORT = "";

    // A common SFC Transport Egress Cookie Base String, allowing
    // all SFC flows to be matched by using cookieStr.startsWith()
    protected static final String TRANSPORT_EGRESS_COOKIE_STR_BASE = "BA5EBA11";
    // The 000001** cookies are for NSH VXGPE Transport Egress flows
    protected static final String TRANSPORT_EGRESS_NSH_VXGPE_COOKIE = "00000101";
    protected static final String TRANSPORT_EGRESS_NSH_VXGPE_NSC_COOKIE = "00000102";
    protected static final String TRANSPORT_EGRESS_NSH_VXGPE_LASTHOP_COOKIE = "00000103";
    protected static final String TRANSPORT_EGRESS_NSH_VXGPE_APPCOEXIST_COOKIE = "00000104";
    // The 000002** cookies are for NSH Eth Transport Egress flows
    protected static final String TRANSPORT_EGRESS_NSH_ETH_COOKIE = "00000201";
    protected static final String TRANSPORT_EGRESS_NSH_ETH_LOGICAL_COOKIE = "00000202";
    protected static final String TRANSPORT_EGRESS_NSH_ETH_LASTHOP_COOKIE = "00000203";
    // The 000003** cookies are for VXGEP NSH Transport Egress flows
    protected static final String TRANSPORT_EGRESS_VLAN_COOKIE = "00000301";
    protected static final String TRANSPORT_EGRESS_VLAN_SF_COOKIE = "00000302";
    protected static final String TRANSPORT_EGRESS_VLAN_LASTHOP_COOKIE = "00000303";
    // The 000004** cookies are for VXGEP NSH Transport Egress flows
    protected static final String TRANSPORT_EGRESS_MPLS_COOKIE = "00000401";
    protected static final String TRANSPORT_EGRESS_MPLS_LASTHOP_COOKIE = "00000402";
    protected static final String TRANSPORT_EGRESS_MAX_COOKIE = "00000FFF";

    // Which bits in the metadata field to set, Assuming 4095 PathId's
    protected static final BigInteger METADATA_MASK_SFP_MATCH = new BigInteger("FFFFFFFFFFFFFFFF",
            COOKIE_BIGINT_HEX_RADIX);

    protected static final Logger LOG = LoggerFactory.getLogger(SfcFlowProgrammerBase.class);

    protected Long flowRspId;
    protected SfcOfFlowWriterInterface sfcOfFlowWriter;
    protected SfcOpenFlowConfig openFlowConfig;

    public SfcFlowProgrammerBase() {
        this.flowRspId = new Long(0);
        this.sfcOfFlowWriter = null;
        this.openFlowConfig = null;
    }

    public SfcFlowProgrammerBase(SfcOfFlowWriterInterface sfcOfFlowWriter, SfcOpenFlowConfig openFlowConfig) {
        this.flowRspId = 0L;
        this.sfcOfFlowWriter = sfcOfFlowWriter;
        this.openFlowConfig = openFlowConfig;
    }

    public static SfcOpenFlowConfig createDefaultSfcOpenFlowConfig() {
        SfcOpenFlowConfig config = new SfcOpenFlowConfig(TABLE_INDEX_MAX_OFFSET);
        config.setTableBase(APP_COEXISTENCE_NOT_SET);
        config.setTableEgress(APP_COEXISTENCE_NOT_SET);

        return config;
    }

    //
    // Getters/Setters
    //

    // These table methods are used for app-coexistence

    public short getTableBase() {
        return openFlowConfig.getTableBase();
    }

    public void setTableBase(short tableBase) {
        this.openFlowConfig.setTableBase(tableBase);
    }

    public short getTableEgress() {
        return openFlowConfig.getTableEgress();
    }

    public void setTableEgress(short tableEgress) {
        this.openFlowConfig.setTableEgress(tableEgress);
    }

    public short getMaxTableOffset() {
        return openFlowConfig.getMaxTableOffset();
    }

    public void setTableIndexMapper(SfcTableIndexMapper tableIndexMapper) {
        this.openFlowConfig.setTableIndexMapper(tableIndexMapper);
    }

    public void setOpenFlowConfig(SfcOpenFlowConfig openFlowConfig) {
        this.openFlowConfig = openFlowConfig;
    }

    public SfcOpenFlowConfig getOpenFlowConfig() {
        return this.openFlowConfig;
    }

    // Set the RSP Id that subsequent flow creations belong to
    public void setFlowRspId(Long rspId) {
        this.flowRspId = rspId;
    }

    protected static BigInteger getMetadataSFP(long sfpId) {
        return BigInteger.valueOf(sfpId).and(METADATA_MASK_SFP_MATCH);
    }

    /**
     * Deletes all flows created for a particular RSP and removes initialization
     * flows from SFFs if the last RSP was removed.
     *
     * @param rspId
     *            the id of the RSP to be deleted
     *
     * @return Node IDs from which initialization flows were removed.
     */
    public Set<NodeId> deleteRspFlows(final long rspId) {
        sfcOfFlowWriter.deleteRspFlows(rspId);
        Set<NodeId> nodes = sfcOfFlowWriter.clearSffsIfNoRspExists();
        sfcOfFlowWriter.deleteFlowSet();
        return nodes;
    }

    // Write any buffered flows to the data store
    public void flushFlows() {
        this.sfcOfFlowWriter.flushFlows();
    }

    // Purge any unwritten flows not written yet. This should be called upon
    // errors, when the remaining buffered flows should not be written.
    public void purgeFlows() {
        this.sfcOfFlowWriter.purgeFlows();
    }

    // Set FlowWriter implementation
    public void setFlowWriter(SfcOfFlowWriterInterface sfcOfFlowWriter) {
        this.sfcOfFlowWriter = sfcOfFlowWriter;
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
        if (cookie == null || cookie.getValue() == null) {
            return false;
        }

        return cookie.toString().toUpperCase().startsWith(TRANSPORT_EGRESS_COOKIE_STR_BASE);
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
    protected short getTableId(short tableIndex) {

        // A transport processor can provide a table index mapper in order
        // to retrieve table positions
        SfcTableIndexMapper tableIndexMapper = this.openFlowConfig.getTableIndexMapper();
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
    public void configureClassifierTableMatchAny(final String sffNodeName) {
        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We dont need this flow with App Coexistence.
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
    public void configureClassifierTableDpdkOutput(final String sffNodeName, Long outPort) {
        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We dont need this flow with App Coexistence.
            return;
        }

        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchInPort(match, new NodeConnectorId(OutputPortValues.LOCAL.toString()));

        int order = 0;

        // Action output
        List<Action> actionList = new ArrayList<>();
        String outPortStr = "output:" + outPort.toString();
        actionList.add(SfcOpenflowUtils.createActionOutPort(outPortStr, order++));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        FlowBuilder classifierDpdkOutputFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_CLASSIFIER),
                FLOW_PRIORITY_CLASSIFIER, "classifier_dpdk_output", match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, classifierDpdkOutputFlow);
    }

    /**
     * Set DPDK input flow in the Classifier table for OVS DPDK.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    public void configureClassifierTableDpdkInput(final String sffNodeName, Long inPort) {
        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We dont need this flow with App Coexistence.
            return;
        }

        // Create the match criteria
        MatchBuilder match = new MatchBuilder();
        SfcOpenflowUtils.addMatchInPort(match, new NodeId(sffNodeName), inPort);

        int order = 0;

        // Action NORMAL
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionNormal(order++));

        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        FlowBuilder classifierDpdkInputFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_CLASSIFIER),
                FLOW_PRIORITY_CLASSIFIER, "classifier_dpdk_input", match, isb);

        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, classifierDpdkInputFlow);
    }

    /**
     * Set the match any flow in the Transport Ingress table to drop.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
    public void configureTransportIngressTableMatchAny(final String sffNodeName) {
        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // We dont need this flow with App Coexistence.
            return;
        }

        FlowBuilder flowBuilder = configureTableMatchAnyDropFlow(getTableId(TABLE_INDEX_TRANSPORT_INGRESS));
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flowBuilder);
    }

    /**
     * Set the match any flow in the Path Mapper table to go to the Path Mapper
     * ACL table.
     *
     * @param sffNodeName
     *            - the SFF to write the flow to
     */
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
    public void configureTransportEgressTableMatchAny(final String sffNodeName) {
        // This is the last table, cant set next table AND doDrop should be
        // false
        FlowBuilder flowBuilder = configureTableMatchAnyDropFlow(getTableId(TABLE_INDEX_TRANSPORT_EGRESS));
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
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionDropPacket(0));

        // Match any
        MatchBuilder match = new MatchBuilder();

        // Finish up the instructions
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(tableId, FLOW_PRIORITY_MATCH_ANY, "MatchAny", match, isb);
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

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(tableId, FLOW_PRIORITY_MATCH_ANY, "MatchAny", match, isb);
    }


    /**
     * Simple pass-through method used by
     * configureTransportIngressFlow() methods.
     *
     * @param match
     *            - the MatchBuilder object which will create the matches
     * @param nextTable
     *            - the nextTable to jump to upon matching
     *
     * @return a FlowBuilder with the created Transport Ingress flow
     */
    protected FlowBuilder configureTransportIngressFlow(MatchBuilder match, short nextTable) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureTransportIngressFlow");

        InstructionsBuilder isb = SfcOpenflowUtils.appendGotoTableInstruction(new InstructionsBuilder(), nextTable);

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_TRANSPORT_INGRESS),
                FLOW_PRIORITY_TRANSPORT_INGRESS, "ingress_Transport_Flow", match, isb);
    }



    /**
     * Simple pass-through method used by the
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
    protected FlowBuilder configureNextHopFlow(MatchBuilder match, List<Action> actionList, int flowPriority) {
        LOG.debug("SfcProviderSffFlowWriter.configureNextHopFlow");

        // this apply actions has an 'order' of 0 - first one executed
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);
        SfcOpenflowUtils.appendGotoTableInstruction(isb, getTableId(TABLE_INDEX_TRANSPORT_EGRESS));

        // Create and configure the FlowBuilder
        return SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_NEXT_HOP), flowPriority,
                NEXT_HOP_FLOW_NAME_LITERAL, match, isb);
    }

    /**
     * Simple pass-through method used by the
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
    protected FlowBuilder configureTransportEgressFlow(MatchBuilder match, List<Action> actionList, String port,
            int flowPriority, String cookieStr) {
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
                "default_egress_flow", match, isb);
    }

}
