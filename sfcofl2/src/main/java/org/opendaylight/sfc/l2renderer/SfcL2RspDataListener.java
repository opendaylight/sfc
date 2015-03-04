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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MplsLocator;
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
    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;
    // each time onDataChanged() is called, store the SFs and SFFs internally
    // so we dont have to query the DataStore repeatedly for the same thing
    private Map<String, ServiceFunction> serviceFunctions;
    private Map<String, ServiceFunctionForwarder> serviceFunctionFowarders;
    private Map<String, Boolean> sffInitialized;
    private boolean addFlow;

    public SfcL2RspDataListener(DataBroker dataBroker, SfcL2FlowProgrammerInterface sfcL2FlowProgrammer) {
        setDataBroker(dataBroker);
        setIID(OpendaylightSfc.RSP_ENTRY_IID);
        registerAsDataChangeListener(LogicalDatastoreType.OPERATIONAL);
        this.sfcL2FlowProgrammer = sfcL2FlowProgrammer;
        this.serviceFunctions = new HashMap<String, ServiceFunction>();
        this.serviceFunctionFowarders = new HashMap<String, ServiceFunctionForwarder>();
        this.sffInitialized = new HashMap<String, Boolean>();
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
     * Return the SfDataPlaneLocator
     *
     * @param sff - The SFF to search in
     * @param dplName - The name of the DPL to look for
     * @return SffDataPlaneLocator or null if not found
     */
    private SfDataPlaneLocator getSfDataPlaneLocator(ServiceFunction sf) {
        // TODO how to tell which SF DPL to use if it has more than one?
        List<SfDataPlaneLocator> sfDataPlanelocatorList = sf.getSfDataPlaneLocator();
        return sfDataPlanelocatorList.get(0);
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
            if(sffSfDict.getName().equals(sfName)) {
                sffSfDpl = sffSfDict.getSffSfDataPlaneLocator();
            }
        }

        return sffSfDpl;
    }

    private boolean getSffInitialized(final String sffName) {
        Boolean isInitialized = sffInitialized.get(sffName);

        if(isInitialized == null) {
            return false;
        }

        return isInitialized.booleanValue();
    }

    private void setSffInitialized(final String sffName, boolean initialized) {
        // If the value is already in the map, its value will be replaced
        sffInitialized.put(sffName, initialized);
    }

    private void initializeSff(final String sffName) {
        if(!getSffInitialized(sffName)) {
            LOG.info("Initializing SFF [{}]", sffName);
            this.sfcL2FlowProgrammer.configureTransportIngressTableMatchAny(sffName, false, true);
            this.sfcL2FlowProgrammer.configureIngressTableMatchAny(sffName, false, true);
            this.sfcL2FlowProgrammer.configureAclTableMatchAny(sffName, false, true);
            this.sfcL2FlowProgrammer.configureNextHopTableMatchAny(sffName, false, true);
            this.sfcL2FlowProgrammer.configureTransportEgressTableMatchAny(sffName, true, true);

            setSffInitialized(sffName, true);
        }
    }

    /**
     * Call the sfcL2FlowProgrammer to write the Transport Ingress Flow
     *
     * @param sffName - which SFF to write the flow to
     * @param dpl - details about the transport to write
     */
    private void configureSffTransportIngressFlow(final String sffName, DataPlaneLocator dpl) {
        LOG.info("configureSffTransportIngressFlow sff [{}]", sffName);

        LocatorType sffLocatorType = dpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            if(((MacAddressLocator) sffLocatorType).getVlanId() == null) {
                this.sfcL2FlowProgrammer.configureIpv4TransportIngressFlow(sffName, this.addFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanTransportIngressFlow(sffName, this.addFlow);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            this.sfcL2FlowProgrammer.configureMplsTransportIngressFlow(sffName, this.addFlow);
        }
        // TODO add VxLAN
    }

    private void configureSffIngressFlow(final String sffName, DataPlaneLocator dpl, final long pathId) {
        LOG.info("configureSffIngressFlow sff [{}] pathId [{}]", sffName, pathId);

        LocatorType sffLocatorType = dpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            Integer vlanTag = ((MacAddressLocator) sffLocatorType).getVlanId();
            MacAddress mac = ((MacAddressLocator) sffLocatorType).getMac();
            if(vlanTag == null) {
                this.sfcL2FlowProgrammer.configureMacIngressFlow(sffName, mac.getValue(), pathId, this.addFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanIngressFlow(sffName, vlanTag, pathId, this.addFlow);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            long mplsLabel = ((MplsLocator) sffLocatorType).getMplsLabel();
            this.sfcL2FlowProgrammer.configureMplsIngressFlow(sffName, mplsLabel, pathId, this.addFlow);
        }
        // TODO add VxLAN
    }

    private void configureSffNextHopFlow(final String sffName, DataPlaneLocator srcDpl, DataPlaneLocator dstDpl, final long pathId) {
        LOG.info("configureSffNextHopFlow sff [{}] pathId [{}]", sffName, pathId);

        MacAddress srcMac = null;
        MacAddress dstMac = null;

        LocatorType srcSffLocatorType = srcDpl.getLocatorType();
        LocatorType dstSffLocatorType = dstDpl.getLocatorType();
            // Assuming srcDpl and dstDpl are of the same type
        Class<? extends DataContainer> implementedInterface = srcSffLocatorType.getImplementedInterface();

        // Mac
        if (implementedInterface.equals(Mac.class)) {
            srcMac = ((MacAddressLocator) srcSffLocatorType).getMac();
            dstMac = ((MacAddressLocator) dstSffLocatorType).getMac();
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            // TODO I added MacAddress to MplsLocator, but was it necessary?
            srcMac = ((MplsLocator) srcSffLocatorType).getMacAddress();
            dstMac = ((MplsLocator) dstSffLocatorType).getMacAddress();
        }
        // TODO add VxLAN

        this.sfcL2FlowProgrammer.configureNextHopFlow(sffName, pathId, srcMac.getValue(), dstMac.getValue(), this.addFlow);
    }

    private void configureSffTransportEgressFlow(final String sffName, DataPlaneLocator dpl) {
        LOG.info("configureSffTransportEgressFlow sff [{}]", sffName);

        LocatorType sffLocatorType = dpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            Integer vlanTag = ((MacAddressLocator) sffLocatorType).getVlanId();
            MacAddress mac = ((MacAddressLocator) sffLocatorType).getMac();
            if(vlanTag == null) {
                this.sfcL2FlowProgrammer.configureMacTransportEgressFlow(sffName, mac.getValue(), this.addFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanTransportEgressFlow(sffName, mac.getValue(), vlanTag, this.addFlow);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            MacAddress mac = ((MplsLocator) sffLocatorType).getMacAddress();
            long mplsLabel = ((MplsLocator) sffLocatorType).getMplsLabel();
            this.sfcL2FlowProgrammer.configureMplsTransportEgressFlow(sffName, mac.getValue(), mplsLabel, this.addFlow);
        }
    }

    /**
     * Populate the TransportIngress and Ingress Flow Tables
     * @param entry
     */
    private void configureSffIngress(SffGraph.SffGraphEntry entry) {
        LOG.info("configureSffIngress srcSff [{}] dstSff [{}] sf [{}] pathId [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSf(), entry.getPathId());

        final String sffDstName = entry.getDstSff();

        if(sffDstName.equals(SffGraph.EGRESS)) {
            // Nothing to be done for the ingress tables, skip it
            return;
        }

        ServiceFunctionForwarder sffDst = getServiceFunctionForwarder(sffDstName);
        SffDataPlaneLocator sffDstIngressDpl = getSffDataPlaneLocator(sffDst, SffGraph.INGRESS);
        if(sffDstIngressDpl == null) {
            LOG.warn("SFF [{}] does not have a DataPlaneLocator named ingress", sffDstName);
            return;
        }

        // Configure the SF related flows
        SffSfDataPlaneLocator sfDpl = getSffSfDataPlaneLocator(sffDst, entry.getSf());
        if(sfDpl == null) {
            LOG.warn("Cant find SFF [{}] to SF [{}] DataPlaneLocator", sffDstName, entry.getSf());
            return;
        }
        // configure the Ingress-SFF-SF ingress Flow
        configureSffIngressFlow(sffDstName, sffDstIngressDpl.getDataPlaneLocator(), entry.getPathId());
        // configure the SF Transport Ingress Flow
        configureSffTransportIngressFlow(sffDstName, sfDpl);
        // configure the SF Ingress Flow
        configureSffIngressFlow(sffDstName, sfDpl, entry.getPathId());

        // Configure the Service Chain Ingress flow(s)
        if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // configure the Transport Ingress Flow using the sffDstIngressDpl
            configureSffTransportIngressFlow(sffDstName, sffDstIngressDpl.getDataPlaneLocator());
            return;
        }

        ServiceFunctionForwarder sffSrc = getServiceFunctionForwarder(entry.getSrcSff());
        SffDataPlaneLocator sffSrcEgressDpl = getSffDataPlaneLocator(sffSrc, SffGraph.EGRESS);
        if(sffSrcEgressDpl == null) {
            LOG.warn("SFF [{}] does not have a DataPlaneLocator named egress", entry.getSrcSff());
            return;
        }
        // configure SFF-SFF-SF ingress flow using sffSrcEgressDpl and entry.toSf
        configureSffIngressFlow(sffDstName, sffSrcEgressDpl.getDataPlaneLocator(), entry.getPathId());
    }

    /**
     * Populate the NextHop and TransportEgress Flow Tables
     * @param entry
     */
    private void configureSffEgress(SffGraph.SffGraphEntry entry) {
        LOG.info("configureSffEgress srcSff [{}] dstSff [{}] sf [{}] pathId [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSf(), entry.getPathId());

        if(entry.getDstSff().equals(SffGraph.EGRESS)) {
            // Nothing to be done for the egress tables, skip it
            return;
        }

        ServiceFunctionForwarder sffDst = getServiceFunctionForwarder(entry.getDstSff());
        SffSfDataPlaneLocator sffSfDpl = getSffSfDataPlaneLocator(sffDst, entry.getSf());
        ServiceFunction sf = getServiceFunction(entry.getSf());
        SfDataPlaneLocator sfDpl = getSfDataPlaneLocator(sf);
        if(sffSfDpl != null && sfDpl != null) {
            // Configure the SFF-SF NextHop using sfDpl
            configureSffNextHopFlow(entry.getDstSff(), sffSfDpl, sfDpl, entry.getPathId());
            // Configure the SFF-SF Transport Egress using sfDpl
            configureSffTransportEgressFlow(entry.getDstSff(), sffSfDpl);
        }

        if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // Nothing else to be done for the egress tables
            return;
        }

        ServiceFunctionForwarder sffSrc = getServiceFunctionForwarder(entry.getSrcSff());
        SffDataPlaneLocator sffSrcIngressDpl = getSffDataPlaneLocator(sffSrc, SffGraph.EGRESS);
        SffDataPlaneLocator sffDstIngressDpl = getSffDataPlaneLocator(sffDst, SffGraph.EGRESS);
        // Configure the SFF-SFF NextHop using the sffDstIngressDpl
        configureSffNextHopFlow(entry.getDstSff(), sffSrcIngressDpl.getDataPlaneLocator(),
                sffDstIngressDpl.getDataPlaneLocator(), entry.getPathId());
        // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
        configureSffTransportEgressFlow(entry.getDstSff(), sffDstIngressDpl.getDataPlaneLocator());
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

            LOG.info("processRenderedServicePath pathId [{}] renderedServicePathHop [{}] Sff [{}] SF [{}] Sff Ingress Locator [{}]",
                    rsp.getPathId(), rspHop.getHopNumber(), curSffName, sfName, rspHop.getServiceFunctionForwarderLocator());

            sffGraph.addEntry(prevSffName, curSffName, sfName, rsp.getPathId());
            prevSffName = curSffName;
        }
        // Add the final connection, which will be the RSP Egress
        sffGraph.addEntry(prevSffName, SffGraph.EGRESS, rsp.getPathId());

        //
        // Now process the entries in the SFF Graph
        //
        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.iterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            if(!entry.getDstSff().equals(SffGraph.EGRESS)) {
                initializeSff(entry.getDstSff());
            }
            configureSffIngress(entry);
            configureSffEgress(entry);
        }
    }
}
