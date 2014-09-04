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

public class OpenflowSfpDataListener extends OpenflowAbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowSfpDataListener.class);
    private static final OpenflowSfcRenderer odlSfc = OpenflowSfcRenderer.getOpendaylightSfcObj();

    public OpenflowSfpDataListener(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        setIID(SfcInstanceIdentifierUtils.createServiceFunctionPathsPath());
        registerAsDataChangeListener();
    }

    private void installDefaultNextHopEntry(long sfpId, ServicePathHop servicePathHop) {

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
                OpenflowSfcFlowProgrammer.getInstance().writeDefaultNextHopFlow(sfpId, curSFSrcMac, curSFSrcVlan);
            }
        }
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject;
        dataUpdatedConfigurationObject = change.getUpdatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPaths) {
                ServiceFunctionPaths updatedServiceFunctionPaths = (ServiceFunctionPaths) entry.getValue();
                writeSffFlows(updatedServiceFunctionPaths);
            }
        }
    }

    private void writeSffFlows(ServiceFunctionPaths updatedServiceFunctionPaths) {
        Iterator<ServiceFunctionPath> sfpIter = updatedServiceFunctionPaths.getServiceFunctionPath().iterator();
        OpenflowSfcFlowProgrammer flowProgrammer = OpenflowSfcFlowProgrammer.getInstance();
        // Each Service Function Path configured
        while (sfpIter.hasNext()) {
            ServiceFunctionPath sfp = sfpIter.next();
            Iterator<ServicePathHop> sfpSpHop = sfp.getServicePathHop().iterator();

            String curSFFName;
            ServicePathHop servicePathHopPrev = null;
            ServicePathHop servicePathHopCur = null;

            // Each Service Function in the Service Function Path
            while (sfpSpHop.hasNext()) {
                servicePathHopCur = sfpSpHop.next();

                curSFFName = servicePathHopCur.getServiceFunctionForwarder();
                flowProgrammer.setNodeInfo(curSFFName);
                flowProgrammer.writeSffNextHopDefaultFlow();

                ServiceFunctionForwarder curSff = SfcOfL2APIUtil.readServiceFunctionForwarder(odlSfc.getDataProvider(),
                        curSFFName);

                List<SffDataPlaneLocator> curSffDataPlanelocatorList = curSff.getSffDataPlaneLocator();
                for (SffDataPlaneLocator curSffDataPlanelocator : curSffDataPlanelocatorList) {
                    LocatorType curSFFLocatorType = curSffDataPlanelocator.getDataPlaneLocator().getLocatorType();
                    if (curSFFLocatorType.getImplementedInterface().equals(Mac.class)) {
                        int curSFFVlan = ((MacAddressLocator) curSffDataPlanelocator.getDataPlaneLocator()
                                .getLocatorType()).getVlanId();
                        flowProgrammer.writeIngressFlow(curSFFVlan);
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
                        flowProgrammer.writeIngressFlow(curSFVlan);

                        // TODO add support for multiple data plane locators
                        break;
                    }
                }

                if (servicePathHopPrev == null) {
                    installDefaultNextHopEntry(sfp.getPathId(), servicePathHopCur);
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
                        LocatorType curSFLocatorType = prevSFDataPlanelocator.getLocatorType();
                        if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                            prevSFSrcMac = ((MacAddressLocator) prevSFDataPlanelocator.getLocatorType()).getMac()
                                    .toString();

                            // TODO add support for multiple data plane locators
                            break;
                        }
                    }

                    // check whether the prev and current sffs are same
                    if (prevSFF.equals(curSff)) {
                        for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                            LocatorType curSFLocatorType = curSfDataPlanelocator.getLocatorType();
                            if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                                MacAddressLocator macAddressLocator;

                                macAddressLocator = (MacAddressLocator) curSFLocatorType;
                                String dstMac = macAddressLocator.getMac().toString();
                                int dstVlan = macAddressLocator.getVlanId();
                                flowProgrammer.writeNextHopFlow(sfp.getPathId(), prevSFSrcMac, dstMac, dstVlan);

                                // TODO add support for multiple data plane
                                // locators
                                break;
                            }
                        }
                    } else {
                        for (SffDataPlaneLocator curSffDataPlanelocator : curSffDataPlanelocatorList) {
                            String srcMacSff = ((MacAddressLocator) curSffDataPlanelocator.getDataPlaneLocator()
                                    .getLocatorType()).getMac().toString();
                            for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                                LocatorType curSFLocatorType = curSfDataPlanelocator.getLocatorType();
                                if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                                    MacAddressLocator macAddressLocator;

                                    macAddressLocator = (MacAddressLocator) curSFLocatorType;
                                    String dstMac = macAddressLocator.getMac().toString();
                                    int dstVlan = macAddressLocator.getVlanId();
                                    // Install one flow in the current Sff
                                    flowProgrammer.writeNextHopFlow(sfp.getPathId(), srcMacSff, dstMac, dstVlan);

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
                                        .getLocatorType()).getMac().toString();
                                int dstVlanSff = ((MacAddressLocator) prevSFFDataPlanelocator.getDataPlaneLocator()
                                        .getLocatorType()).getVlanId();
                                // Install one flow in the old Sff
                                flowProgrammer.setNodeInfo(servicePathHopPrev.getServiceFunctionForwarder());
                                flowProgrammer.writeNextHopFlow(sfp.getPathId(), prevSFSrcMac, dstMacSff, dstVlanSff);
                                // TODO add support for multiple data plane
                                // locators
                                break;
                            }
                        }

                    }

                }
                servicePathHopPrev = servicePathHopCur;
            }
        }
    }
}
