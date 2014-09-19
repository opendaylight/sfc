/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ofsfc.provider;

import org.opendaylight.ofsfc.provider.utils.SfcInstanceIdentifierUtils;
import org.opendaylight.ofsfc.provider.utils.SfcOfL2APIUtil;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.SfcProviderRestAPI;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OpenflowSfpDataListener extends OpenflowAbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowSfpDataListener.class);
    private static final OpenflowSfcRenderer odlSfc = OpenflowSfcRenderer.getOpendaylightSfcObj();

    public OpenflowSfpDataListener(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        setIID(SfcInstanceIdentifierUtils.createServiceFunctionPathsPath());
        registerAsDataChangeListener();
    }

    private void installDefaultNextHopEntry(long sfpId, ServicePathHop servicePathHop, boolean isAddFlow) {

        int curSFSrcVlan;
        String curSFSrcMac;
        LocatorType curSFLocatorType;
        MacAddressLocator curSFMacAddressLocator;

        OpenflowSfcFlowProgrammer.getInstance().setNodeInfo(servicePathHop.getServiceFunctionForwarder());

        ServiceFunction sf = SfcOfL2APIUtil.readServiceFunction(servicePathHop.getServiceFunctionName());
        List<SfDataPlaneLocator> curSfDataPlaneLocatorList = sf.getSfDataPlaneLocator();

        for (SfDataPlaneLocator sfDataPlanelocator : curSfDataPlaneLocatorList) {
            curSFLocatorType = sfDataPlanelocator.getLocatorType();

            if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                curSFMacAddressLocator = (MacAddressLocator) sfDataPlanelocator.getLocatorType();
                curSFSrcMac = curSFMacAddressLocator.getMac().getValue();
                curSFSrcVlan = curSFMacAddressLocator.getVlanId();

                // install ingress flow for each of the dataplane locator
                OpenflowSfcFlowProgrammer.getInstance().configureDefaultNextHopFlow(sfpId, curSFSrcMac, curSFSrcVlan,
                        isAddFlow);
            }
        }
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject;
        dataOriginalConfigurationObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPaths) {
                ServiceFunctionPaths originalServiceFunctionPaths = (ServiceFunctionPaths) entry.getValue();
                List<ServiceFunctionPath> sfcServiceFunctionPathList = originalServiceFunctionPaths
                        .getServiceFunctionPath();
                for (ServiceFunctionPath sfcServiceFunctionPath : sfcServiceFunctionPathList) {
                    LOG.debug("\n########## Original ServiceFunction name: {}", sfcServiceFunctionPath.getName());

                }
            }
        }

        // SFP creation
        Map<InstanceIdentifier<?>, DataObject> dataCreatedConfigurationObject;
        dataCreatedConfigurationObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPaths) {
                ServiceFunctionPaths createdServiceFunctionPaths = (ServiceFunctionPaths) entry.getValue();

                configureSffFlows(createdServiceFunctionPaths, true);

            }
        }

        // SFP updation

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject;
        dataUpdatedConfigurationObject = change.getUpdatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionPaths && (!(dataCreatedConfigurationObject
                    .containsKey(entry.getKey()))))) {
                ServiceFunctionPaths updatedServiceFunctionPaths = (ServiceFunctionPaths) entry.getValue();

                configureSffFlows(updatedServiceFunctionPaths, true);

            }
        }

        // SFP deletion
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalConfigurationObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionPaths) {
                ServiceFunctionPaths originalServiceFunctionPaths = (ServiceFunctionPaths) dataObject;

                configureSffFlows(originalServiceFunctionPaths, false);

            }
        }
    }

    private void configureSffFlows(ServiceFunctionPaths createdServiceFunctionPaths, boolean isAddFlow) {
        Iterator<ServiceFunctionPath> sfpIter = createdServiceFunctionPaths.getServiceFunctionPath().iterator();
        OpenflowSfcFlowProgrammer flowProgrammer = OpenflowSfcFlowProgrammer.getInstance();
        ServicePathHop servicePathHopPrev = null;
        Long sfpId = 0L;
        // Each Service Function Path configured
        while (sfpIter.hasNext()) {
            ServiceFunctionPath sfp = sfpIter.next();
            sfpId = sfp.getPathId();
            Iterator<ServicePathHop> servicePathHopIter = sfp.getServicePathHop().iterator();

            String curSFFName = null;

            ServicePathHop servicePathHopCur = null;

            // Each Service Function in the Service Function Path
            while (servicePathHopIter.hasNext()) {
                servicePathHopCur = servicePathHopIter.next();

                curSFFName = servicePathHopCur.getServiceFunctionForwarder();
                flowProgrammer.setNodeInfo(curSFFName);
                flowProgrammer.configureSffNextHopDefaultFlow(isAddFlow);

                ServiceFunctionForwarder curSff = SfcOfL2APIUtil.readServiceFunctionForwarder(odlSfc.getDataProvider(),
                        curSFFName);

                List<SffDataPlaneLocator> curSffDataPlanelocatorList = curSff.getSffDataPlaneLocator();
                for (SffDataPlaneLocator curSffDataPlanelocator : curSffDataPlanelocatorList) {
                    LocatorType curSFFLocatorType = curSffDataPlanelocator.getDataPlaneLocator().getLocatorType();
                    if (curSFFLocatorType.getImplementedInterface().equals(Mac.class)) {
                        int curSFFVlan = ((MacAddressLocator) curSffDataPlanelocator.getDataPlaneLocator()
                                .getLocatorType()).getVlanId();
                        flowProgrammer.configureIngressFlow(curSFFVlan, isAddFlow);
                        // TODO add support for multiple data plane locators
                        break;
                    }
                }

                ServiceFunction curSF = SfcOfL2APIUtil.readServiceFunction(servicePathHopCur.getServiceFunctionName());
                List<SfDataPlaneLocator> curSfDataPlaneLocatorList = curSF.getSfDataPlaneLocator();
                for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                    LocatorType curSFLocatorType = curSfDataPlanelocator.getLocatorType();
                    if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                        int curSFVlan = ((MacAddressLocator) curSfDataPlanelocator.getLocatorType()).getVlanId();
                        // install ingress flow for dataplane locator of the SF
                        flowProgrammer.configureIngressFlow(curSFVlan, isAddFlow);

                        // TODO add support for multiple data plane locators
                        break;
                    }
                }

                if (servicePathHopPrev == null) {
                    installDefaultNextHopEntry(sfp.getPathId(), servicePathHopCur, isAddFlow);
                } else {
                    String prevSFSrcMac = null;

                    ServiceFunction prevSF;
                    ServiceFunctionForwarder prevSFF;
                    List<SfDataPlaneLocator> prevSFDataPlaneLocatorList;

                    prevSFF = SfcOfL2APIUtil.readServiceFunctionForwarder(odlSfc.getDataProvider(),
                            servicePathHopPrev.getServiceFunctionForwarder());
                    prevSF = SfcOfL2APIUtil.readServiceFunction(servicePathHopPrev.getServiceFunctionName());

                    prevSFDataPlaneLocatorList = prevSF.getSfDataPlaneLocator();
                    for (SfDataPlaneLocator prevSFDataPlanelocator : prevSFDataPlaneLocatorList) {
                        LocatorType prevSFLocatorType = prevSFDataPlanelocator.getLocatorType();
                        if (prevSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                            prevSFSrcMac = ((MacAddressLocator) prevSFLocatorType).getMac().getValue();

                            // TODO add support for multiple data plane locators
                            break;
                        }
                    }

                    // check whether the prev and current sffs are same
                    if (servicePathHopPrev.getServiceFunctionForwarder().equals(curSFFName)) {
                        for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                            LocatorType curSFLocatorType = curSfDataPlanelocator.getLocatorType();
                            if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                                MacAddressLocator macAddressLocator;
                                macAddressLocator = (MacAddressLocator) curSFLocatorType;
                                String dstMac = macAddressLocator.getMac().getValue();
                                int dstVlan = macAddressLocator.getVlanId();

                                flowProgrammer.configureNextHopFlow(sfp.getPathId(), prevSFSrcMac, dstMac, dstVlan,
                                        isAddFlow);

                                // TODO add support for multiple data plane
                                // locators
                                break;
                            }
                        }
                    } else {
                        for (SffDataPlaneLocator curSffDataPlanelocator : curSffDataPlanelocatorList) {
                            String srcMacSff = ((MacAddressLocator) curSffDataPlanelocator.getDataPlaneLocator()
                                    .getLocatorType()).getMac().getValue();
                            for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                                LocatorType curSFLocatorType = curSfDataPlanelocator.getLocatorType();
                                if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                                    MacAddressLocator macAddressLocator;

                                    macAddressLocator = (MacAddressLocator) curSFLocatorType;
                                    String dstMac = macAddressLocator.getMac().getValue();
                                    int dstVlan = macAddressLocator.getVlanId();
                                    // Install one flow in the current Sff
                                    flowProgrammer.configureNextHopFlow(sfp.getPathId(), srcMacSff, dstMac, dstVlan,
                                            isAddFlow);

                                    // TODO add support for multiple data plane
                                    // locators
                                    break;
                                }
                            }
                        }

                        List<SffDataPlaneLocator> prevSFFDataPlanelocatorList = prevSFF.getSffDataPlaneLocator();
                        for (SffDataPlaneLocator prevSFFDataPlanelocator : prevSFFDataPlanelocatorList) {
                            LocatorType curSFFLocatorType = prevSFFDataPlanelocator.getDataPlaneLocator()
                                    .getLocatorType();
                            if (curSFFLocatorType.getImplementedInterface().equals(Mac.class)) {
                                String dstMacSff = ((MacAddressLocator) prevSFFDataPlanelocator.getDataPlaneLocator()
                                        .getLocatorType()).getMac().getValue();
                                int dstVlanSff = ((MacAddressLocator) prevSFFDataPlanelocator.getDataPlaneLocator()
                                        .getLocatorType()).getVlanId();
                                // Install one flow in the old Sff
                                flowProgrammer.setNodeInfo(servicePathHopPrev.getServiceFunctionForwarder());
                                flowProgrammer.configureNextHopFlow(sfp.getPathId(), prevSFSrcMac, dstMacSff,
                                        dstVlanSff, isAddFlow);

                                // TODO add support for multiple data plane
                                // locators
                                break;
                            }
                        }

                    }

                }
                servicePathHopPrev = servicePathHopCur;
                servicePathHopCur = null;
            }
        }
        // TODO escape route
        ServiceFunction prevSF = SfcOfL2APIUtil.readServiceFunction(servicePathHopPrev.getServiceFunctionName());
        String prevSFSrcMac = null;
        List<SfDataPlaneLocator> prevSFDataPlaneLocatorList = prevSF.getSfDataPlaneLocator();
        for (SfDataPlaneLocator prevSFDataPlanelocator : prevSFDataPlaneLocatorList) {
            LocatorType prevSFLocatorType = prevSFDataPlanelocator.getLocatorType();
            if (prevSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                prevSFSrcMac = ((MacAddressLocator) prevSFLocatorType).getMac().getValue();

                // TODO add support for multiple data plane locators
                break;
            }
        }

        String prevSFFName = servicePathHopPrev.getServiceFunctionForwarder();
        flowProgrammer.setNodeInfo(prevSFFName);

        ServiceFunctionForwarder prevSff = SfcOfL2APIUtil.readServiceFunctionForwarder(odlSfc.getDataProvider(),
                prevSFFName);

        List<SffDataPlaneLocator> prevSffDataPlanelocatorList = prevSff.getSffDataPlaneLocator();
        for (SffDataPlaneLocator prevSffDataPlanelocator : prevSffDataPlanelocatorList) {
            LocatorType prevSFFLocatorType = prevSffDataPlanelocator.getDataPlaneLocator().getLocatorType();
            if (prevSFFLocatorType.getImplementedInterface().equals(Mac.class)) {
                int prevSFFVlan = ((MacAddressLocator) prevSffDataPlanelocator.getDataPlaneLocator().getLocatorType())
                        .getVlanId();
                String prevMacSff = ((MacAddressLocator) prevSffDataPlanelocator.getDataPlaneLocator().getLocatorType())
                        .getMac().getValue();
                flowProgrammer.configureNextHopFlow(sfpId, prevSFSrcMac, prevMacSff, prevSFFVlan, isAddFlow);
                // TODO add support for multiple data plane locators

                break;
            }
        }
    }
}
