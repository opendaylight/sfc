/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.sfc.l2renderer;

import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SfcL2SfpDataListener extends SfcL2AbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2SfpDataListener.class);
    private SfcL2FlowProgrammer sfcL2FlowProgrammer;

    public SfcL2SfpDataListener(DataBroker dataBroker, SfcL2FlowProgrammer sfcL2FlowProgrammer) {
        setDataBroker(dataBroker);
        setIID(OpendaylightSfc.SFP_ENTRY_IID);
        registerAsDataChangeListener();
        this.sfcL2FlowProgrammer = sfcL2FlowProgrammer;
    }

    private void installDefaultNextHopEntry(long sfpId, ServicePathHop servicePathHop, boolean isAddFlow) {

        int curSFSrcVlan;
        String curSFSrcMac;
        LocatorType curSFLocatorType;
        MacAddressLocator curSFMacAddressLocator;

        this.sfcL2FlowProgrammer.setNodeInfo(servicePathHop.getServiceFunctionForwarder());

        ServiceFunction sf = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(servicePathHop.getServiceFunctionName());
        List<SfDataPlaneLocator> curSfDataPlaneLocatorList = sf.getSfDataPlaneLocator();

        for (SfDataPlaneLocator sfDataPlanelocator : curSfDataPlaneLocatorList) {
            curSFLocatorType = sfDataPlanelocator.getLocatorType();

            if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                curSFMacAddressLocator = (MacAddressLocator) sfDataPlanelocator.getLocatorType();
                curSFSrcMac = curSFMacAddressLocator.getMac().getValue();
                curSFSrcVlan = curSFMacAddressLocator.getVlanId();

                // install ingress flow for each of the dataplane locator
                this.sfcL2FlowProgrammer.configureDefaultNextHopFlow(sfpId, curSFSrcMac, curSFSrcVlan, isAddFlow);
            }
            break;
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

        // SFP create
        Map<InstanceIdentifier<?>, DataObject> dataCreatedConfigurationObject;
        dataCreatedConfigurationObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPaths) {
                ServiceFunctionPaths createdServiceFunctionPaths = (ServiceFunctionPaths) entry.getValue();

                configureSffFlows(createdServiceFunctionPaths, true);

            }
        }

        // SFP update

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject;
        dataUpdatedConfigurationObject = change.getUpdatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionPaths && (!(dataCreatedConfigurationObject
                    .containsKey(entry.getKey()))))) {
                ServiceFunctionPaths updatedServiceFunctionPaths = (ServiceFunctionPaths) entry.getValue();
                configureSffFlows(updatedServiceFunctionPaths, true);
            }
        }

        // SFP delete
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
                this.sfcL2FlowProgrammer.setNodeInfo(curSFFName);
                this.sfcL2FlowProgrammer.configureIngressTransportFlow(isAddFlow);
                this.sfcL2FlowProgrammer.configureSffNextHopDefaultFlow(isAddFlow);

                ServiceFunctionForwarder curSff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(curSFFName);

                List<SffDataPlaneLocator> curSffDataPlanelocatorList = curSff.getSffDataPlaneLocator();
                for (SffDataPlaneLocator curSffDataPlanelocator : curSffDataPlanelocatorList) {
                    LocatorType curSFFLocatorType = curSffDataPlanelocator.getDataPlaneLocator().getLocatorType();
                    if (curSFFLocatorType.getImplementedInterface().equals(Mac.class)) {
                        int curSFFVlan = ((MacAddressLocator) curSffDataPlanelocator.getDataPlaneLocator()
                                .getLocatorType()).getVlanId();
                        this.sfcL2FlowProgrammer.configureIngressFlow(curSFFVlan, isAddFlow);
                        // TODO add support for multiple data plane locators
                    }
                }

                ServiceFunction curSF = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(servicePathHopCur.getServiceFunctionName());

                List<SfDataPlaneLocator> curSfDataPlaneLocatorList = curSF.getSfDataPlaneLocator();
                for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                    LocatorType curSFLocatorType = curSfDataPlanelocator.getLocatorType();
                    if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                        int curSFVlan = ((MacAddressLocator) curSfDataPlanelocator.getLocatorType()).getVlanId();
                        // install ingress flow for dataplane locator of the SF
                        this.sfcL2FlowProgrammer.configureIngressFlow(curSFVlan, isAddFlow);

                        // TODO add support for multiple data plane locators
                    }
                }

                if (servicePathHopPrev == null) {
                    installDefaultNextHopEntry(sfp.getPathId(), servicePathHopCur, isAddFlow);
                } else {
                    String prevSFSrcMac = null;

                    ServiceFunction prevSF;
                    ServiceFunctionForwarder prevSFF;
                    List<SfDataPlaneLocator> prevSFDataPlaneLocatorList;

                    prevSFF = SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(servicePathHopPrev.getServiceFunctionForwarder());
                    prevSF  = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(servicePathHopPrev.getServiceFunctionName());

                    prevSFDataPlaneLocatorList = prevSF.getSfDataPlaneLocator();

                    // check whether the prev and current sffs are same
                    if (servicePathHopPrev.getServiceFunctionForwarder().equals(curSFFName)) {
                        for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                            LocatorType curSFLocatorType = curSfDataPlanelocator.getLocatorType();
                            if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                                MacAddressLocator macAddressLocator;
                                macAddressLocator = (MacAddressLocator) curSFLocatorType;
                                String dstMac = macAddressLocator.getMac().getValue();
                                int dstVlan = macAddressLocator.getVlanId();

                                for (SfDataPlaneLocator prevSFDataPlanelocator : prevSFDataPlaneLocatorList) {
                                    LocatorType prevSFLocatorType = prevSFDataPlanelocator.getLocatorType();
                                    if (prevSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                                        prevSFSrcMac = ((MacAddressLocator) prevSFLocatorType).getMac().getValue();
                                        this.sfcL2FlowProgrammer.configureNextHopFlow(sfp.getPathId(), prevSFSrcMac, dstMac, dstVlan, isAddFlow);

                                        // TODO add support for multiple data
                                        // plane
                                        // locators
                                    }
                                }
                                break;
                            }
                        }
                    } else {
                        for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                            LocatorType curSFLocatorType = curSfDataPlanelocator.getLocatorType();
                            if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                                MacAddressLocator macAddressLocator;

                                macAddressLocator = (MacAddressLocator) curSFLocatorType;
                                String dstMac = macAddressLocator.getMac().getValue();
                                int dstVlan = macAddressLocator.getVlanId();
                                // Install one flow in the current Sff
                                for (SffDataPlaneLocator curSffDataPlanelocator : curSffDataPlanelocatorList) {
                                    String srcMacSff = ((MacAddressLocator) curSffDataPlanelocator
                                            .getDataPlaneLocator().getLocatorType()).getMac().getValue();
                                    this.sfcL2FlowProgrammer.configureNextHopFlow(sfp.getPathId(), srcMacSff, dstMac, dstVlan, isAddFlow);
                                }

                                // TODO add support for multiple data plane
                                // locators
                                break;
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
                                this.sfcL2FlowProgrammer.setNodeInfo(servicePathHopPrev.getServiceFunctionForwarder());

                                for (SfDataPlaneLocator prevSFDataPlanelocator : prevSFDataPlaneLocatorList) {
                                    LocatorType prevSFLocatorType = prevSFDataPlanelocator.getLocatorType();
                                    if (prevSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                                        prevSFSrcMac = ((MacAddressLocator) prevSFLocatorType).getMac().getValue();
                                        this.sfcL2FlowProgrammer.configureNextHopFlow(sfp.getPathId(), prevSFSrcMac, dstMacSff, dstVlanSff, isAddFlow);
                                    }
                                }

                                // TODO add support for multiple data plane locators
                                break;
                            }
                        }

                    }
                }
                servicePathHopPrev = servicePathHopCur;
                servicePathHopCur = null;
            }

        }
    }

}
