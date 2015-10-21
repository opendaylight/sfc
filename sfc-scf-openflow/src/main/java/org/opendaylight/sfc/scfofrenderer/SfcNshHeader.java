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

import com.google.common.collect.Iterables;

public class SfcNshHeader {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNshHeader.class);

    private final Ipv4Address nshTunIpDst;
    private final PortNumber nshTunUdpPort;
    private final Long nshNspToChain;
    private final Short nshNsiToChain;
    private final Long nshNspFromChain;
    private final Short nshNsiFromChain;
    private final Long nshMetaC1;
    private final Long nshMetaC2;
    private final Long nshMetaC3;
    private final Long nshMetaC4;

    private SfcNshHeader(SfcNshHeaderBuilder builder) {
        this.nshMetaC1 = builder.nshMetaC1;
        this.nshMetaC2 = builder.nshMetaC2;
        this.nshMetaC3 = builder.nshMetaC3;
        this.nshMetaC4 = builder.nshMetaC4;
        this.nshTunIpDst = builder.nshTunIpDst;
        this.nshTunUdpPort = builder.nshTunUdpPort;
        this.nshNspToChain = builder.nshNspToChain;
        this.nshNspFromChain = builder.nshNspFromChain;
        this.nshNsiToChain = builder.nshNsiToChain;
        this.nshNsiFromChain = builder.nshNsiFromChain;
    }

    public boolean isValid(SfcNshHeader sfcNshHeader) {
        if (sfcNshHeader.nshTunIpDst == null)
            return false;
        if (sfcNshHeader.nshNspToChain == null)
            return false;
        if (sfcNshHeader.nshNspFromChain == null)
            return false;
        if (sfcNshHeader.nshNsiToChain == null)
            return false;
        if (sfcNshHeader.nshNsiFromChain == null)
            return false;
        if (sfcNshHeader.nshMetaC1 == null)
            return false;
        if (sfcNshHeader.nshMetaC2 == null)
            return false;
        return true;
    }

    public Ipv4Address getNshTunIpDst() {
        return nshTunIpDst;
    }

    public PortNumber getNshTunUdpPort() {
        return nshTunUdpPort;
    }

    public Long getNshNspToChain() {
        return nshNspToChain;
    }

    public Short getNshNsiToChain() {
        return nshNsiToChain;
    }

    public Long getNshNspFromChain() {
        return nshNspFromChain;
    }

    public Short getNshNsiFromChain() {
        return nshNsiFromChain;
    }

    public Long getNshMetaC1() {
        return nshMetaC1;
    }

    public Long getNshMetaC2() {
        return nshMetaC2;
    }

    public Long getNshMetaC3() {
        return nshMetaC3;
    }

    public Long getNshMetaC4() {
        return nshMetaC4;
    }

    public static SfcNshHeader getSfcNshHeader(RspName rspName) {
        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        RenderedServicePathFirstHop rspFirstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(rspName);
        if (!isValidRspFirstHop(rspFirstHop)) {
            // Errors logged in method.
            return null;
        }
        String context = renderedServicePath.getContextMetadata();
        // String context = "NSH1";
        ContextMetadata md = SfcProviderServiceFunctionMetadataAPI.readContextMetadata(context);

        RenderedServicePathHop firstRspHop = renderedServicePath.getRenderedServicePathHop().get(0);
        RenderedServicePathHop lastRspHop = Iterables.getLast(renderedServicePath.getRenderedServicePathHop());

        SfcNshHeader sfcNshHeader = new SfcNshHeaderBuilder().setNshTunIpDst(rspFirstHop.getIp().getIpv4Address())
            .setNshTunUdpPort(rspFirstHop.getPort())
            .setNshNsiToChain(firstRspHop.getServiceIndex())
            .setNshNspToChain(renderedServicePath.getPathId())
            .setNshNsiFromChain((short) (lastRspHop.getServiceIndex().intValue() - 1))
            .setNshNspFromChain(renderedServicePath.getPathId())
            .setNshMetaC1(md.getContextHeader1())
            .setNshMetaC2(md.getContextHeader2())
            .setNshMetaC3(md.getContextHeader3())
            .setNshMetaC4(md.getContextHeader4())
            .build();

        return sfcNshHeader;
    }

    private static boolean isValidRspFirstHop(RenderedServicePathFirstHop rspFirstHop) {
        boolean valid = true;
        if (rspFirstHop == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop is null.");
            return false;
        }
        if (rspFirstHop.getIp() == null || rspFirstHop.getIp().getIpv4Address() == null
                || rspFirstHop.getIp().getIpv6Address() != null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has invalid IP address.");
            valid = false;
        }
        if (rspFirstHop.getPort() == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has no IP port .");
            valid = false;
        }
        if (rspFirstHop.getPathId() == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has no Path Id (NSP).");
            valid = false;
        }
        if (rspFirstHop.getStartingIndex() == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has no Starting Index (NSI)");
            valid = false;
        }
        return valid;
    }

    private static class SfcNshHeaderBuilder {

        private Ipv4Address nshTunIpDst;
        private PortNumber nshTunUdpPort;
        private Long nshNspToChain;
        private Short nshNsiToChain;
        private Long nshNspFromChain;
        private Short nshNsiFromChain;
        private Long nshMetaC1;
        private Long nshMetaC2;
        private Long nshMetaC3;
        private Long nshMetaC4;

        public SfcNshHeader build() {
            SfcNshHeader sfcNshHeader = new SfcNshHeader(this);
            return sfcNshHeader;
        }

        public SfcNshHeaderBuilder() {

        }

        public SfcNshHeaderBuilder(SfcNshHeader sfcNshHeader) {

            this.nshTunIpDst = sfcNshHeader.nshTunIpDst;
            this.nshTunUdpPort = sfcNshHeader.nshTunUdpPort;
            this.nshNspToChain = sfcNshHeader.nshNspToChain;
            this.nshNsiToChain = sfcNshHeader.nshNsiToChain;
            this.nshNspFromChain = sfcNshHeader.nshNspFromChain;
            this.nshNsiFromChain = sfcNshHeader.nshNsiFromChain;
            this.nshMetaC1 = sfcNshHeader.nshMetaC1;
            this.nshMetaC2 = sfcNshHeader.nshMetaC2;
            this.nshMetaC3 = sfcNshHeader.nshMetaC3;
            this.nshMetaC4 = sfcNshHeader.nshMetaC4;
        }

        public SfcNshHeaderBuilder setNshTunIpDst(Ipv4Address nshTunIpDst) {
            this.nshTunIpDst = nshTunIpDst;
            return this;
        }

        public SfcNshHeaderBuilder setNshTunUdpPort(PortNumber nshTunUdpPort) {
            this.nshTunUdpPort = nshTunUdpPort;
            return this;
        }

        public SfcNshHeaderBuilder setNshNspToChain(Long nshNspToChain) {
            this.nshNspToChain = nshNspToChain;
            return this;
        }

        public SfcNshHeaderBuilder setNshNsiToChain(Short nshNsiToChain) {
            this.nshNsiToChain = nshNsiToChain;
            return this;
        }

        public SfcNshHeaderBuilder setNshNspFromChain(Long nshNspFromChain) {
            this.nshNspFromChain = nshNspFromChain;
            return this;
        }

        public SfcNshHeaderBuilder setNshNsiFromChain(Short nshNsiFromChain) {
            this.nshNsiFromChain = nshNsiFromChain;
            return this;
        }

        public SfcNshHeaderBuilder setNshMetaC1(Long nshMetaC1) {
            this.nshMetaC1 = nshMetaC1;
            return this;
        }

        public SfcNshHeaderBuilder setNshMetaC2(Long nshMetaC2) {
            this.nshMetaC2 = nshMetaC2;
            return this;
        }

        public SfcNshHeaderBuilder setNshMetaC3(Long nshMetaC3) {
            this.nshMetaC3 = nshMetaC3;
            return this;
        }

        public SfcNshHeaderBuilder setNshMetaC4(Long nshMetaC4) {
            this.nshMetaC4 = nshMetaC4;
            return this;
        }
    }
}
