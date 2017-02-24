/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sfc_vpp_renderer.renderer;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.util.vpp.SfcVppUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppRspProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(VppRspProcessor.class);

    private final VppNodeManager nodeManager;
    private static final String SFC_BD_NAME = new String("SFCVPP");
    private static final String DUMMY_BD_NAME = new String("SFCDUMMY");
    private final Map<String, String> bridgeDomainCreated = new HashMap<>();

    public VppRspProcessor(VppNodeManager nodeManager) {
        this.nodeManager = Preconditions.checkNotNull(nodeManager);
    }

    public void updateRsp(RenderedServicePath renderedServicePath) {
        Preconditions.checkNotNull(renderedServicePath);
        Long pathId = renderedServicePath.getPathId();
        Short serviceIndex = renderedServicePath.getStartingIndex();
        DataBroker previousMountPoint = null;
        DataBroker currentMountpoint = null;
        SffName previousSffName = null;
        SffName currentSffName = null;
        SfName sfName;
        List<IpAddress> ipList = null;
        IpAddress localIp = null;
        IpAddress remoteIp;
        IpAddress preLocalIp = null;
        boolean ret = false;

        if (renderedServicePath.getRenderedServicePathHop() == null ||
                renderedServicePath.getRenderedServicePathHop().isEmpty()) {
            LOG.warn("Rendered path {} does not contain any hop", renderedServicePath.getName().getValue());
            return;
        }
        Iterator<RenderedServicePathHop> rspHopIterator = renderedServicePath.getRenderedServicePathHop()
                .iterator();

        while (rspHopIterator.hasNext()) {
            RenderedServicePathHop hop = rspHopIterator.next();
            previousSffName = currentSffName;
            previousMountPoint = currentMountpoint;
            preLocalIp = localIp;
            currentSffName = hop.getServiceFunctionForwarder();
            currentMountpoint = SfcVppUtils.getSffMountpoint(this.nodeManager.getMountPointService(), currentSffName);
            if (currentMountpoint == null) {
                LOG.error("Resolving of RSP {} failed in updateRsp, mountpoint for SFF {} is null", renderedServicePath.getName()
                    .getValue(), currentSffName.getValue());
                return;
            }

            sfName = hop.getServiceFunctionName();
            serviceIndex = hop.getServiceIndex();
            ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
            if (serviceFunction == null) {
                LOG.error("Service function {} not present in datastore", sfName.getValue());
                return;
            }
            ipList = SfcVppUtils.getSffSfIps(currentSffName, sfName);
            if (ipList == null || ipList.isEmpty()) {
                LOG.error("failed to get IP for DPL for SFF {} in RSP {}", currentSffName.getValue(), renderedServicePath.getName().getValue());
                return;
            }
            localIp = ipList.get(0);
            remoteIp = ipList.get(1);

            /* Create BridgeDomain */
            if (!bridgeDomainCreated.containsKey(currentSffName.getValue())) {
                SfcVppUtils.addDummyBridgeDomain(currentMountpoint, DUMMY_BD_NAME, currentSffName.getValue());
                SfcVppUtils.addDummyNshEntry(currentMountpoint, 0L, (short)1, currentSffName.getValue());
                SfcVppUtils.addDummyNshMap(currentMountpoint, 0L, (short)1, 0L, (short)1, new String("local0"), currentSffName.getValue());
                SfcVppUtils.addBridgeDomain(currentMountpoint, SFC_BD_NAME, currentSffName.getValue());
                bridgeDomainCreated.put(currentSffName.getValue(), SFC_BD_NAME);
            }

            ret = SfcVppUtils.configureVxlanGpeNsh(currentMountpoint, currentSffName, SFC_BD_NAME, localIp, remoteIp, pathId, serviceIndex);
            if (!ret) {
                LOG.error("failed to configure VxLAN-gpe and NSH for RSP {} in SFF {} for SF hop", renderedServicePath.getName().getValue(), currentSffName.getValue());
                return;
            }

            //previous SFF <-> current SFF
            if (previousSffName != null && !previousSffName.equals(currentSffName)) {
                ret = SfcVppUtils.configureVxlanGpeNsh(previousMountPoint, previousSffName, SFC_BD_NAME, preLocalIp, localIp, pathId, serviceIndex);
                if (!ret) {
                    LOG.error("failed to configure VxLAN-gpe and NSH for RSP {} in SFF {} for SFF hop", renderedServicePath.getName().getValue(), previousSffName.getValue());
                    return;
                }
            }
        }

        /* vpp classifier will configure VxlanGpeNsh for last hop to classifier */
    }

    public void deleteRsp(RenderedServicePath renderedServicePath) {
        boolean ret = false;
        Preconditions.checkNotNull(renderedServicePath);
        Long pathId = renderedServicePath.getPathId();
        Short serviceIndex = renderedServicePath.getStartingIndex();
        DataBroker previousMountPoint = null;
        DataBroker currentMountpoint = null;
        SffName previousSffName = null;
        SffName currentSffName = null;
        SfName sfName;
        List<IpAddress> ipList = null;
        IpAddress localIp = null;
        IpAddress remoteIp;
        IpAddress preLocalIp = null;

        if (renderedServicePath.getRenderedServicePathHop() == null ||
                renderedServicePath.getRenderedServicePathHop().isEmpty()) {
            LOG.warn("Rendered path {} does not contain any hop", renderedServicePath.getName().getValue());
            return;
        }
        Iterator<RenderedServicePathHop> rspHopIterator = renderedServicePath.getRenderedServicePathHop()
                .iterator();

        while (rspHopIterator.hasNext()) {
            RenderedServicePathHop hop = rspHopIterator.next();
            previousSffName = currentSffName;
            previousMountPoint = currentMountpoint;
            preLocalIp = localIp;
            currentSffName = hop.getServiceFunctionForwarder();
            currentMountpoint = SfcVppUtils.getSffMountpoint(this.nodeManager.getMountPointService(), currentSffName);
            if (currentMountpoint == null) {
                LOG.error("Resolving of RSP {} failed in deleteRsp, mountpoint for SFF {} is null", renderedServicePath.getName()
                    .getValue(), currentSffName.getValue());
                return;
            }

            sfName = hop.getServiceFunctionName();
            serviceIndex = hop.getServiceIndex();
            ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
            if (serviceFunction == null) {
                LOG.error("Service function {} not present in datastore", sfName.getValue());
                return;
            }
            ipList = SfcVppUtils.getSffSfIps(currentSffName, sfName);
            if (ipList == null || ipList.isEmpty()) {
                LOG.error("failed to get IP for DPL for SFF {} in RSP {}", currentSffName.getValue(), renderedServicePath.getName().getValue());
                return;
            }
            localIp = ipList.get(0);
            remoteIp = ipList.get(1);

            ret = SfcVppUtils.removeVxlanGpeNsh(currentMountpoint, currentSffName, localIp, remoteIp, pathId, serviceIndex);
            if (!ret) {
                LOG.error("failed to remove VxLAN-gpe and NSH for RSP {} in SFF {}", renderedServicePath.getName().getValue(), currentSffName.getValue());
                return;
            }

            //previous SFF <-> current SFF
            if (previousSffName != null && !previousSffName.equals(currentSffName)) {
                ret = SfcVppUtils.removeVxlanGpeNsh(previousMountPoint, previousSffName, preLocalIp, localIp, pathId, serviceIndex);
                if (!ret) {
                    LOG.error("failed to remove VxLAN-gpe and NSH for RSP {} in SFF {}", renderedServicePath.getName().getValue(), previousSffName.getValue());
                    return;
                }
            }
        }
        /* vpp classifier will remove VxlanGpeNsh for last hop to classifier */
    }

}
