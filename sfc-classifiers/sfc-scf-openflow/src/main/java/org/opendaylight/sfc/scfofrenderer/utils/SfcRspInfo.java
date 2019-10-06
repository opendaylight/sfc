/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.utils;

import com.google.common.collect.Iterables;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionMetadataAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.ContextMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class has some overlap function with SfcProviderRenderedPathAPI.java
 * Todo: will move all functions to SfcProviderRenderedPathAPI.java
 */
public class SfcRspInfo {

    private static final Logger LOG = LoggerFactory.getLogger(SfcRspInfo.class);

    private Ipv4Address vxlanIpDst;
    private PortNumber vxlanUdpPort;
    private Long nshNsp;
    private Short nshStartNsi;
    private Short nshEndNsi;
    private Long nshMetaC1;
    private Long nshMetaC2;
    private Long nshMetaC3;
    private Long nshMetaC4;
    private SffName firstSffName;
    private SffName lastSffName;
    private SfName firstSfName;
    private RenderedServicePath rsp;

    public SfcRspInfo() {
    }

    public Ipv4Address getVxlanIpDst() {
        return vxlanIpDst;
    }

    public SfcRspInfo setVxlanIpDst(Ipv4Address vxlanIpDst) {
        this.vxlanIpDst = vxlanIpDst;
        return this;
    }

    public PortNumber getVxlanUdpPort() {
        return vxlanUdpPort;
    }

    public SfcRspInfo setVxlanUdpPort(PortNumber vxlanUdpPort) {
        this.vxlanUdpPort = vxlanUdpPort;
        return this;
    }

    public Long getNshNsp() {
        return nshNsp;
    }

    public SfcRspInfo setNshNsp(Long nshNsp) {
        this.nshNsp = nshNsp;
        return this;
    }

    public Short getNshStartNsi() {
        return nshStartNsi;
    }

    public SfcRspInfo setNshStartNsi(Short nshStartNsi) {
        this.nshStartNsi = nshStartNsi;
        return this;
    }

    public Short getNshEndNsi() {
        return nshEndNsi;
    }

    public SfcRspInfo setNshEndNsi(Short nshEndNsi) {
        this.nshEndNsi = nshEndNsi;
        return this;
    }

    public Long getNshMetaC1() {
        return this.nshMetaC1;
    }

    public SfcRspInfo setNshMetaC1(Long nshMetaC1) {
        this.nshMetaC1 = nshMetaC1;
        return this;
    }

    public Long getNshMetaC2() {
        return this.nshMetaC2;
    }

    public SfcRspInfo setNshMetaC2(Long nshMetaC2) {
        this.nshMetaC2 = nshMetaC2;
        return this;
    }

    public Long getNshMetaC3() {
        return nshMetaC3;
    }

    public SfcRspInfo setNshMetaC3(Long nshMetaC3) {
        this.nshMetaC3 = nshMetaC3;
        return this;
    }

    public Long getNshMetaC4() {
        return nshMetaC4;
    }

    public SfcRspInfo setNshMetaC4(Long nshMetaC4) {
        this.nshMetaC4 = nshMetaC4;
        return this;
    }

    public SffName getFirstSffName() {
        return firstSffName;
    }

    public SfcRspInfo setFirstSffName(SffName sffName) {
        this.firstSffName = sffName;
        return this;
    }

    public SffName getLastSffName() {
        return lastSffName;
    }

    public SfcRspInfo setLastSffName(SffName sffName) {
        this.lastSffName = sffName;
        return this;
    }

    public SfName getFirstSfName() {
        return firstSfName;
    }

    public SfcRspInfo setFirstSfName(SfName sfName) {
        this.firstSfName = sfName;
        return this;
    }

    public RenderedServicePath getRsp() {
        return rsp;
    }

    public SfcRspInfo setRsp(RenderedServicePath theRsp) {
        rsp = theRsp;
        return this;
    }

    public static SfcRspInfo getSfcRspInfo(RenderedServicePath theRsp) {

        RenderedServicePathHop theFirstHop = theRsp.getRenderedServicePathHop().get(0);
        RenderedServicePathHop lastRspHop = Iterables.getLast(theRsp.getRenderedServicePathHop());

        if (lastRspHop == null) {
            LOG.error("getSfcRspInfo: last rsp hop is null");
            return null;
        }

        RenderedServicePathFirstHop rspFirstHop = SfcProviderRenderedPathAPI
                .readRenderedServicePathFirstHop(theRsp.getName());
        if (rspFirstHop == null) {
            LOG.error("getSfcRspInfo: rsp first hop is null");
            return null;
        }

        SfcRspInfo sfcRspInfo = new SfcRspInfo().setRsp(theRsp).setNshNsp(theRsp.getPathId().toJava())
                .setNshStartNsi(theRsp.getStartingIndex().toJava())
                .setNshEndNsi((short) (lastRspHop.getServiceIndex().intValue() - 1))
                .setFirstSffName(theFirstHop.getServiceFunctionForwarder())
                .setLastSffName(lastRspHop.getServiceFunctionForwarder())
                .setFirstSfName(theFirstHop.getServiceFunctionName());

        if (rspFirstHop.getIp() != null) {
            sfcRspInfo.setVxlanIpDst(rspFirstHop.getIp().getIpv4Address()).setVxlanUdpPort(rspFirstHop.getPort());
        }

        String context = theRsp.getContextMetadata();
        if (context == null) {
            LOG.debug("getSfcRspInfo: rsp context metadata is null");
            return sfcRspInfo;
        }

        ContextMetadata md = SfcProviderServiceFunctionMetadataAPI.readContextMetadata(context);
        if (md == null) {
            LOG.debug("getSfcRspInfo: metadata could not be read from data store");
            return sfcRspInfo;
        }

        sfcRspInfo.setNshMetaC1(md.getContextHeader1().toJava())
                .setNshMetaC2(md.getContextHeader2().toJava())
                .setNshMetaC3(md.getContextHeader3().toJava())
                .setNshMetaC4(md.getContextHeader4().toJava());

        return sfcRspInfo;
    }

    public static SfcRspInfo getSfcRspInfo(RspName rspName) {

        if (rspName == null) {
            LOG.error("getSfcRspInfo: rspName is null\n");
            return null;
        }

        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (renderedServicePath == null) {
            LOG.error("getSfcRspInfo: rsp is null\n");
            return null;
        }

        return getSfcRspInfo(renderedServicePath);
    }
}
