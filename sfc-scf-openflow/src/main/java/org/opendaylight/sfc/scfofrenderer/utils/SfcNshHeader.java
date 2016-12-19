/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
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
public class SfcNshHeader {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNshHeader.class);

    private Ipv4Address vxlanIpDst;
    private PortNumber vxlanUdpPort;
    private Long nshNsp;
    private Short nshStartNsi;
    private Short nshEndNsi;
    private Long nshMetaC1;
    private Long nshMetaC2;
    private Long nshMetaC3;
    private Long nshMetaC4;
    private SffName sffName;
    private SfName sfName;
    private RenderedServicePath rsp;

    public SfcNshHeader() {
    }

    public Ipv4Address getVxlanIpDst() {
        return vxlanIpDst;
    }

    public SfcNshHeader setVxlanIpDst(Ipv4Address vxlanIpDst) {
        this.vxlanIpDst = vxlanIpDst;
        return this;
    }

    public PortNumber getVxlanUdpPort() {
        return vxlanUdpPort;
    }

    public SfcNshHeader setVxlanUdpPort(PortNumber vxlanUdpPort) {
        this.vxlanUdpPort = vxlanUdpPort;
        return this;
    }

    public Long getNshNsp() {
        return nshNsp;
    }

    public SfcNshHeader setNshNsp(Long nshNsp) {
        this.nshNsp = nshNsp;
        return this;
    }

    public Short getNshStartNsi() {
        return nshStartNsi;
    }

    public SfcNshHeader setNshStartNsi(Short nshStartNsi) {
        this.nshStartNsi = nshStartNsi;
        return this;
    }

    public Short getNshEndNsi() {
        return nshEndNsi;
    }

    public SfcNshHeader setNshEndNsi(Short nshEndNsi) {
        this.nshEndNsi = nshEndNsi;
        return this;
    }

    public Long getNshMetaC1() {
        return this.nshMetaC1;
    }

    public SfcNshHeader setNshMetaC1(Long nshMetaC1) {
        this.nshMetaC1 = nshMetaC1;
        return this;
    }

    public Long getNshMetaC2() {
        return this.nshMetaC2;
    }

    public SfcNshHeader getNshMetaC2(Long nshMetaC2) {
        this.nshMetaC2 = nshMetaC2;
        return this;
    }

    public SfcNshHeader setNshMetaC2(Long nshMetaC2) {
        this.nshMetaC2 = nshMetaC2;
        return this;
    }

    public Long getNshMetaC3() {
        return nshMetaC3;
    }

    public SfcNshHeader setNshMetaC3(Long nshMetaC3) {
        this.nshMetaC3 = nshMetaC3;
        return this;
    }

    public Long getNshMetaC4() {
        return nshMetaC4;
    }

    public SfcNshHeader setNshMetaC4(Long nshMetaC4) {
        this.nshMetaC4 = nshMetaC4;
        return this;
    }

    public SffName getSffName() {
        return sffName;
    }

    public SfcNshHeader setSffName(SffName sffName) {
        this.sffName = sffName;
        return this;
    }

    public SfName getFirstSfName() {
        return sfName;
    }

    public SfcNshHeader setFirstSfName(SfName sfName) {
        this.sfName = sfName;
        return this;
    }

    public RenderedServicePath getRsp() {
        return rsp;
    }

    public SfcNshHeader setRsp(RenderedServicePath theRsp) {
        rsp = theRsp;
        return this;
    }

    public static SfcNshHeader getSfcNshHeader(RenderedServicePath theRsp) {

        RenderedServicePathHop theFirstHop = theRsp.getRenderedServicePathHop().get(0);
        RenderedServicePathHop lastRspHop = Iterables.getLast(theRsp.getRenderedServicePathHop());

        if (lastRspHop == null) {
            LOG.error("getSfcNshHeader: last rsp hop is null\n");
            return null;
        }

        RenderedServicePathFirstHop rspFirstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(theRsp.getName());
        if (rspFirstHop == null) {
            LOG.error("getSfcNshHeader: rsp first hop is null\n");
            return null;
        }

        SfcNshHeader sfcNshHeader = new SfcNshHeader()
                .setRsp(theRsp)
                .setNshNsp(theRsp.getPathId())
                .setNshStartNsi(theRsp.getStartingIndex())
                .setNshEndNsi((short) (lastRspHop.getServiceIndex().intValue() - 1))
                .setSffName(lastRspHop.getServiceFunctionForwarder())
                .setFirstSfName(theFirstHop.getServiceFunctionName());

        if (rspFirstHop.getIp() != null) {
            sfcNshHeader.setVxlanIpDst(rspFirstHop.getIp().getIpv4Address()).setVxlanUdpPort(rspFirstHop.getPort());
        }

        String context = theRsp.getContextMetadata();
        if (context == null) {
            LOG.error("getSfcNshHeader: context is null\n");
        }

        ContextMetadata md = SfcProviderServiceFunctionMetadataAPI.readContextMetadata(context);
        if (md == null) {
            LOG.error("getSfcNshHeader: metadata is null\n");
        } else {
            sfcNshHeader.setNshMetaC1(md.getContextHeader1())
                    .setNshMetaC2(md.getContextHeader2())
                    .setNshMetaC3(md.getContextHeader3())
                    .setNshMetaC4(md.getContextHeader4());
        }

        return sfcNshHeader;
    }

    public static SfcNshHeader getSfcNshHeader(RspName rspName) {

        if (rspName == null) {
            LOG.error("getSfcNshHeader: rspName is null\n");
            return null;
        }

        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (renderedServicePath == null) {
            LOG.error("getSfcNshHeader: rsp is null\n");
            return null;
        }

        RenderedServicePathFirstHop rspFirstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(rspName);
        if (rspFirstHop == null) {
            LOG.error("getSfcNshHeader: rsp first hop is null\n");
            return null;
        }

        if (renderedServicePath.getRenderedServicePathHop() == null) {
            LOG.error("getSfcNshHeader: getRenderedServicePathHop is null\n");
            return null;
        }

        RenderedServicePathHop lastRspHop = Iterables.getLast(renderedServicePath.getRenderedServicePathHop());

        if (lastRspHop == null) {
            LOG.error("getSfcNshHeader: last rsp hop is null\n");
            return null;
        }

        RenderedServicePathHop theFirstHop = renderedServicePath.getRenderedServicePathHop().get(0);

        SfcNshHeader sfcNshHeader = new SfcNshHeader()
                .setRsp(renderedServicePath)
                .setNshNsp(renderedServicePath.getPathId())
                .setNshStartNsi(rspFirstHop.getStartingIndex())
                .setNshEndNsi((short) (lastRspHop.getServiceIndex().intValue() - 1))
                .setSffName(lastRspHop.getServiceFunctionForwarder())
                .setFirstSfName(theFirstHop.getServiceFunctionName());

        if (rspFirstHop.getIp() != null) {
            sfcNshHeader.setVxlanIpDst(rspFirstHop.getIp().getIpv4Address()).setVxlanUdpPort(rspFirstHop.getPort());
        }

        String context = renderedServicePath.getContextMetadata();
        if (context == null) {
            LOG.error("getSfcNshHeader: context is null\n");
        }

        ContextMetadata md = SfcProviderServiceFunctionMetadataAPI.readContextMetadata(context);
        if (md == null) {
            LOG.error("getSfcNshHeader: metadata is null\n");
        } else {
            sfcNshHeader.setNshMetaC1(md.getContextHeader1())
                .setNshMetaC2(md.getContextHeader2())
                .setNshMetaC3(md.getContextHeader3())
                .setNshMetaC4(md.getContextHeader4());
        }

        return sfcNshHeader;
    }
}
