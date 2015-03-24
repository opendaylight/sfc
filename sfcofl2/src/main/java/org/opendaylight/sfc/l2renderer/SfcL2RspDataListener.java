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
    // Later these should be parameterized per SFF
    private static final int GW_PORT = 3;
    private static final int SF_PORT = 2;
    private static final int SFF_SFF_PORT = 1;

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
            if(sffDataPlanelocator.getName().equals(dplName)) {
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

    /**
     * Given the SFF ingress DPL, return the SFF egress DPL. Assuming the SFFs will have 2 DPLs, so the
     * egress is the DPL that has a name different than the ingress DPL.
     *
     * @param sffName - The SFF to search in
     * @param sffIngressName - The name of the SFF ingress DPL
     * @return The egress SFF DPL name or null if not found
     */
    private String getSffEgressDplName(final String sffName, final String sffIngressName) {
        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);

        List<SffDataPlaneLocator> sffDataPlanelocatorList = sff.getSffDataPlaneLocator();
        if(sffDataPlanelocatorList.size() == 1) {
            // Handle the case where the SFF only has 1 DPL
            return sffDataPlanelocatorList.get(0).getName();
        }
        for (SffDataPlaneLocator sffDataPlanelocator : sffDataPlanelocatorList) {
            if(!sffDataPlanelocator.getName().equals(sffIngressName)) {
                return sffDataPlanelocator.getName();
            }
        }

        return null;
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
            ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
            if(sff == null) {
                LOG.error("configureSffIngressFlow SFF {} does not exist", sffName);
                return;
            }

            LOG.info("Initializing SFF [{}]", sffName);
            this.sfcL2FlowProgrammer.configureTransportIngressTableMatchAny( sff.getServiceNode(), true, true);
            this.sfcL2FlowProgrammer.configureIngressTableMatchAny(          sff.getServiceNode(), false, true);
            this.sfcL2FlowProgrammer.configureAclTableMatchAny(              sff.getServiceNode(), false, true);
            this.sfcL2FlowProgrammer.configureNextHopTableMatchAny(          sff.getServiceNode(), false, true);
            //this.sfcL2FlowProgrammer.configureTransportEgressTableMatchAny(sff.getServiceNode(), true, true);

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

        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
        if(sff == null) {
            LOG.error("configureSffIngressFlow SFF {} does not exist", sffName);
            return;
        }
        String sffNodeName = sff.getServiceNode();

        LocatorType sffLocatorType = dpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            if(((MacAddressLocator) sffLocatorType).getVlanId() == null) {
                this.sfcL2FlowProgrammer.configureIpv4TransportIngressFlow(sffNodeName, this.addFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanTransportIngressFlow(sffNodeName, this.addFlow);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            this.sfcL2FlowProgrammer.configureMplsTransportIngressFlow(sffNodeName, this.addFlow);
        }
        // TODO add VxLAN
    }

    /**
     * Configure an SFF Ingress Flow
     *
     * @param sffName - Where to write the flow
     * @param hopDpl - Data Plane Locator details
     * @param pathId - Which Service PathId the flow belongs to
     */
    private void configureSffIngressFlow(final String sffName, DataPlaneLocator hopDpl, final long pathId) {
        LOG.info("configureSffIngressFlow sff [{}] pathId [{}]", sffName, pathId);

        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
        if(sff == null) {
            LOG.error("configureSffIngressFlow SFF {} does not exist", sffName);
            return;
        }
        String sffNodeName = sff.getServiceNode();

        LocatorType sffLocatorType = hopDpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            Integer vlanTag = ((MacAddressLocator) sffLocatorType).getVlanId();
            MacAddress mac = ((MacAddressLocator) sffLocatorType).getMac();
            if(vlanTag == null) {
                this.sfcL2FlowProgrammer.configureMacIngressFlow(sffNodeName, mac.getValue(), pathId, this.addFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanIngressFlow(sffNodeName, vlanTag, pathId, this.addFlow);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            long mplsLabel = ((MplsLocator) sffLocatorType).getMplsLabel();
            this.sfcL2FlowProgrammer.configureMplsIngressFlow(sffNodeName, mplsLabel, pathId, this.addFlow);
        }
        // TODO add VxLAN
    }

    private void configureSffNextHopFlow(final String sffName, DataPlaneLocator srcDpl, DataPlaneLocator dstDpl, final long pathId) {
        LOG.info("configureSffNextHopFlow sff [{}] pathId [{}]", sffName, pathId);

        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
        if(sff == null) {
            LOG.error("configureSffNextHopFlow SFF {} does not exist", sffName);
            return;
        }

        String srcMac = null;
        String dstMac = null;

        LocatorType srcSffLocatorType = (srcDpl != null) ? srcDpl.getLocatorType() : null;
        LocatorType dstSffLocatorType = dstDpl.getLocatorType();
            // Assuming srcDpl and dstDpl are of the same type
        Class<? extends DataContainer> implementedInterface = dstSffLocatorType.getImplementedInterface();

        // Mac
        if (implementedInterface.equals(Mac.class)) {
            if(srcSffLocatorType != null) {
                // If the srcSffLocatorType, then the packet is entering SFC and we dont know from where
                srcMac = ((MacAddressLocator) srcSffLocatorType).getMac().getValue();
            }
            dstMac = ((MacAddressLocator) dstSffLocatorType).getMac().getValue();
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            if(srcSffLocatorType != null) {
                // If the srcSffLocatorType, then the packet is entering SFC and we dont know from where
                srcMac = ((MplsLocator) srcSffLocatorType).getMacAddress().getValue();
            }
            dstMac = ((MplsLocator) dstSffLocatorType).getMacAddress().getValue();
        }
        // TODO add VxLAN
        else {
            return;
        }

        this.sfcL2FlowProgrammer.configureNextHopFlow(sff.getServiceNode(), pathId, srcMac, dstMac, this.addFlow);
    }

    private void configureSffTransportEgressFlow(
            final String sffName, DataPlaneLocator srcDpl, DataPlaneLocator dstDpl, DataPlaneLocator hopDpl, int port) {
        LOG.info("configureSffTransportEgressFlow sff [{}]", sffName);

        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
        if(sff == null) {
            LOG.error("configureSffTransportEgressFlow SFF {} does not exist", sffName);
            return;
        }
        String sffNodeName = sff.getServiceNode();

        LocatorType hopLocatorType = hopDpl.getLocatorType();
        LocatorType srcLocatorType = srcDpl.getLocatorType();
        LocatorType dstLocatorType = (dstDpl == null ? null : dstDpl.getLocatorType());
        Class<? extends DataContainer> implementedInterface = hopLocatorType.getImplementedInterface();

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            Integer vlanTag = ((MacAddressLocator) hopLocatorType).getVlanId();
            String srcMac = ((MacAddressLocator) srcLocatorType).getMac().getValue();
            String dstMac = dstLocatorType == null ?
                    null : ((MacAddressLocator) dstLocatorType).getMac().getValue();
            if(vlanTag == null) {
                this.sfcL2FlowProgrammer.configureMacTransportEgressFlow(
                        sffNodeName, srcMac, dstMac, port, this.addFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanTransportEgressFlow(
                        sffNodeName, srcMac, dstMac, vlanTag, port, this.addFlow);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            long mplsLabel = ((MplsLocator) hopLocatorType).getMplsLabel();
            String srcMac = ((MplsLocator) srcLocatorType).getMacAddress().getValue();
            String dstMac = dstLocatorType ==null ?
                    null : ((MplsLocator) dstLocatorType).getMacAddress().getValue();
            this.sfcL2FlowProgrammer.configureMplsTransportEgressFlow(
                    sffNodeName, srcMac, dstMac, mplsLabel, port, this.addFlow);
        }
        // TODO add VxLAN
    }

    /**
     * Populate the TransportIngress and Ingress Flow Tables
     * @param entry
     */
    private void configureSffIngress(SffGraph.SffGraphEntry entry, SffGraph sffGraph) {
        LOG.info("configureSffIngress srcSff [{}] dstSff [{}] sf [{}] pathId [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSf(), entry.getPathId());

        final String sffDstName = entry.getDstSff();

        if(sffDstName.equals(SffGraph.EGRESS)) {
            // Nothing to be done for the ingress tables, skip it
            return;
        }

        ServiceFunctionForwarder sffDst = getServiceFunctionForwarder(sffDstName);
        SffDataPlaneLocator sffDstIngressDpl =
                getSffDataPlaneLocator(sffDst, sffGraph.getSffIngressDpl(sffDstName, entry.getPathId()));
        if(sffDstIngressDpl == null) {
            LOG.warn("SFF [{}] does not have a DataPlaneLocator named ingress", sffDstName);
            return;
        }

        // This is the HOP DPL details between srcSFF and dstSFF, for example: VLAN ID 100
        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(entry.getDstSff(), entry.getPathId());

        // Configure the SF related flows
        SffSfDataPlaneLocator sfDpl = getSffSfDataPlaneLocator(sffDst, entry.getSf());
        if(sfDpl == null) {
            LOG.warn("Cant find SFF [{}] to SF [{}] DataPlaneLocator", sffDstName, entry.getSf());
            return;
        }
        // configure the Ingress-SFF-SF ingress Flow
        configureSffIngressFlow(sffDstName, dstHopIngressDpl, entry.getPathId());
        // configure the SF Transport Ingress Flow
        configureSffTransportIngressFlow(sffDstName, sfDpl);
        // configure the SF Ingress Flow
        configureSffIngressFlow(sffDstName, sfDpl, entry.getPathId());

        // Configure the Service Chain Ingress flow(s)
        if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // configure the SFF Transport Ingress Flow using the sffDstIngressDpl
            configureSffTransportIngressFlow(sffDstName, dstHopIngressDpl);
            return;
        }

        // TODO now that we have the dstHopIngressDpl, sffSrc and sffSrcEgressDpl arent necessary
        ServiceFunctionForwarder sffSrc = getServiceFunctionForwarder(entry.getSrcSff());
        SffDataPlaneLocator sffSrcEgressDpl =
                getSffDataPlaneLocator(sffSrc, sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
        if(sffSrcEgressDpl == null) {
            LOG.warn("SFF [{}] does not have a DataPlaneLocator named egress", entry.getSrcSff());
            return;
        }
        // configure SFF-SFF-SF ingress flow using sffSrcEgressDpl and entry.toSf
        configureSffIngressFlow(sffDstName, dstHopIngressDpl, entry.getPathId());
    }

    /**
     * Populate the NextHop and TransportEgress Flow Tables
     * @param entry
     */
    private void configureSffEgress(SffGraph.SffGraphEntry entry, SffGraph sffGraph) {
        LOG.info("configureSffEgress srcSff [{}] dstSff [{}] sf [{}] pathId [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSf(), entry.getPathId());

        /* These are the SFFGraph entries and how the NextHops are calculated:
         * (retrieved with: grep addEntry karaf.log | awk -F "|" '{print $6}')
         *
         * "openflow:2" => SFF1, "openflow:2" => SFF2
         *  srcSff        dstSff       SF     pathId => SFF, NHop, MACsrc
         *  [ingress]     [openflow:2] [sf1]  [1]    => 1,   SF1,  GW1/null  (dont put macSrc and use lower priority)
         *  [openflow:2]  [openflow:3] [sf2]  [1]    => 1,   SFF2, SF1
         *                                           => 2,   SF2,  SFF1ulEgr
         *  [openflow:3]  [egress]     [null] [1]    => 2,   GW2,  SFF2dlEgr (dont have GW macDst, leave out of nextHop)
         *  [ingress]     [openflow:3] [sf2]  [2]    => 2,   SF2,  GW2/null  (dont put macSrc and use lower priority)
         *  [openflow:3]  [openflow:2] [sf1]  [2]    => 2,   SFF1, SF2
         *                                           => 1,   SF1,  SFF2dlEgr
         *  [openflow:2]  [egress]     [null] [2]    => 1,   GW1,  SFF1ulEgr (dont have GW macDst, leave out of nextHop)
         */

        // This is the HOP DPL details between srcSFF and dstSFF, for example: VLAN ID 100
        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(entry.getDstSff(), entry.getPathId());

        ServiceFunctionForwarder sffSrc = getServiceFunctionForwarder(entry.getSrcSff());
        SffDataPlaneLocator sffSrcEgressDpl = null;
        if(entry.getDstSff().equals(SffGraph.EGRESS)) {
            sffSrcEgressDpl =
                    getSffDataPlaneLocator(sffSrc, sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));

            // Configure the SFF-Egress Transport Egress
            configureSffTransportEgressFlow(
                    entry.getSrcSff(),
                    sffSrcEgressDpl.getDataPlaneLocator(),
                    null,
                    sffGraph.getPathEgressDpl(entry.getPathId()),
                    GW_PORT);

            // Nothing else to be done for the egress tables
            return;
        }

        // Configure NextHop flows TO the SFs
        ServiceFunctionForwarder sffDst = getServiceFunctionForwarder(entry.getDstSff());
        ServiceFunction sfDst = getServiceFunction(entry.getSf());
        SfDataPlaneLocator sfDstDpl = getSfDataPlaneLocator(sfDst);
        if(sfDstDpl != null) {
            if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
                // Configure the GW-SFF-SF NextHop using sfDpl
                configureSffNextHopFlow(entry.getDstSff(), null, sfDstDpl, entry.getPathId());
            } else {
                sffSrcEgressDpl =
                        getSffDataPlaneLocator(sffSrc, sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
                // Configure the SFF-SFF-SF NextHop using sfDpl
                configureSffNextHopFlow(entry.getDstSff(), sffSrcEgressDpl.getDataPlaneLocator(), sfDstDpl, entry.getPathId());
            }

            SffSfDataPlaneLocator sffSfDstDpl = getSffSfDataPlaneLocator(sffDst, entry.getSf());
            // Configure the SFF-SF Transport Egress using sfDpl
            configureSffTransportEgressFlow(entry.getDstSff(), sffSfDstDpl, sfDstDpl, sfDstDpl, SF_PORT);
        }

        if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // Nothing else to be done for the egress tables
            return;
        }

        // TODO what if there is more than one SF in the dict?
        // TODO add srcSf to SffGraph and change current sf to dstSf
        ServiceFunction sfSrc = getServiceFunction(sffSrc.getServiceFunctionDictionary().get(0).getName());
        SfDataPlaneLocator sfSrcDpl = getSfDataPlaneLocator(sfSrc);

        SffDataPlaneLocator sffDstIngressDpl =
                getSffDataPlaneLocator(sffDst, sffGraph.getSffIngressDpl(entry.getDstSff(), entry.getPathId()));

        // Configure the SFF-SFF NextHop using the sfDpl and sffDstIngressDpl
        configureSffNextHopFlow(
                entry.getSrcSff(), sfSrcDpl, sffDstIngressDpl.getDataPlaneLocator(), entry.getPathId());

        // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
        configureSffTransportEgressFlow(
                entry.getSrcSff(),
                sffSrcEgressDpl.getDataPlaneLocator(),
                sffDstIngressDpl.getDataPlaneLocator(),
                dstHopIngressDpl,
                SFF_SFF_PORT);
    }


    private void processRenderedServicePath(RenderedServicePath rsp, boolean isAddFlow) {
        // Setting to INGRESS for the first graph entry, which is the RSP Ingress
        String prevSffName = SffGraph.INGRESS;
        SffGraph sffGraph = new SffGraph();

        // TODO we never consider the case where traffic just flows
        //      through the SFF and doesnt go to an SF on the SFF

        //
        // Populate the SFF Connection Graph and SFF ingress/egress DPLs
        //
        Iterator<RenderedServicePathHop> servicePathHopIter = rsp.getRenderedServicePathHop().iterator();
        while (servicePathHopIter.hasNext()) {
            RenderedServicePathHop rspHop = servicePathHopIter.next();
            String curSffName = rspHop.getServiceFunctionForwarder();
            String sfName = rspHop.getServiceFunctionName();
            String curSffIngressName = rspHop.getServiceFunctionForwarderLocator();

            LOG.info("processRenderedServicePath pathId [{}] renderedServicePathHop [{}] Sff [{}] SF [{}] Sff Ingress Locator [{}]",
                    rsp.getPathId(), rspHop.getHopNumber(), curSffName, sfName, curSffIngressName);

            sffGraph.addGraphEntry(prevSffName, curSffName, sfName, rsp.getPathId());
            sffGraph.addSffDpls(curSffName, rsp.getPathId(), curSffIngressName,
                    getSffEgressDplName(curSffName, curSffIngressName),
                    rspHop.getRenderedServicePathHopDataPlaneLocator());

            prevSffName = curSffName;
        }
        // Add the final connection, which will be the RSP Egress
        sffGraph.addGraphEntry(prevSffName, SffGraph.EGRESS, rsp.getPathId());

        sffGraph.setPathEgressDpl(rsp.getPathId(), rsp.getRenderedServicePathEgressDataPlaneLocator());

        //
        // Now process the entries in the SFF Graph
        //
        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.iterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            if(!entry.getDstSff().equals(SffGraph.EGRESS)) {
                initializeSff(entry.getDstSff());
            }
            configureSffIngress(entry, sffGraph);
            configureSffEgress(entry, sffGraph);
        }
    }
}
