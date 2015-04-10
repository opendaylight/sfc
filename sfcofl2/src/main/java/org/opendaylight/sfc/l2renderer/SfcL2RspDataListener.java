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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
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
    // TODO Later these should be parameterized per SFF
    private static final int GW_PORT = 3;
    private static final int SF_PORT = 1;
    private static final int SFF_SFF_PORT = 2;
    private static final int VXLAN_GPE_NSH_UDP_PORT = 6633;

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
        String sffNodeName = getSffServiceNodeName(sffName);
        if(sffNodeName == null) {
            LOG.error("initializeSff SFF [{}] does not exist", sffName);
            return;
        }

        if(!getSffInitialized(sffName)) {
            LOG.info("Initializing SFF [{}] node [{}]", sffName, sffNodeName);
            this.sfcL2FlowProgrammer.configureTransportIngressTableMatchAny( sffNodeName, true, true);
            this.sfcL2FlowProgrammer.configureIngressTableMatchAny(          sffNodeName, false, true);
            this.sfcL2FlowProgrammer.configureAclTableMatchAny(              sffNodeName, true, true);
            this.sfcL2FlowProgrammer.configureNextHopTableMatchAny(          sffNodeName, false, true);
            this.sfcL2FlowProgrammer.configureTransportEgressTableMatchAny(  sffNodeName, true, true);

            setSffInitialized(sffName, true);
        }
    }

    private String getSffServiceNodeName(final String sffName) {
        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
        if(sff == null) {
            return null;
        }

        return sff.getServiceNode();
    }

    /**
     * Call the sfcL2FlowProgrammer to write the Transport Ingress Flow
     *
     * @param sffName - which SFF to write the flow to
     * @param dpl - details about the transport to write
     */
    private void configureSffTransportIngressFlow(final String sffName, DataPlaneLocator dpl) {
        String sffNodeName = getSffServiceNodeName(sffName);
        if(sffNodeName == null) {
            LOG.error("configureSffTransportIngressFlow SFF {} does not exist", sffName);
            return;
        }

        LOG.info("configureSffTransportIngressFlow sff [{}] node [{}]", sffName, sffNodeName);

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
        } else if (implementedInterface.equals(Ip.class)) {
           //VxLAN-gpe, it is IP flow with VLAN tag
           if (dpl.getTransport().equals(VxlanGpe.class)) {
                //Only support VxLAN-gpe + NSH currently
                this.sfcL2FlowProgrammer.configureVxlanGpeTransportIngressFlow(sffNodeName, VXLAN_GPE_NSH_UDP_PORT, this.addFlow);
           }
        }
    }

    private void configureSffIngressFlow(final String sffName, DataPlaneLocator dpl, final long pathId, final short serviceIndex) {
        String sffNodeName = getSffServiceNodeName(sffName);
        if(sffNodeName == null) {
            LOG.error("configureSffIngressFlow SFF {} does not exist", sffName);
            return;
        }

        LOG.info("configureSffIngressFlow sff [{}] node [{}] pathId [{}] serviceIndex [{}]", sffName, sffNodeName, pathId, serviceIndex);

        LocatorType sffLocatorType = dpl.getLocatorType();
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
        } else if (implementedInterface.equals(Ip.class)) {
           //VxLAN-gpe, it is IP/UDP flow with VLAN tag
           if (dpl.getTransport().equals(VxlanGpe.class)) {
                long nsp = pathId;
                short nsi = serviceIndex; //TODO get current hop number
                this.sfcL2FlowProgrammer.configureVxlanGpeIngressFlow(sffNodeName, nsp, nsi, pathId, this.addFlow);
           }
        }
    }

    private void configureSffNextHopFlow(final String sffName, DataPlaneLocator srcDpl, DataPlaneLocator dstDpl, final long pathId, final short serviceIndex) {
        String sffNodeName = getSffServiceNodeName(sffName);
        if(sffNodeName == null) {
            LOG.error("configureSffNextHopFlow SFF {} does not exist", sffName);
            return;
        }

        LOG.info("configureSffNextHopFlow sff [{}] pathId [{}] serviceIndex [{}]", sffName, pathId, serviceIndex);

        if(srcDpl == null && dstDpl == null) {
            LOG.error("configureSffNextHopFlow sff [{}] node [{}] pathId [{}] both srcDpl and dstDpl are null",
                    sffName, sffNodeName, pathId);
            return;
        }

        String srcMac = null;
        String dstMac = null;

        LocatorType srcSffLocatorType = ((srcDpl == null) ? null : srcDpl.getLocatorType());
        LocatorType dstSffLocatorType = ((dstDpl == null) ? null : dstDpl.getLocatorType());
        LocatorType transportLocatorType = ((srcDpl == null) ? dstSffLocatorType : srcSffLocatorType);

        // Assuming srcDpl and dstDpl are of the same type
        Class<? extends DataContainer> implementedInterface = transportLocatorType.getImplementedInterface();

        // Mac
        if (implementedInterface.equals(Mac.class)) {
            srcMac = ((srcDpl == null) ? null : ((MacAddressLocator) srcSffLocatorType).getMac().getValue());
            dstMac = ((dstDpl == null) ? null : ((MacAddressLocator) dstSffLocatorType).getMac().getValue());
            this.sfcL2FlowProgrammer.configureNextHopFlow(sffNodeName, pathId, srcMac, dstMac, this.addFlow);
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            this.sfcL2FlowProgrammer.configureNextHopFlow(sffNodeName, pathId, srcMac, dstMac, this.addFlow);
        } else if (implementedInterface.equals(Ip.class)) {
           //VxLAN-gpe, it is IP/UDP flow with VLAN tag
           if (srcDpl != null && srcDpl.getTransport().equals(VxlanGpe.class)) {
                String srcIp = ((IpPortLocator) srcSffLocatorType).getIp().toString();
                String dstIp = ((IpPortLocator) dstSffLocatorType).getIp().toString();
                long nsp = pathId;
                short nsi = serviceIndex; //TODO get current hop number
                this.sfcL2FlowProgrammer.configureVxlanGpeNextHopFlow(sffNodeName, pathId, srcIp, dstIp, nsp, nsi, this.addFlow);
           }
        }
    }

    private void configureSffTransportEgressFlow(
            final String sffName, DataPlaneLocator srcDpl, DataPlaneLocator dstDpl, int port, long pathId, short serviceIndex, boolean isSf) {

        String sffNodeName = getSffServiceNodeName(sffName);
        if(sffNodeName == null) {
            LOG.error("configureSffTransportEgressFlow SFF {} does not exist", sffName);
            return;
        }

        LOG.info("configureSffTransportEgressFlow sff [{}] node [{}]", sffName, sffNodeName);

        // Either the srcDpl or the dstDpl can be null, but not both
        if(srcDpl == null && dstDpl == null) {
            LOG.error("configureSffTransportEgressFlow sff [{}] pathId [{}] both dstDpl and srcDpl are null", sffName, pathId);
            return;
        }

        LocatorType srcLocatorType = (srcDpl == null) ? null : srcDpl.getLocatorType();
        LocatorType dstLocatorType = (dstDpl == null) ? null : dstDpl.getLocatorType();
        LocatorType transportLocatorType = (srcLocatorType == null) ? dstLocatorType : srcLocatorType;

        Class<? extends DataContainer> implementedInterface = transportLocatorType.getImplementedInterface();

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            Integer vlanTag = ((MacAddressLocator) transportLocatorType).getVlanId();

            String srcMac = srcLocatorType == null ?
                    null : ((MacAddressLocator) srcLocatorType).getMac().getValue();
            String dstMac = dstLocatorType == null ?
                    null : ((MacAddressLocator) dstLocatorType).getMac().getValue();
            if(vlanTag == null) {
                this.sfcL2FlowProgrammer.configureMacTransportEgressFlow(
                        sffNodeName, srcMac, dstMac, port, pathId, isSf, this.addFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanTransportEgressFlow(
                        sffNodeName, srcMac, dstMac, vlanTag, port, pathId, isSf, this.addFlow);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            long mplsLabel = ((MplsLocator) transportLocatorType).getMplsLabel();

            // TODO the mac was removed from MPLS, need to get it now from the SFF DPL
            String srcMac = null;
            String dstMac = null;
            this.sfcL2FlowProgrammer.configureMplsTransportEgressFlow(
                    sffNodeName, srcMac, dstMac, mplsLabel, port, pathId, isSf, this.addFlow);
        } else if (implementedInterface.equals(Ip.class)) {
           //VxLAN-gpe, it is IP/UDP flow with VLAN tag
           if (srcDpl != null && srcDpl.getTransport().equals(VxlanGpe.class)) {
                String srcIp = ((IpPortLocator) srcLocatorType).getIp().toString();
                String dstIp = ((IpPortLocator) dstLocatorType).getIp().toString();
                long nsp = pathId;
                short nsi = serviceIndex;
                this.sfcL2FlowProgrammer.configureVxlanGpeTransportEgressFlow(sffNodeName, srcIp, dstIp, nsp, nsi, port, pathId, isSf, this.addFlow);
           }
        }
    }

    /**
     * Populate the TransportIngress and Ingress Flow Tables
     * @param entry
     */
    private void configureSffIngress(SffGraph.SffGraphEntry entry) {
        LOG.info("configureSffIngress srcSff [{}] dstSff [{}] sf [{}] pathId [{}] serviceIndex [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSf(), entry.getPathId(), entry.getServiceIndex());

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
        configureSffIngressFlow(sffDstName, sffDstIngressDpl.getDataPlaneLocator(), entry.getPathId(), entry.getServiceIndex());
        // configure the SF Transport Ingress Flow
        configureSffTransportIngressFlow(sffDstName, sfDpl);
        // configure the SF Ingress Flow, setting negative pathId so it wont
        // set metadata and will goto classification table instead of NextHop
        configureSffIngressFlow(sffDstName, sfDpl, -1, entry.getServiceIndex());

        // Configure the SF ACL flow
        this.sfcL2FlowProgrammer.configureClassificationFlow(
                getSffServiceNodeName(sffDstName), entry.getPathId(), true);

        // Configure the Service Chain Ingress flow(s)
        if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // configure the SFF Transport Ingress Flow using the sffDstIngressDpl
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
        configureSffIngressFlow(sffDstName, sffSrcEgressDpl.getDataPlaneLocator(), entry.getPathId(), entry.getServiceIndex());
    }

    /**
     * Populate the NextHop and TransportEgress Flow Tables
     * @param entry
     */
    private void configureSffEgress(SffGraph.SffGraphEntry entry) {
        LOG.info("configureSffEgress srcSff [{}] dstSff [{}] sf [{}] pathId [{}] serviceIndex [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSf(), entry.getPathId(), entry.getServiceIndex());

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

        if(entry.getDstSff().equals(SffGraph.EGRESS)) {
            // Nothing to be done for the egress tables, skip it
            return;
        }

        ServiceFunctionForwarder sffDst = getServiceFunctionForwarder(entry.getDstSff());
        SffSfDataPlaneLocator sffSfDpl = getSffSfDataPlaneLocator(sffDst, entry.getSf());
        ServiceFunction sf = getServiceFunction(entry.getSf());
        SfDataPlaneLocator sfDpl = getSfDataPlaneLocator(sf);
        if(sffSfDpl != null && sfDpl != null) {
            if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
                // Configure the GW-SFF-SF NextHop using sfDpl, srcMac is null
                configureSffNextHopFlow(entry.getDstSff(), null, sffSfDpl, entry.getPathId(), entry.getServiceIndex());
            } else {
                // Configure the SFF-SFF-SF NextHop using sfDpl
                configureSffNextHopFlow(entry.getDstSff(), sffSfDpl, sfDpl, entry.getPathId(), entry.getServiceIndex());
            }
            // Configure the SFF-SF Transport Egress using sfDpl
            // TODO the srcDpl should either be null for GW or that of the previous SFF
            configureSffTransportEgressFlow(entry.getDstSff(), null, sfDpl, SF_PORT, entry.getPathId(), entry.getServiceIndex(), true);
        }

        if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // Nothing else to be done for the egress tables
            return;
        }

        ServiceFunctionForwarder sffSrc = getServiceFunctionForwarder(entry.getSrcSff());
        SffDataPlaneLocator sffSrcIngressDpl = getSffDataPlaneLocator(sffSrc, SffGraph.EGRESS);
        SffDataPlaneLocator sffDstIngressDpl = getSffDataPlaneLocator(sffDst, SffGraph.EGRESS);
        // Configure the SFF-SFF NextHop using the sffDstIngressDpl
        configureSffNextHopFlow(entry.getDstSff(),
                sffSrcIngressDpl == null ? null : sffSrcIngressDpl.getDataPlaneLocator(),
                sffDstIngressDpl == null ? null : sffDstIngressDpl.getDataPlaneLocator(),
                entry.getPathId(), entry.getServiceIndex());
        // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
        configureSffTransportEgressFlow(
                entry.getDstSff(),
                sfDpl,
                (sffDstIngressDpl == null ? null : sffDstIngressDpl.getDataPlaneLocator()),
                SFF_SFF_PORT, entry.getPathId(), entry.getServiceIndex(), false);
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

            LOG.info("processRenderedServicePath pathId [{}] renderedServicePathHop [{}]",
                    rsp.getPathId(), rspHop.getHopNumber());

            sffGraph.addEntry(prevSffName, curSffName, sfName, rsp.getPathId(), rspHop.getServiceIndex());
            prevSffName = curSffName;
        }
        // Add the final connection, which will be the RSP Egress
        sffGraph.addEntry(prevSffName, SffGraph.EGRESS, rsp.getPathId(), (short)0);

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
