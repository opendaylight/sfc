/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionMetadataAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.ContextMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcNshHeader {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNshHeader.class);

    private Ipv4Address vxlanIpDst = null;
    private PortNumber vxlanUdpPort = null;
    private Long nshNsp = null;
    private Short nshNsi = null;
    private Long nshMetaC1 = null;
    private Long nshMetaC2 = null;
    private Long nshMetaC3 = null;
    private Long nshMetaC4 = null;

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

    public Short getNshNsi() {
        return nshNsi;
    }

    public SfcNshHeader setNshNsi(Short nshNsi) {
        this.nshNsi = nshNsi;
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

    public static SfcNshHeader getSfcNshHeader(RspName rspName) {

        if (rspName == null) {
            LOG.error("\ngetSfcNshHeader: rspName is null");
            return null;
        }

        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (renderedServicePath == null) {
            LOG.error("\ngetSfcNshHeader: rsp is null");
            return null;
        }

        RenderedServicePathFirstHop rspFirstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(rspName);
        if (rspFirstHop == null) {
            LOG.error("\ngetSfcNshHeader: rsp first hop is null");
            return null;
        }

        if (rspFirstHop.getIp() == null) {
            LOG.error("\ngetSfcNshHeader: ip address of rsp first hop is null");
            return null;
        }

        RenderedServicePathHop firstRspHop = renderedServicePath.getRenderedServicePathHop().get(0);
        if (firstRspHop == null) {
            LOG.error("\ngetSfcNshHeader: first rsp hop is null");
            return null;
        }

        SfcNshHeader sfcNshHeader = new SfcNshHeader()
            .setVxlanIpDst(rspFirstHop.getIp().getIpv4Address())
            .setVxlanUdpPort(rspFirstHop.getPort())
            .setNshNsi(firstRspHop.getServiceIndex())
            .setNshNsp(renderedServicePath.getPathId());

        String context = renderedServicePath.getContextMetadata();
        if (context == null) {
            LOG.error("\ngetSfcNshHeader: context is null");
        }

        ContextMetadata md = SfcProviderServiceFunctionMetadataAPI.readContextMetadata(context);
        if (md == null) {
            LOG.error("\ngetSfcNshHeader: metadata is null");
        } else {
            sfcNshHeader.setNshMetaC1(md.getContextHeader1())
                .setNshMetaC2(md.getContextHeader2())
                .setNshMetaC3(md.getContextHeader3())
                .setNshMetaC4(md.getContextHeader4());
        }

        return sfcNshHeader;
    }
}
