/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunction;
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

public class SfcProviderSfpDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory
            .getLogger(SfcProviderSfpDataListener.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc
            .getOpendaylightSfcObj();

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

    private void installDefaultNextHopEntry(SfpServiceFunction sfpSf) {

        SfcProviderSffFlowWriter.getInstance().setNodeInfo(
                sfpSf.getServiceFunctionForwarder());
        ServiceFunction sf = SfcProviderServiceFunctionAPI
                .readServiceFunction(sfpSf.getName());

        String srcMac = ((MacAddressLocator) sf.getSfDataPlaneLocator())
                .getMac().toString();
        int srcVlan = ((MacAddressLocator) sf.getSfDataPlaneLocator())
                .getVlanId();
        SfcProviderSffFlowWriter.getInstance().writeSffNextHopDefaultFlow(
                srcMac, srcVlan);

    }

    private void writeSffFlows(ServiceFunctionPaths updatedServiceFunctionPaths) {
        Iterator<ServiceFunctionPath> sfpIter = updatedServiceFunctionPaths
                .getServiceFunctionPath().iterator();

        // Each Service Function Path configured
        while (sfpIter.hasNext()) {
            ServiceFunctionPath sfp = sfpIter.next();
            Iterator<SfpServiceFunction> sfpSfIter = sfp
                    .getSfpServiceFunction().iterator();

            SfpServiceFunction oldsfpSf = null;
            SfpServiceFunction newsfpSf = null;
            // Each Service Function in the Service Function Path
            while (sfpSfIter.hasNext()) {

                newsfpSf = sfpSfIter.next();

                // The SFF name should be the name of the actual switch
                SfcProviderSffFlowWriter.getInstance().setNodeInfo(
                        newsfpSf.getServiceFunctionForwarder());

                ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI
                        .readServiceFunctionForwarder(newsfpSf
                                .getServiceFunctionForwarder());
                Iterator<SffDataPlaneLocator> sffDplIter = sff
                        .getSffDataPlaneLocator().iterator();
                Iterator<ServiceFunctionDictionary> sffSfDictIter = sff
                        .getServiceFunctionDictionary().iterator();
                // TODO need to get the inPort and srcMac

                // Get everything needed to write to the Next Hop table
                ServiceFunction sf = SfcProviderServiceFunctionAPI
                        .readServiceFunction(newsfpSf.getName());
                LocatorType sfLt = sf.getSfDataPlaneLocator().getLocatorType();

                if (oldsfpSf == null) {
                    installDefaultNextHopEntry(newsfpSf);
                    oldsfpSf = newsfpSf;
                } else {
                    ServiceFunction oldsf = SfcProviderServiceFunctionAPI
                            .readServiceFunction(oldsfpSf.getName());
                    LocatorType oldsfLt = sf.getSfDataPlaneLocator()
                            .getLocatorType();
                    String srcMac = ((MacAddressLocator) oldsf
                            .getSfDataPlaneLocator()).getMac().toString();
                    int srcVlan = ((MacAddressLocator) oldsf
                            .getSfDataPlaneLocator()).getVlanId();

                    ServiceFunction newsf = SfcProviderServiceFunctionAPI
                            .readServiceFunction(oldsfpSf.getName());
                    LocatorType newsfLt = sf.getSfDataPlaneLocator()
                            .getLocatorType();
                    String dstMac = ((MacAddressLocator) newsf
                            .getSfDataPlaneLocator()).getMac().toString();
                    int dstVlan = ((MacAddressLocator) newsf
                            .getSfDataPlaneLocator()).getVlanId();
                    if (sfLt.getImplementedInterface().equals(Mac.class)) {
                        SfcProviderSffFlowWriter.getInstance().writeSffNextHop(
                                srcMac, srcVlan, dstMac, dstVlan,
                                sfp.getPathId());
                    }

                }

                // TODO<Brady/ the Mac DataPlanLocator choice was
                // temporarily
                // removed
                // due to yangtools bug:
                // https://bugs.opendaylight.org/show_bug.cgi?id=1467
                // TODO<Brady/ get the dst MACs and the outPort from the
                // LocatorType

                // assumption is both Sfs are located on the same Sff

                // What to do if its not a MAC locator??
            }
            ServiceFunction oldsf = SfcProviderServiceFunctionAPI
                    .readServiceFunction(oldsfpSf.getName());
            LocatorType oldsfLt = oldsf.getSfDataPlaneLocator()
                    .getLocatorType();

            String srcMac = ((MacAddressLocator) oldsf.getSfDataPlaneLocator())
                    .getMac().toString();
            int srcVlan = ((MacAddressLocator) oldsf.getSfDataPlaneLocator())
                    .getVlanId();
            SfcProviderSffFlowWriter.getInstance().writeSffNextHopGroup(srcMac,
                    srcVlan, sfp.getPathId(), 1000);
        }
        // This is the other choice, but I dont think we'll be using it
        // else if(sfLt.getImplementedInterface().equals(Ip.class))

        // TODO<Brady/>SfcProviderSffFlowWriter.getInstance().writeSffNextHop(inPort,
        // sfp.getPathId(), srcMac, dstMac, outPort);

        // TODO<shuva/>since the implementation is now only for L2
        // domain we get the mac/vlan for each of the SFs from the list
        // of SFs that belong to a SFP.hence we will provision flows for
        // all of them eg src is from SF1, dest will be SF2 and so on
        // the default is assumed to go to SF1

    }

}
