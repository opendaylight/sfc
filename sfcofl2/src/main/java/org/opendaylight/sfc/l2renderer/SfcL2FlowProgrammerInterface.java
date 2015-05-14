/**
 * Copyright (c) 2014 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.sfc.l2renderer;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.sfc.l2renderer.sfg.GroupBucketInfo;

/**
 * An interface to be implemented by concrete classes that will write to OpenFlow or OVS switches.
 * <p>
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @since 2015-02-24
 */
public interface SfcL2FlowProgrammerInterface {

    public void shutdown() throws ExecutionException, InterruptedException;

    public short getTableBase();

    public void setTableBase(short tableBase);

    //
    // Congfigure Table 0, Transport Ingress
    //
    public void configureIpv4TransportIngressFlow(final String sffNodeName, final boolean isAddFlow);

    public void configureVlanTransportIngressFlow(final String sffNodeName, final boolean isAddFlow);

    public void configureVxlanGpeTransportIngressFlow(final String sffNodeName, final boolean isAddFlow);

    public void configureMplsTransportIngressFlow(final String sffNodeName, final boolean isAddFlow);

    //
    // Configure Table 1, Path Mapper
    //
    public void configureMacPathMapperFlow(final String sffNodeName, final String mac, long pathId, boolean isSf, final boolean isAddFlow);

    public void configureMplsPathMapperFlow(final String sffNodeName, final long label, long pathId, boolean isSf, final boolean isAddFlow);

    public void configureVlanPathMapperFlow(final String sffNodeName, final int vlan, long pathId, boolean isSf, final boolean isAddFlow);

    public void configureVxlanGpePathMapperFlow(final String sffNodeName, long nsp, short nsi, long pathId, final boolean isAddFlow);

    //
    // Table 2, NextHop
    //
    public void configureNextHopFlow(
            final String sffNodeName, final long sfpId, final String srcMac, final String dstMac, final boolean isAddFlow);

    public void configureGroupNextHopFlow(
            final String sffNodeName, final long sfpId, final String srcMac, final long groupId, final String groupName, final boolean isAddFlow);

    public void configureVxlanGpeNextHopFlow(
            final String sffNodeName, final String dstIp, final long nsp, final short nsi, final boolean isAddFlow);

    //
    // Table 10, Transport Egress
    //
    public void configureMacTransportEgressFlow(
            final String sffNodeName,
            final String srcMac,
            final String dstMac,
            final String port,
            final long pathId,
            final boolean setDscp,
            final boolean doPktIn,
            final boolean isAddFlow);

    public void configureVlanTransportEgressFlow(
            final String sffNodeName,
            final String srcMac,
            final String dstMac,
            final int dstVlan,
            final String port,
            final long pathId,
            final boolean setDscp,
            final boolean doPktIn,
            final boolean isAddFlow);

    public void configureVxlanGpeTransportEgressFlow(
            final String sffNodeName,
            final long nshNsp,
            final short nshNsi,
            final String port,
            final boolean doPktIn,
            final boolean isAddFlow);

    public void configureMplsTransportEgressFlow(
            final String sffNodeName,
            final String srcMac,
            final String dstMac,
            final long mplsLabel,
            final String port,
            final long pathId,
            boolean setDscp,
            final boolean doPktIn,
            final boolean isAddFlow);

    //
    // Configure the MatchAny entry specifying if it should drop or goto the next table
    // If doDrop == False
    //      TransportIngress MatchAny will go to Ingress
    //      PathMapper       MatchAny will go to PathMapperAcl
    //      PathMapperAcl    MatchAny will go to NextHop
    //      NextHop          MatchAny will go to TransportEgress
    //
    public void configureTransportIngressTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow);
    public void configurePathMapperTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow);
    public void configurePathMapperAclTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow);
    public void configureNextHopTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow);
    public void configureTransportEgressTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow);

    // group configuration
    public void configureGroup(final String sffNodeName, final String openflowNodeId, final String sfgName, final long sfgId, int groupType, List<GroupBucketInfo> bucketInfos, final boolean isAddGroup);

}
