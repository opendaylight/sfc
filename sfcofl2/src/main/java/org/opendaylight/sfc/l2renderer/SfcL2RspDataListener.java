/**
 * Copyright (c) 2014 by Ericsson and others. All rights reserved.
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
import org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mpls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class has will be notified when changes are mad to Rendered Service Paths.
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @version 0.1
 * <p/>
 * @since 2015-01-27
 */
public class SfcL2RspDataListener extends SfcL2AbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2RspDataListener.class);
    private static final String INGRESS_STR = "ingress";
    private static final String EGRESS_STR = "egress";
    private SfcL2FlowProgrammer sfcL2FlowProgrammer;
    // each time onDataChanged is called, store the SFs and SFFs internally
    // so we dont have to query the DataStore repeatadly for the same thing
    private Map<String, ServiceFunction> serviceFunctions;
    private Map<String, ServiceFunctionForwarder> serviceFunctionFowarders;
    private boolean addFlow;

    public SfcL2RspDataListener(DataBroker dataBroker, SfcL2FlowProgrammer sfcL2FlowProgrammer) {
        setDataBroker(dataBroker);
        setIID(OpendaylightSfc.RSP_ENTRY_IID);
        registerAsDataChangeListener(LogicalDatastoreType.OPERATIONAL);
        this.sfcL2FlowProgrammer = sfcL2FlowProgrammer;
        this.serviceFunctions = new HashMap<String, ServiceFunction>();
        this.serviceFunctionFowarders = new HashMap<String, ServiceFunctionForwarder>();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        // Currently we dont need to do anything for the OriginalData

        // configureSffFlows will do a check for each SFF to see
        // if its Openflow Enabled, and if not, skip it

        this.addFlow = true;

        // RSP create
        Map<InstanceIdentifier<?>, DataObject> dataCreatedConfigurationObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath) {
                LOG.info("SfcL2RspDataListener.onDataChanged RSP {}", ((RenderedServicePath) entry.getValue()).getName());
                processRenderedServicePath((RenderedServicePath) entry.getValue(), true);
            }
        }

        // RSP update
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof RenderedServicePath && (!(dataCreatedConfigurationObject.containsKey(entry.getKey()))))) {
                processRenderedServicePath((RenderedServicePath) entry.getValue(), true);
            }
        }

        // RSP delete
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier<?> instanceIdentifier : dataRemovedConfigurationIID) {
            this.addFlow = false;
            DataObject dataObject = change.getOriginalData().get(instanceIdentifier);
            if (dataObject instanceof RenderedServicePath) {
                processRenderedServicePath((RenderedServicePath) dataObject, false);
            }
        }

        // Reset the internal SF and SFF storage
        this.serviceFunctions.clear();
        this.serviceFunctionFowarders.clear();

        // TODO need to initialize the tables for each SFF (MatchAny->Goto Table N)
        //      keep a map<sffName, boolean> to only initialize them once
    }

    /**
     * Return the named ServiceFunction
     * Acts as a local cache to not have to go to DataStore so often
     * First look in internal storage, if its not there
     * get it from the DataStore and store it internally
     * 
     * @param sfName - The SF Name to search for
     * @return - The ServiceFunction object, or null if not found
     */
    private ServiceFunction getServiceFunction(final String sfName) {
        ServiceFunction sf = this.serviceFunctions.get(sfName);
        if(sf == null) {
            sf = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(sfName);
            if(sf != null) {
                this.serviceFunctions.put(sfName, sf);
            }
        }

        return sf;
    }

    /**
     * Return the named ServiceFunctionForwarder
     * Acts as a local cache to not have to go to DataStore so often
     * First look in internal storage, if its not there
     * get it from the DataStore and store it internally
     * 
     * @param sffName - The SFF Name to search for
     * @return The ServiceFunctionForwarder object, or null if not found
     */
    // 
    private ServiceFunctionForwarder getServiceFunctionForwarder(final String sffName) {
        ServiceFunctionForwarder sff = this.serviceFunctionFowarders.get(sffName);
        if(sff == null) {
            sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(sffName);
            if(sff != null) {
                this.serviceFunctionFowarders.put(sffName, sff);
            }
        }

        return sff;
    }

    /**
     * Return a named SffDataPlaneLocator
     * 
     * @param sff - The SFF to search in
     * @param dplName - The name of the DPL to look for
     * @return SffDataPlaneLocator or null if not found
     */
    private SffDataPlaneLocator getSffDataPlaneLocator(ServiceFunctionForwarder sff, String dplName) {
        SffDataPlaneLocator sffDpl = null;

        List<SffDataPlaneLocator> sffDataPlanelocatorList = sff.getSffDataPlaneLocator();
        for (SffDataPlaneLocator sffDataPlanelocator : sffDataPlanelocatorList) {
            if(sffDataPlanelocator.getName().toLowerCase().contains(dplName)) {
                sffDpl = sffDataPlanelocator;
                break;
            }
        }

        return sffDpl;
    }

    /**
     * Return a named SffSfDataPlaneLocator
     * 
     * @param sff - The SFF to search in
     * @param sfName - The name of the DPL to look for
     * @return SffSfDataPlaneLocator or null if not found
     */
    private SffSfDataPlaneLocator getSffSfDataPlaneLocator(ServiceFunctionForwarder sff, String sfName) {
        SffSfDataPlaneLocator sffSfDpl = null;

        List<ServiceFunctionDictionary> sffSfDictList = sff.getServiceFunctionDictionary();
        for (ServiceFunctionDictionary sffSfDict : sffSfDictList) {
            if(sffSfDict.getName() == sfName) {
                sffSfDpl = sffSfDict.getSffSfDataPlaneLocator();
            }
        }

        return sffSfDpl;
    }

    /**
     * Call the sfcL2FlowProgrammer to write the Transport Ingress Flow
     * 
     * @param sffName - which SFF to write the flow to
     * @param dpl - details about the transport to write
     */
    private void writeSffTransportIngressFlow(final String sffName, DataPlaneLocator dpl) {
        configureIngressTransportFlow(sffName, etherType, this.addFlow);
    }

    // TODO using the nextHopName, we need to figure out the appropriate metadata
    // TODO if nextHopName is null, Goto table 2 (ACL)
    private void writeSffIngressFlow(final String sffName, DataPlaneLocator dpl, final String nextHopName) {
        LocatorType curSFFLocatorType = dpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = curSFFLocatorType.getImplementedInterface();

        // Mac and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            Integer vlanTag = ((MacAddressLocator) curSFFLocatorType).getVlanId();
            MacAddress mac = ((MacAddressLocator) curSFFLocatorType).getMac();
            if(vlanTag == null) {
                this.sfcL2FlowProgrammer.configureMacIngressFlow(sffName, mac.getValue(), nextHop, this.addFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanIngressFlow(sffName, vlanTag, nextHop, this.addFlow);
            }
        }
        else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            int mplsLabel = ((MacAddressLocator) curSFFLocatorType).getVlanId();
            this.sfcL2FlowProgrammer.configureMplsIngressFlow(sffName, mplsLabel, nextHop, this.addFlow);
        }
        // TODO add VxLAN
    }

    // TODO using the nextHopName, we need to figure out the appropriate metadata
    private void writeSffNextHopFlow(final String sffName, final String nextHopName, DataPlaneLocator dpl) {
    }

    private void writeSffTransportEgressFlow(final String sffName, DataPlaneLocator dpl) {
    }

    /**
     * Populate the TransportIngress and Ingress Flow Tables
     * @param entry
     */
    private void configureSffIngressFlows(SffGraph.SffGraphEntry entry) {
        if(entry.getDstSff() == SffGraph.EGRESS) {
            // Nothing to be done for the ingress tables, skip it
            return;
        }

        ServiceFunctionForwarder sffDst = getServiceFunctionForwarder(entry.getDstSff());
        SffDataPlaneLocator sffDstIngressDpl = getSffDataPlaneLocator(sffDst, SffGraph.INGRESS);
        if(sffDstIngressDpl == null) {
            LOG.warn("SFF {} does not have a DataPlaneLocator named ingress", entry.getDstSff());
        }

        // Configure the switch Ingress flow(s)
        if(entry.getSrcSff() == SffGraph.INGRESS) {
            // configure the Transport Ingress Flow using the sffDstIngressDpl
            writeSffTransportIngressFlow(entry.getDstSff(), sffDstIngressDpl.getDataPlaneLocator());
        }

        // Configure the SF related flows
        SffSfDataPlaneLocator sfDpl = getSffSfDataPlaneLocator(sffDst, entry.getToSf());
        if(sfDpl == null) {
            LOG.warn("Cant find SFF [{}] to SF [{}] DataPlaneLocator", entry.getDstSff(), entry.getToSf());
        }
        // configure the Ingress-SFF-SF ingress Flow
        writeSffIngressFlow(entry.getDstSff(), sffDstIngressDpl.getDataPlaneLocator(), entry.getToSf());
        // configure the SF Transport Ingress Flow
        writeSffTransportIngressFlow(entry.getDstSff(), sfDpl);
        // configure the SF Ingress Flow
        writeSffIngressFlow(entry.getDstSff(), sfDpl, null);

        ServiceFunctionForwarder sffSrc = getServiceFunctionForwarder(entry.getSrcSff());
        SffDataPlaneLocator sffSrcEgressDpl = getSffDataPlaneLocator(sffSrc, SffGraph.EGRESS);
        if(sffSrcEgressDpl == null && sfDpl == null) {
            LOG.warn("SFF {} does not have a DataPlaneLocator named egress", entry.getSrcSff());
        }
        // configure SFF-SFF-SF ingress flow using sffSrcEgressDpl and entry.toSf
        writeSffIngressFlow(entry.getDstSff(), sffSrcEgressDpl.getDataPlaneLocator(), entry.getToSf());
    }

    /**
     * Populate the NextHop and TransportEgress Flow Tables
     * @param entry
     */
    private void configureSffEgressFlows(SffGraph.SffGraphEntry entry) {
        ServiceFunctionForwarder sffDst = getServiceFunctionForwarder(entry.getDstSff());
        SffSfDataPlaneLocator sfDpl = getSffSfDataPlaneLocator(sffDst, entry.getToSf());
        if(sfDpl != null) {
            // Configure the SF NextHop using sfDpl
            writeSffNextHopFlow(entry.getDstSff(), entry.getToSf(), sfDpl);
            // Configure the SF Transport Egress using sfDpl
            writeSffTransportEgressFlow(entry.getDstSff(), sfDpl);
        }

        if(entry.getSrcSff() == SffGraph.INGRESS || entry.getDstSff() == SffGraph.EGRESS) {
            // Nothing else to be done for the egress tables
            return;
        }

        SffDataPlaneLocator sffDstIngressDpl = getSffDataPlaneLocator(sffDst, SffGraph.EGRESS);
        // Configure the SFF-SFF NextHop using the sffDstIngressDpl
        writeSffNextHopFlow(entry.getDstSff(), entry.getDstSff(), sffDstIngressDpl.getDataPlaneLocator());
        // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
        writeSffTransportEgressFlow(entry.getDstSff(), sffDstIngressDpl.getDataPlaneLocator());
    }


    private void processRenderedServicePath(RenderedServicePath rsp, boolean isAddFlow) {
        // Setting to INGRESS for the first graph entry, which is the RSP Ingress
        String prevSffName = SffGraph.INGRESS;
        SffGraph sffGraph = new SffGraph();

        //
        // Populate the SFF Connection Graph
        //
        Iterator<RenderedServicePathHop> servicePathHopIter = rsp.getRenderedServicePathHop().iterator();
        while (servicePathHopIter.hasNext()) {
            RenderedServicePathHop rspHop = servicePathHopIter.next();
            String curSffName = rspHop.getServiceFunctionForwarder();
            String sfName = rspHop.getServiceFunctionName();

            LOG.info("SfcL2RspDataListener.configureSffFlows renderedServicePathHop {} prevSffName {} curSffName {} sfName {}",
                    rspHop.getHopNumber(), prevSffName, curSffName, sfName);

            sffGraph.addEntry(prevSffName, curSffName, sfName);
            prevSffName = curSffName;
        }
        // Add the final connection, which will be the RSP Egress
        sffGraph.addEntry(prevSffName, SffGraph.EGRESS);

        //
        // Now process the entries in the SFF Graph
        //
        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.iterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            configureSffIngressFlows(entry);
            configureSffEgressFlows(entry);
        }
    }

    private void installDefaultNextHopEntry(long sfpId, RenderedServicePathHop servicePathHop, boolean isAddFlow) {

        int curSFSrcVlan;
        String curSFSrcMac;
        LocatorType curSFLocatorType;
        MacAddressLocator curSFMacAddressLocator;

        ServiceFunction sf = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(servicePathHop.getServiceFunctionName());
        List<SfDataPlaneLocator> curSfDataPlaneLocatorList = sf.getSfDataPlaneLocator();

        for (SfDataPlaneLocator sfDataPlanelocator : curSfDataPlaneLocatorList) {
            curSFLocatorType = sfDataPlanelocator.getLocatorType();

            if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                curSFMacAddressLocator = (MacAddressLocator) sfDataPlanelocator.getLocatorType();
                curSFSrcMac = curSFMacAddressLocator.getMac().getValue();
                curSFSrcVlan = curSFMacAddressLocator.getVlanId();

                // install ingress flow for each of the dataplane locator
                this.sfcL2FlowProgrammer.configureDefaultNextHopFlow(
                        servicePathHop.getServiceFunctionForwarder(), sfpId, curSFSrcMac, curSFSrcVlan, isAddFlow);
            }
            break;
        }
    }

    //
    // For each SFF Data Plane Locator, configure the SFF flows
    //
    private void processSffDpl(String sffName, SffDataPlaneLocator sffDpl, boolean isAddFlow) {
        // Expecting the DPL name to contain "ingress" or "egress"
        // all other DPL names will be considered SFF-SF DPLs
        String dplName = sffDpl.getName();
        LocatorType curSFFLocatorType = sffDpl.getDataPlaneLocator().getLocatorType();
        Class<? extends DataContainer> implementedInterface = curSFFLocatorType.getImplementedInterface();

        boolean isIngress = false;
        if(dplName.toLowerCase().contains(INGRESS_STR)) {
            isIngress = true;
        } else if(!dplName.toLowerCase().contains(EGRESS_STR)) {
            // SFF-SF DPLs will be handled elsewhere
            return;
        }

        // Mac and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            Integer vlanTag = ((MacAddressLocator) curSFFLocatorType).getVlanId();
            MacAddress mac = ((MacAddressLocator) curSFFLocatorType).getMac();
            if(vlanTag == null) {
                this.sfcL2FlowProgrammer.configureMacIngressFlow(sffName, mac.getValue(), isAddFlow);
                this.sfcL2FlowProgrammer.configureEgressTransportFlow(sffName, dstMac, dstVlan, isAddFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanIngressFlow(sffName, vlanTag, isAddFlow);
                this.sfcL2FlowProgrammer.configureEgressTransportFlow(sffName, dstMac, dstVlan, isAddFlow);
            }
        // MPLS
        } else if (implementedInterface.equals(Mpls.class)) {
            int mplsLabel = ((MacAddressLocator) curSFFLocatorType).getVlanId();
            this.sfcL2FlowProgrammer.configureMplsIngressFlow(sffName, mplsLabel, isAddFlow);
            this.sfcL2FlowProgrammer.configureEgressTransportFlow(sffName, dstMac, dstVlan, isAddFlow);
        }
    }

    private void configureSffFlows2(RenderedServicePath rsp, boolean isAddFlow) {

        // Each Service Function in the Service Function Path
        Iterator<RenderedServicePathHop> servicePathHopIter = rsp.getRenderedServicePathHop().iterator();
        while (servicePathHopIter.hasNext()) {
            RenderedServicePathHop servicePathHopCur = servicePathHopIter.next();

            String curSFFName = servicePathHopCur.getServiceFunctionForwarder();
            LOG.info("SfcL2RspDataListener.configureSffFlows servicePathHopCur {} curSFName {} curSFFName {}",
                    servicePathHopCur.getHopNumber(), servicePathHopCur.getServiceFunctionName(), curSFFName);

            // Only configure OpenFlow Capable SFFs
            if(!SfcOpenflowUtils.isSffOpenFlowCapable(curSFFName)) {
                // TODO what if the switch we're discarding now connects later???
                //      Should we have a switch listener, and when it connects, send it its config?
                LOG.info("SFF {} is NOT flow capable, skipping it", curSFFName);
                continue;
            }

            this.sfcL2FlowProgrammer.configureIngressTransportFlow(curSFFName, isAddFlow);
            this.sfcL2FlowProgrammer.configureSffNextHopDefaultFlow(curSFFName, isAddFlow);

            //
            // Process the SFF Data Plane Locators
            //
            ServiceFunctionForwarder curSff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(curSFFName);

            List<SffDataPlaneLocator> curSffDataPlanelocatorList = curSff.getSffDataPlaneLocator();
            for (SffDataPlaneLocator curSffDataPlanelocator : curSffDataPlanelocatorList) {
                processSffDpl(curSFFName, curSffDataPlanelocator, isAddFlow);
            }

            //
            // Process the SF Data Plane Locators
            //
            ServiceFunction curSF = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(servicePathHopCur.getServiceFunctionName());

            List<SfDataPlaneLocator> curSfDataPlaneLocatorList = curSF.getSfDataPlaneLocator();
            for (SfDataPlaneLocator curSfDataPlanelocator : curSfDataPlaneLocatorList) {
                LocatorType curSFLocatorType = curSfDataPlanelocator.getLocatorType();
                if (curSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                    int curSFVlan = ((MacAddressLocator) curSfDataPlanelocator.getLocatorType()).getVlanId();
                    // install ingress flow for dataplane locator of the SF
                    this.sfcL2FlowProgrammer.configureVlanIngressFlow(curSFFName, curSFVlan, isAddFlow);
                }
            }

            if (servicePathHopPrev == null) {
                installDefaultNextHopEntry(rsp.getPathId(), servicePathHopCur, isAddFlow);
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
                                    this.sfcL2FlowProgrammer.configureNextHopFlow(
                                            curSFFName, rsp.getPathId(), prevSFSrcMac, dstMac, dstVlan, isAddFlow);

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
                                this.sfcL2FlowProgrammer.configureNextHopFlow(
                                        curSFFName, rsp.getPathId(), srcMacSff, dstMac, dstVlan, isAddFlow);
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

                            for (SfDataPlaneLocator prevSFDataPlanelocator : prevSFDataPlaneLocatorList) {
                                LocatorType prevSFLocatorType = prevSFDataPlanelocator.getLocatorType();
                                if (prevSFLocatorType.getImplementedInterface().equals(Mac.class)) {
                                    prevSFSrcMac = ((MacAddressLocator) prevSFLocatorType).getMac().getValue();
                                    this.sfcL2FlowProgrammer.configureNextHopFlow(
                                            servicePathHopPrev.getServiceFunctionForwarder(), rsp.getPathId(), prevSFSrcMac, dstMacSff, dstVlanSff, isAddFlow);
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
