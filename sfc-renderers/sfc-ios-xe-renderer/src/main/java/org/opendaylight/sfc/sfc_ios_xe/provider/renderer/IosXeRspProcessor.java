/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.renderer;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.sfc_ios_xe.provider.listener.RenderedPathListener;
import org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI;
import org.opendaylight.sfc.sfc_ios_xe.provider.utils.RspStatus;
import org.opendaylight.sfc.sfc_ios_xe.provider.utils.SfcIosXeUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.ConfigServiceChainPathModeBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.ServiceIndexBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.Services;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.ServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.ServicesKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.ServiceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.service.type.choice.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.service.type.choice.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.service.type.choice.TerminateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.DELETE_PATH;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.WRITE_PATH;
import static org.opendaylight.sfc.sfc_ios_xe.provider.utils.IosXeDataStoreAPI.Transaction.WRITE_REMOTE;
import static org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.renderer.path.state.configured.rendered.paths.ConfiguredRenderedPath.PathStatus.Failure;
import static org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.renderer.path.state.configured.rendered.paths.ConfiguredRenderedPath.PathStatus.InProgress;
import static org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.renderer.path.state.configured.rendered.paths.ConfiguredRenderedPath.PathStatus.Success;

public class IosXeRspProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(IosXeRspProcessor.class);

    private final DataBroker dataBroker;
    private final NodeManager nodeManager;
    private final RenderedPathListener rspListener;

    public IosXeRspProcessor(DataBroker dataBroker, NodeManager nodeManager) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.nodeManager = Preconditions.checkNotNull(nodeManager);
        // Register RSP listener
        rspListener = new RenderedPathListener(dataBroker, this);
    }

    public void updateRsp(RenderedServicePath renderedServicePath) {
        // Set status
        RspStatus status = new RspStatus(dataBroker, renderedServicePath.getName());
        status.writeStatus(InProgress);

        Preconditions.checkNotNull(renderedServicePath);
        Long pathId = renderedServicePath.getPathId();
        Short serviceIndex = renderedServicePath.getStartingIndex();
        DataBroker previousMountPoint;
        DataBroker currentMountpoint;
        SffName previousSffName;
        SffName currentSffName;
        if (renderedServicePath.getRenderedServicePathHop() == null ||
                renderedServicePath.getRenderedServicePathHop().isEmpty()) {
            LOG.warn("Rendered path {} does not contain any hop", renderedServicePath.getName().getValue());
            status.writeStatus(Failure);
            return;
        }
        Iterator<RenderedServicePathHop> rspHopIterator = renderedServicePath.getRenderedServicePathHop()
                .iterator();
        // Proceed first hop in Rsp. Service Type choice for first hop is always Service Function
        RenderedServicePathHop hop = rspHopIterator.next();
        currentSffName = hop.getServiceFunctionForwarder();
        currentMountpoint = getSffMountpoint(currentSffName);
        if (currentMountpoint == null) {
            LOG.error("Resolving of RSP {} failed, mountpoint for SFF {} is null", renderedServicePath.getName()
                    .getValue(), currentSffName.getValue());
            deleteRsp(renderedServicePath);
            status.writeStatus(Failure);
            return;
        }
        // New list of services has to be created every time new mountpoint is created
        List<Services> services = new ArrayList<>();
        SfName sfName = hop.getServiceFunctionName();
        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        if (serviceFunction == null) {
            LOG.error("Service function {} not present in datastore", sfName.getValue());
            return;
        }
        ServiceTypeChoice serviceTypeChoice = buildServiceFunctionChoice(serviceFunction);
        Services serviceEntry = createServicesEntry(serviceIndex, serviceTypeChoice);
        services.add(serviceEntry);
        serviceIndex--;
        while (rspHopIterator.hasNext()) {
            hop = rspHopIterator.next();
            // Find out whether next hop SF is connected to the same SFF
            previousSffName = currentSffName;
            currentSffName = hop.getServiceFunctionForwarder();
            if (previousSffName.equals(currentSffName)) {
                // Next hop SF is on the same local SFF/node as the previous one
                sfName = hop.getServiceFunctionName();
                serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
                if (serviceFunction == null) {
                    LOG.error("Service function {} not present in datastore", sfName.getValue());
                    return;
                }
                serviceTypeChoice = buildServiceFunctionChoice(serviceFunction);
                serviceEntry = createServicesEntry(serviceIndex, serviceTypeChoice);
                services.add(serviceEntry);
                serviceIndex--;
            } else {
                // Next hop SF is on different node. Store previous SFF and its mountpoint
                previousMountPoint = currentMountpoint;
                currentSffName = hop.getServiceFunctionForwarder();
                currentMountpoint = getSffMountpoint(currentSffName);
                if (currentMountpoint == null) {
                    LOG.error("Resolving of RSP {} failed, mountpoint for SFF {} is null", renderedServicePath.getName()
                            .getValue(), currentSffName.getValue());
                    deleteRsp(renderedServicePath);
                    status.writeStatus(Failure);
                    return;
                }
                // Write current SFF to previous SFF node as remote
                ServiceFfName currentRemoteForwarder = SfcIosXeUtils.createRemoteForwarder(currentSffName);
                if (currentRemoteForwarder == null) {
                    LOG.error("SFF {} ip address is null", currentSffName.getValue());
                    deleteRsp(renderedServicePath);
                    status.writeStatus(Failure);
                    return;
                }
                new IosXeDataStoreAPI(previousMountPoint, currentRemoteForwarder, WRITE_REMOTE,
                        LogicalDatastoreType.CONFIGURATION).call();
                // Create last service entry to previous node which sends traffic to current node
                serviceTypeChoice = buildServiceFunctionForwarderChoice(currentSffName.getValue());
                serviceEntry = createServicesEntry(serviceIndex, serviceTypeChoice);
                services.add(serviceEntry);
                // List of services completed for last mountpoint, create service path entries and write it
                ServicePath servicePath = createServicePath(pathId, services);
                new IosXeDataStoreAPI(previousMountPoint, servicePath, WRITE_PATH,
                        LogicalDatastoreType.CONFIGURATION).call();
                // Start with new services list
                services = new ArrayList<>();
                sfName = hop.getServiceFunctionName();
                serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
                if (serviceFunction == null) {
                    LOG.error("Service function {} not present in datastore", sfName.getValue());
                    return;
                }
                serviceTypeChoice = buildServiceFunctionChoice(serviceFunction);
                serviceEntry = createServicesEntry(serviceIndex, serviceTypeChoice);
                services.add(serviceEntry);
                serviceIndex--;
            }
        }
        // Proceed last entry (it's the same hop as the previous one using same mountpoint and list of services)
        // Service Type choice is always Terminate
        serviceTypeChoice = buildTerminateChoice();
        serviceEntry = createServicesEntry(serviceIndex, serviceTypeChoice);
        services.add(serviceEntry);
        // List of services completed for last mountpoint, create last service path entries and write it
        ServicePath servicePath = createServicePath(pathId, services);
        new IosXeDataStoreAPI(currentMountpoint, servicePath, WRITE_PATH, LogicalDatastoreType.CONFIGURATION).call();
        LOG.info("Rendered service path {} successfully processed", renderedServicePath.getName().getValue());
        status.writeStatus(Success);
    }

    public void deleteRsp(RenderedServicePath renderedServicePath) {
        boolean success = true;
        long pathId = renderedServicePath.getPathId();
        ServicePathKey servicePathKey = new ServicePathKey(pathId);
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePath.getRenderedServicePathHop()) {
            SffName sffName = renderedServicePathHop.getServiceFunctionForwarder();
            DataBroker sffDataBroker = getSffMountpoint(sffName);
            boolean transaction = (boolean) new IosXeDataStoreAPI(sffDataBroker, servicePathKey, DELETE_PATH,
                    LogicalDatastoreType.CONFIGURATION).call();
            if (!transaction) {
                success = false;
            }
        }
        if (success) {
            LOG.info("Service path {} removed", pathId);
        } else {
            LOG.error("Failed to remove service path {}", pathId);
        }
    }

    private ServiceTypeChoice buildServiceFunctionForwarderChoice(String sffName) {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setServiceFunctionForwarder(sffName);
        return serviceFunctionForwarderBuilder.build();
    }

    private ServiceTypeChoice buildServiceFunctionChoice(ServiceFunction serviceFunction) {
        ServiceFunctionBuilder serviceFunctionTypeChoice = new ServiceFunctionBuilder();
        serviceFunctionTypeChoice.setServiceFunction(serviceFunction.getName().getValue());
        return serviceFunctionTypeChoice.build();
    }

    private ServiceTypeChoice buildTerminateChoice() {
        TerminateBuilder terminateBuilder = new TerminateBuilder();
        terminateBuilder.setTerminate(true);
        return terminateBuilder.build();
    }

    private ServicePath createServicePath(Long pathId, List<Services> services) {
        // Service Index
        ServiceIndexBuilder serviceIndexBuilder = new ServiceIndexBuilder();
        serviceIndexBuilder.setServices(services);
        // Service Chain Path Mode
        ConfigServiceChainPathModeBuilder pathModeBuilder = new ConfigServiceChainPathModeBuilder();
        pathModeBuilder.setServiceIndex(serviceIndexBuilder.build());
        // Service Path
        ServicePathBuilder servicePathBuilder = new ServicePathBuilder();
        servicePathBuilder.setKey(new ServicePathKey(pathId))
                .setServicePathId(pathId)
                .setConfigServiceChainPathMode(pathModeBuilder.build());
        return servicePathBuilder.build();
    }

    private Services createServicesEntry(short index, ServiceTypeChoice choice) {
        ServicesBuilder servicesBuilder = new ServicesBuilder();
        servicesBuilder.setKey(new ServicesKey(index))
                .setServiceIndexId(index)
                .setServiceTypeChoice(choice);
        return servicesBuilder.build();
    }

    private DataBroker getSffMountpoint(SffName sffName) {
        // Read SFF from Controller CONF
        org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder sfcForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        if (sfcForwarder == null) {
            LOG.error("SFF name {} not found in data store", sffName.getValue());
            return null;
        }
        IpAddress sffMgmtIp = sfcForwarder.getIpMgmtAddress();
        if (sffMgmtIp == null) {
            LOG.error("Unable to obtain management IP for SFF {}", sffName.getValue());
            return null;
        }
        return nodeManager.getMountpointFromIpAddress(new IpAddress(new Ipv4Address(sffMgmtIp.getIpv4Address()
                .getValue())));
    }

    public void unregisterRspListener() {
        rspListener.getRegistrationObject().close();
    }
}
