/*
 * Copyright (c) 2014, 2017 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.netconf.provider.api;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import com.google.common.base.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFDescriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFMonitoringInfoInputBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFMonitoringInfoOutput;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.ServiceFunctionDescriptionMonitorReportService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.networks.network.network.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcProviderSfDescriptionMonitorAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfDescriptionMonitorAPI.class);
    private static final InstanceIdentifier<Topology> NETCONF_TOPO_IID = InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    private final MountPointService mountService;

    public SfcProviderSfDescriptionMonitorAPI(MountPointService mountService) {
        this.mountService = mountService;
        LOG.info("SfcProviderSfDescriptionMonitorAPI bean initialized.");
    }

    public GetSFDescriptionOutput getSFDescriptionInfoFromNetconf(String nodeName) {
        GetSFDescriptionOutput ret = null;
        printTraceStart(LOG);
        ServiceFunctionDescriptionMonitorReportService service = getSfDescriptionMonitorService(nodeName);
        if (service != null) {
            Future<RpcResult<GetSFDescriptionOutput>> result =
                    service.getSFDescription(new GetSFDescriptionInputBuilder().build());
            RpcResult<GetSFDescriptionOutput> output;
            try {
                output = result.get();
                if (output.isSuccessful()) {
                    ret = output.getResult();
                    LOG.info("getSFDescription() successfully.");
                } else {
                    LOG.error("getSFDescription() failed.");
                }

            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Cannot get the SF description information", e);
            }
        }
        printTraceStop(LOG);
        return ret;
    }

    public GetSFMonitoringInfoOutput getSFMonitorInfoFromNetconf(String nodeName) {
        GetSFMonitoringInfoOutput ret = null;
        printTraceStart(LOG);

        ServiceFunctionDescriptionMonitorReportService service = getSfDescriptionMonitorService(nodeName);
        if (service != null) {
            Future<RpcResult<GetSFMonitoringInfoOutput>> result =
                    service.getSFMonitoringInfo(new GetSFMonitoringInfoInputBuilder().build());
            RpcResult<GetSFMonitoringInfoOutput> output;
            try {
                output = result.get();
                if (output.isSuccessful()) {
                    ret = output.getResult();
                    LOG.info("getSFMonitoringInfo() succeeded.");
                } else {
                    LOG.error("getSFMonitoringInfo() failed.");
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Cannot get the SF monitoring information", e);
            }
        }
        printTraceStop(LOG);
        return ret;
    }

    private ServiceFunctionDescriptionMonitorReportService getSfDescriptionMonitorService(String nodeName) {
        InstanceIdentifier<?> nodeIID = NETCONF_TOPO_IID.child(Node.class, new NodeKey(new NodeId(nodeName)));

        Optional<MountPoint> mountPoint = mountService.getMountPoint(nodeIID);
        if (!mountPoint.isPresent()) {
            LOG.error("Mount point for node {} doesn't exist", nodeName);
            return null;
        }
        final Optional<RpcConsumerRegistry> service = mountPoint.get().getService(RpcConsumerRegistry.class);
        if (!service.isPresent()) {
            LOG.error("Failed to get RpcService for node {}", nodeName);
            return null;
        }
        return service.get().getRpcService(ServiceFunctionDescriptionMonitorReportService.class);
    }
}
