/**
 * Copyright (c) 2014 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.sfc.l2renderer;


/**
 * An interface to be implemented by concrete classes that will write to OpenFlow or OVS switches.
 * <p>
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @since 2015-02-24
 */
public interface SfcL2FlowProgrammerInterface {

    public void shutdown();

    public short getTableBase();

    public void setTableBase(short tableBase);

    //
    // Congfigure Table 0, Transport Ingress
    //
    public void configureIpv4TransportIngressFlow(final String sffNodeName, final boolean isAddFlow);

    public void configureVlanTransportIngressFlow(final String sffNodeName, final boolean isAddFlow);

    public void configureMplsTransportIngressFlow(final String sffNodeName, final boolean isAddFlow);

    //
    // Configure Table 1, Ingress
    //
    public void configureMacIngressFlow(final String sffNodeName, final String mac, long pathId, final boolean isAddFlow);

    public void configureMplsIngressFlow(final String sffNodeName, final long label, long pathId, final boolean isAddFlow);

    public void configureVlanIngressFlow(final String sffNodeName, final int vlan, long pathId, final boolean isAddFlow);

    //
    // Configure Table 2, ACL
    //
    public void configureClassificationFlow(final String sffNodeName, final long pathId, final boolean isAddFlow);

    //
    // Table 3, NextHop
    //
    public void configureNextHopFlow(
            final String sffNodeName, final long sfpId, final String srcMac, final String dstMac, final boolean isAddFlow);

    //
    // Table 10, Transport Egress
    //
    public void configureMacTransportEgressFlow(
            final String sffNodeName,
            final String srcMac, final String dstMac,
            int port, final long pathId,
            boolean setDscp, final boolean isAddFlow);

    public void configureVlanTransportEgressFlow(
            final String sffNodeName,
            final String srcMac, final String dstMac,
            final int dstVlan, int port, final long pathId,
            boolean setDscp, final boolean isAddFlow);

    public void configureMplsTransportEgressFlow(
            final String sffNodeName,
            final String srcMac, final String dstMac,
            final long mplsLabel, int port, final long pathId,
            boolean setDscp, final boolean isAddFlow);

    //
    // Configure the MatchAny entry specifying if it should drop or goto the next table
    // If doDrop == False
    //      TransportIngress MatchAny will go to Ingress
    //      Ingress          MatchAny will go to Acl
    //      Acl              MatchAny will go to NextHop
    //      NextHop          MatchAny will go to TransportEgress
    //
    public void configureTransportIngressTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow);
    public void configureIngressTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow);
    public void configureAclTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow);
    public void configureNextHopTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow);
    public void configureTransportEgressTableMatchAny(final String sffNodeName, final boolean doDrop, final boolean isAddFlow);
}
