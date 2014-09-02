/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ofsfc.provider;

import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.SfcProviderRestAPI;
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

/**
 * This class is the DataListener for SFP changes.
 * 
 * <p>
 * 
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2014-06-30
 */

public class OfSfcProviderSfpDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory
            .getLogger(OfSfcProviderSfpDataListener.class);
    private static final OfSfcProvider odlSfc = OfSfcProvider
            .getOpendaylightSfcObj();

    private void installDefaultNextHopEntry(long sfpId, ServicePathHop sfpSfHop) {

        OfSfcProviderSffFlowWriter.getInstance().setNodeInfo(
                sfpSfHop.getServiceFunctionForwarder());

        ServiceFunction sf = SfcProviderServiceFunctionAPI
                .readServiceFunction(sfpSfHop.getServiceFunctionName());
        List<SfDataPlaneLocator> curSfDataPlaneLocatorList = sf
                .getSfDataPlaneLocator();
        LocatorType curSfLt;
        String srcMac;
        int srcVlan;

        for (SfDataPlaneLocator sfDataPlanelocator : curSfDataPlaneLocatorList) {
            curSfLt = sfDataPlanelocator.getLocatorType();
            if (curSfLt.getImplementedInterface().equals(Mac.class)) {

                srcMac = ((MacAddressLocator) sfDataPlanelocator
                        .getLocatorType()).getMac().getValue();
                srcVlan = ((MacAddressLocator) sfDataPlanelocator
                        .getLocatorType()).getVlanId();
                // install ingress flow for each of the dataplane locator
                OfSfcProviderSffFlowWriter.getInstance()
                        .writeDefaultNextHopFlow(sfpId, srcMac, srcVlan);
            }
        }
    }

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        LOG.debug("\n########## Start: {}", Thread.currentThread()
                .getStackTrace()[1]);
        /*
         * when a SFP is created we will process and send it to southbound
         * devices. But first we need to make sure all info is present or we
         * will pass.
         */
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change
                .getUpdatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject
                .entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPaths) {

                ServiceFunctionPaths updatedServiceFunctionPaths = (ServiceFunctionPaths) entry
                        .getValue();
                Object[] serviceForwarderObj = { updatedServiceFunctionPaths };
                Class[] serviceForwarderClass = { ServiceFunctionPaths.class };
                odlSfc.executor.execute(SfcProviderRestAPI
                        .getPutServiceFunctionPaths(serviceForwarderObj,
                                serviceForwarderClass));

                //
                // Write the flows to the SFF
                // For now these will be both ACL and NextHop flow tables,
                // later on logic needs to be added here to write different flow
                // entry types depending on the individual switch encapsulation,
                // etc
                //
                writeSffFlows(updatedServiceFunctionPaths);
            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread()
                .getStackTrace()[1]);
    }

    private void writeSffFlows(ServiceFunctionPaths updatedServiceFunctionPaths) {
        Iterator<ServiceFunctionPath> sfpIter = updatedServiceFunctionPaths
                .getServiceFunctionPath().iterator();

        // Each Service Function Path configured
        while (sfpIter.hasNext()) {
            ServiceFunctionPath sfp = sfpIter.next();
            Iterator<ServicePathHop> sfpSpHop = sfp.getServicePathHop()
                    .iterator();

            ServicePathHop oldsfHop = null;
            ServicePathHop cursfHop = null;
            // test
            // System.out.println("Testing default flow");
            // SfcProviderSffFlowWriter.getInstance().setNodeInfo("openflow:1");
            // System.out.println("Testing default flow--1");
            // SfcProviderSffFlowWriter.getInstance().writeIngressFlow(100);
            // System.out.println("Testing default flow--2");

            // Each Service Function in the Service Function Path
            while (sfpSpHop.hasNext()) {

                cursfHop = sfpSpHop.next();

                // This is the current SFF
                OfSfcProviderSffFlowWriter.getInstance().setNodeInfo(
                        cursfHop.getServiceFunctionForwarder());

                OfSfcProviderSffFlowWriter.getInstance()
                        .writeSffNextHopDefaultFlow();

                ServiceFunctionForwarder curSff = SfcProviderServiceForwarderAPI
                        .readServiceFunctionForwarder(odlSfc.getDataProvider(),
                                cursfHop.getServiceFunctionForwarder());

                // TODO multiple SFF dataplane locators right now assuming one
                List<SffDataPlaneLocator> curSffDataPlanelocatorList = curSff
                        .getSffDataPlaneLocator();
                LocatorType curSffLt;
                int vlan;

                // currently assumes only one member in a list
                for (SffDataPlaneLocator curSffDataPlanelocator : curSffDataPlanelocatorList) {
                    curSffLt = curSffDataPlanelocator.getDataPlaneLocator()
                            .getLocatorType();
                    if (curSffLt.getImplementedInterface().equals(Mac.class)) {
                        vlan = ((MacAddressLocator) curSffDataPlanelocator
                                .getDataPlaneLocator().getLocatorType())
                                .getVlanId();
                        // install ingress flow for each of the dataplane
                        // locator
                        OfSfcProviderSffFlowWriter.getInstance()
                                .writeIngressFlow(vlan);
                    } else {
                        System.out.println("\n Not a Mac Result");
                        OfSfcProviderSffFlowWriter.getInstance()
                                .writeIngressFlow(300);
                    }
                }

                // Get everything needed to write to the Next Hop table
                ServiceFunction cursf = SfcProviderServiceFunctionAPI
                        .readServiceFunction(cursfHop.getServiceFunctionName());

                List<SfDataPlaneLocator> curSfDataPlaneLocatorList = cursf
                        .getSfDataPlaneLocator();

                for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                    vlan = ((MacAddressLocator) curSfDataPlanelocator
                            .getLocatorType()).getVlanId();
                    // install ingress flow for dataplane locator of the SF
                    OfSfcProviderSffFlowWriter.getInstance().writeIngressFlow(
                            vlan);
                }

                if (oldsfHop == null) {
                    installDefaultNextHopEntry(sfp.getPathId(), cursfHop);
                    oldsfHop = cursfHop;
                } else {

                    ServiceFunctionForwarder oldSff = SfcProviderServiceForwarderAPI
                            .readServiceFunctionForwarder(
                                    odlSfc.getDataProvider(),
                                    oldsfHop.getServiceFunctionForwarder());
                    List<SffDataPlaneLocator> oldSffDataPlanelocatorList = oldSff
                            .getSffDataPlaneLocator();

                    ServiceFunction oldsf = SfcProviderServiceFunctionAPI
                            .readServiceFunction(oldsfHop
                                    .getServiceFunctionName());

                    // LocatorType oldsfLt = oldsf.getLocatorType();
                    List<SfDataPlaneLocator> oldSfDataPlaneLocatorList = oldsf
                            .getSfDataPlaneLocator();
                    String srcMac = null;
                    for (SfDataPlaneLocator oldSfDataPlanelocator : oldSfDataPlaneLocatorList) {
                        srcMac = ((MacAddressLocator) oldSfDataPlanelocator
                                .getLocatorType()).getMac().toString();
                    }
                    // check whether the prev and current sffs are same
                    if (oldSff.equals(curSff)) {
                        for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                            String dstMac = ((MacAddressLocator) curSfDataPlanelocator
                                    .getLocatorType()).getMac().toString();
                            int dstVlan = ((MacAddressLocator) curSfDataPlanelocator
                                    .getLocatorType()).getVlanId();
                            OfSfcProviderSffFlowWriter.getInstance()
                                    .writeNextHopFlow(srcMac, dstMac, dstVlan,
                                            sfp.getPathId());
                        }
                    } else {
                        for (SffDataPlaneLocator curSffDataPlanelocator : curSffDataPlanelocatorList) {
                            String srcMacSff = ((MacAddressLocator) curSffDataPlanelocator
                                    .getDataPlaneLocator().getLocatorType())
                                    .getMac().toString();
                            for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                                String dstMac = ((MacAddressLocator) curSfDataPlanelocator
                                        .getLocatorType()).getMac().toString();
                                int dstVlan = ((MacAddressLocator) curSfDataPlanelocator
                                        .getLocatorType()).getVlanId();
                                // Install one flow in the current Sff
                                OfSfcProviderSffFlowWriter.getInstance()
                                        .writeNextHopFlow(srcMacSff, dstMac,
                                                dstVlan, sfp.getPathId());
                            }
                        }
                        for (SffDataPlaneLocator oldSffDataPlanelocator : oldSffDataPlanelocatorList) {
                            String dstMacSff = ((MacAddressLocator) oldSffDataPlanelocator
                                    .getDataPlaneLocator().getLocatorType())
                                    .getMac().toString();
                            int dstVlanSff = ((MacAddressLocator) oldSffDataPlanelocator
                                    .getDataPlaneLocator().getLocatorType())
                                    .getVlanId();
                            // Install one flow in the old Sff
                            OfSfcProviderSffFlowWriter
                                    .getInstance()
                                    .setNodeInfo(
                                            oldsfHop.getServiceFunctionForwarder());
                            OfSfcProviderSffFlowWriter.getInstance()
                                    .writeNextHopFlow(srcMac, dstMacSff,
                                            dstVlanSff, sfp.getPathId());
                        }

                    }

                }

            }
        }

    }

}
