package org.opendaylight.sfc.l2renderer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.sfc.l2renderer.SffGraph.SffDataPlaneLocators;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MplsLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MplsBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.ServiceFunctionDictionary1;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.port.details.OfsPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcL2RspProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SfcL2RspProcessor.class);
    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;
    // each time onDataChanged() is called, store the SFs and SFFs internally
    // so we dont have to query the DataStore repeatedly for the same thing
    private Map<String, ServiceFunction> serviceFunctions;
    private Map<String, ServiceFunctionForwarder> serviceFunctionFowarders;
    private Map<String, Boolean> sffInitialized;
    private boolean addFlow;
    private int lastMplsLabel;
    private int lastVlanId;

    private static final int VXLAN_GPE_NSH_UDP_PORT = 6633;

    private static final int MPLS_LABEL_INCR_HOP = 1;
    private static final int MPLS_LABEL_INCR_RSP = 100;
    private static final int VLAN_ID_INCR_HOP = 1;
    private static final int VLAN_ID_INCR_RSP = 100;

    // Locator Type Strings TODO should these be moved to SfcProviderAPI?
    private static final String FUNCTION = "function";
    private static final String IP = "ip";
    private static final String LISP = "lisp";
    private static final String MAC = "mac";
    private static final String MPLS = "mpls";

    public SfcL2RspProcessor(SfcL2FlowProgrammerInterface sfcL2FlowProgrammer) {
        this.sfcL2FlowProgrammer = sfcL2FlowProgrammer;
        this.serviceFunctions = new HashMap<String, ServiceFunction>();
        this.serviceFunctionFowarders = new HashMap<String, ServiceFunctionForwarder>();
        this.sffInitialized = new HashMap<String, Boolean>();
        this.lastMplsLabel = 0;
        this.lastVlanId = 0;
    }

    // TODO if this takes too long, consider launching it in a thread
    public void processRenderedServicePath(RenderedServicePath rsp, boolean addFlow) {
        // Reset the internal SF and SFF storage
        this.serviceFunctions.clear();
        this.serviceFunctionFowarders.clear();
        this.addFlow = addFlow;

        // Setting to INGRESS for the first graph entry, which is the RSP Ingress
        String prevSffName = SffGraph.INGRESS;
        SffGraph sffGraph = new SffGraph();

        // TODO we never consider the case where traffic just flows
        //      through the SFF and doesnt go to an SF on the SFF

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

            sffGraph.addGraphEntry(prevSffName, curSffName, sfName, rsp.getPathId(), rspHop.getServiceIndex());
            prevSffName = curSffName;
        }
        // Add the final connection, which will be the RSP Egress
        sffGraph.addGraphEntry(prevSffName, SffGraph.EGRESS, rsp.getPathId(), (short)0);

        //
        // Populate the SFF ingress and egress DPLs from the sffGraph
        //
        processSffDpls(sffGraph, rsp.getTransportType().getName());

        setRspTransports(sffGraph, rsp.getTransportType(), rsp.getPathId());

        // Print the SFF DPLs
        Set<Long> dplKeys = sffGraph.getSffDplKeys();
        for(Long key : dplKeys) {
            Map<String, SffDataPlaneLocators> sffDpls = sffGraph.getSffDplsForPath(key);
            Set<String> sffDplKeys = sffDpls.keySet();
            for(String sffDplKey : sffDplKeys) {
                SffDataPlaneLocators sffDpl = sffDpls.get(sffDplKey);
                LOG.info("SFF [{}] pathId [{}] IngressDpl [{}] EgressDpl [{}] IngressHopDpl Transport [{}] Vlan ID [{}]",
                        sffDpl.getSffName(), sffDpl.getPathId(), sffDpl.getIngressDplName(), sffDpl.getEgressDplName(),
                        ((sffDpl.getIngressHopDpl() == null) ? "null" : sffDpl.getIngressHopDpl().getTransport().getName()),
                        //((Mpls)sffDpl.getIngressHopDpl().getLocatorType()).getMplsLabel());
                        ((sffDpl.getIngressHopDpl() == null) ? "null" : ((Mac)sffDpl.getIngressHopDpl().getLocatorType()).getVlanId()));
            }
        }

        // Print the Path Egress DPLs
        Set<Long> egressDplKeys = sffGraph.getEgressLocatorKeys();
        for(Long pathId : egressDplKeys) {
            DataPlaneLocator dpl = sffGraph.getPathEgressDpl(pathId);
            LOG.info("Path Egress DPL pathId [{}] Dpl Transport [{}] Vlan ID [{}]",
                     pathId,
                     dpl.getTransport().getName(),
                     ((Mac)dpl.getLocatorType()).getVlanId());
        }

        //
        // Now process the entries in the SFF Graph and populate the flow tables
        //
        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            if(!entry.getDstSff().equals(SffGraph.EGRESS)) {
                initializeSff(entry.getDstSff());
            }
            configureSffIngress(entry, sffGraph);
            configureSffEgress(entry, sffGraph);
        }
    }

    /****************************************************************
     *
     *               Flow Table Orchestration methods
     *
     ****************************************************************/

    /**
     * Populate the TransportIngress and Ingress Flow Tables
     * @param entry
     */
    private void configureSffIngress(SffGraph.SffGraphEntry entry, SffGraph sffGraph) {
        LOG.info("configureSffIngress srcSff [{}] dstSff [{}] sf [{}] pathId [{}] serviceIndex [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSf(), entry.getPathId(), entry.getServiceIndex());

        final String sffDstName = entry.getDstSff();

        if(sffDstName.equals(SffGraph.EGRESS)) {
            // Nothing to be done for the ingress tables, skip it
            return;
        }

        ServiceFunctionForwarder sffDst = getServiceFunctionForwarder(sffDstName);
        SffDataPlaneLocator sffDstIngressDpl =
                getSffDataPlaneLocator(sffDst, sffGraph.getSffIngressDpl(sffDstName, entry.getPathId()));
        if(sffDstIngressDpl == null) {
            LOG.warn("configureSffIngress SFF [{}] does not have a DataPlaneLocator for pathId [{}]",
                    sffDstName, entry.getPathId());
            return;
        }

        // This is the HOP DPL details between srcSFF and dstSFF, for example: VLAN ID 100
        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(sffDstName, entry.getPathId());
        if(dstHopIngressDpl == null) {
            LOG.warn("configureSffIngress SFF [{}] Hop Ingress DPL is null for pathId [{}]", sffDstName, entry.getPathId());
            return;
        }

        // Configure the SF related flows
        SffSfDataPlaneLocator sfDpl = getSffSfDataPlaneLocator(sffDst, entry.getSf());
        if(sfDpl == null) {
            LOG.warn("Cant find SFF [{}] to SF [{}] DataPlaneLocator", sffDstName, entry.getSf());
            return;
        }

        // configure the Ingress-SFF-SF ingress Flow
        configureSffIngressFlow(sffDstName, dstHopIngressDpl, entry.getPathId(), entry.getServiceIndex());
        // configure the SF Transport Ingress Flow
        configureSffTransportIngressFlow(sffDstName, sfDpl);
        // configure the SF Ingress Flow, setting negative pathId so it wont
        // set metadata and will goto classification table instead of NextHop
        configureSffIngressFlow(sffDstName, sfDpl, -1, entry.getServiceIndex());

        // Configure the SF ACL flow
        // TODO need to change this to write to the Ingress table instead
        this.sfcL2FlowProgrammer.configureClassificationFlow(
                getSffServiceNodeName(sffDstName), entry.getPathId(), true);

        // Configure the Service Chain Ingress flow(s)
        if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // configure the SFF Transport Ingress Flow using the dstHopIngressDpl
            configureSffTransportIngressFlow(sffDstName, dstHopIngressDpl);
            return;
        }

        // configure SFF-SFF-SF ingress flow using dstHopIngressDpl
        configureSffIngressFlow(sffDstName, dstHopIngressDpl, entry.getPathId(), entry.getServiceIndex());
    }

    /**
     * Populate the NextHop and TransportEgress Flow Tables
     * @param entry
     */
    private void configureSffEgress(SffGraph.SffGraphEntry entry, SffGraph sffGraph) {
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
                    sffSrcEgressDpl,
                    null,
                    sffGraph.getPathEgressDpl(entry.getPathId()),
                    entry.getPathId(),
                    entry.getServiceIndex(),
                    false);

            // Nothing else to be done for the egress tables
            return;
        }

        ServiceFunctionForwarder sffDst = getServiceFunctionForwarder(entry.getDstSff());
        ServiceFunction sfDst = getServiceFunction(entry.getSf());
        SfDataPlaneLocator sfDstDpl = getSfDataPlaneLocator(sfDst);
        if(sfDstDpl != null) {
            if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
                // Configure the GW-SFF-SF NextHop using sfDpl
                configureSffNextHopFlow(entry.getDstSff(), (ServiceFunctionDictionary) null, sfDstDpl, entry.getPathId(), entry.getServiceIndex());
            } else {
                sffSrcEgressDpl =
                        getSffDataPlaneLocator(sffSrc, sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
                // Configure the SFF-SFF-SF NextHop using sfDpl
                ServiceFunctionDictionary sffSrcSfDict = getSffSfDictionary(sffSrc, entry.getSf());
                configureSffNextHopFlow(entry.getDstSff(), sffSrcSfDict, sfDstDpl, entry.getPathId(), entry.getServiceIndex());
            }

            ServiceFunctionDictionary sffSfDict = getSffSfDictionary(sffDst, entry.getSf());
            // Configure the SFF-SF Transport Egress using sfDpl
            configureSffTransportEgressFlow(
                    entry.getDstSff(), sffSfDict, sfDstDpl, sfDstDpl, entry.getPathId(), entry.getServiceIndex(), true);
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
        configureSffNextHopFlow(entry.getSrcSff(),
                                sfSrcDpl,
                                sffDstIngressDpl,
                                entry.getPathId(),
                                entry.getServiceIndex());

        // Configure the SFF-SFF Transport Egress using the sffDstIngressDpl
        configureSffTransportEgressFlow(
                entry.getSrcSff(),
                sffSrcEgressDpl,
                sffDstIngressDpl,
                dstHopIngressDpl,
                entry.getPathId(),
                entry.getServiceIndex(),
                false);
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

    private void configureSffIngressFlow(final String sffName, DataPlaneLocator hopDpl, final long pathId, final short serviceIndex) {
        String sffNodeName = getSffServiceNodeName(sffName);
        if(sffNodeName == null) {
            LOG.error("configureSffIngressFlow SFF {} does not exist", sffName);
            return;
        }

        LOG.info("configureSffIngressFlow sff [{}] node [{}] pathId [{}] serviceIndex [{}]", sffName, sffNodeName, pathId, serviceIndex);

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
        } else if (implementedInterface.equals(Ip.class)) {
           //VxLAN-gpe, it is IP/UDP flow with VLAN tag
           if (hopDpl.getTransport().equals(VxlanGpe.class)) {
                long nsp = pathId;
                short nsi = serviceIndex; //TODO get current hop number
                this.sfcL2FlowProgrammer.configureVxlanGpeIngressFlow(sffNodeName, nsp, nsi, pathId, this.addFlow);
           }
        }
    }

    // Simple pass-through method
    // This is the case of either SFF-SFF-SF or GW-SFF-SF, where the source
    // info is in the SFF SF dictionary and the dstDpl is the SF DPL
    private void configureSffNextHopFlow(final String sffName,
                                         ServiceFunctionDictionary srcSffSfDict,
                                         SfDataPlaneLocator dstSfDpl,
                                         final long pathId,
                                         final short serviceIndex) {
        DataPlaneLocator srcDpl = null;
        String srcMac = null;
        if(srcSffSfDict != null) {
            srcDpl = srcSffSfDict.getSffSfDataPlaneLocator();
            srcMac = getDictPortInfoMac(srcSffSfDict);
        }
        String dstMac = getSfDplMac(dstSfDpl);
        configureSffNextHopFlow(sffName, srcDpl, dstSfDpl, srcMac, dstMac, pathId, serviceIndex);
    }

    // Simple pass-through method
    // This is the case of SF-SFF-SFF flows
    private void configureSffNextHopFlow(final String sffName,
                                         SfDataPlaneLocator srcSfDpl,
                                         SffDataPlaneLocator dstDpl,
                                         final long pathId,
                                         final short serviceIndex) {

        String srcMac = getSfDplMac(srcSfDpl);
        String dstMac = getDplPortInfoMac(dstDpl);
        configureSffNextHopFlow(sffName, srcSfDpl, dstDpl.getDataPlaneLocator(), srcMac, dstMac, pathId, serviceIndex);
    }

    // This version is only used by the previous 2 configureSffNextHopFlow() signatures
    private void configureSffNextHopFlow(final String sffName,
                                         DataPlaneLocator srcDpl,
                                         DataPlaneLocator dstDpl,
                                         String srcMac,
                                         String dstMac,
                                         final long pathId,
                                         final short serviceIndex) {

        String sffNodeName = getSffServiceNodeName(sffName);
        if(sffNodeName == null) {
            LOG.error("configureSffNextHopFlow SFF {} does not exist", sffName);
            return;
        }

        LOG.info("configureSffNextHopFlow sff [{}] pathId [{}] serviceIndex [{}]", sffName, pathId, serviceIndex);

        LocatorType srcSffLocatorType = (srcDpl != null) ? srcDpl.getLocatorType() : null;
        LocatorType dstSffLocatorType = dstDpl.getLocatorType();
            // Assuming srcDpl and dstDpl are of the same type
        Class<? extends DataContainer> implementedInterface = dstSffLocatorType.getImplementedInterface();

        if (implementedInterface.equals(Ip.class)) {
            //VxLAN-gpe, it is IP/UDP flow with VLAN tag
            if (srcDpl != null && srcDpl.getTransport().equals(VxlanGpe.class)) {
                 String srcIp = ((IpPortLocator) srcSffLocatorType).getIp().toString();
                 String dstIp = ((IpPortLocator) dstSffLocatorType).getIp().toString();
                 long nsp = pathId;
                 short nsi = serviceIndex; //TODO get current hop number
                 this.sfcL2FlowProgrammer.configureVxlanGpeNextHopFlow(sffNodeName, pathId, srcIp, dstIp, nsp, nsi, this.addFlow);
            }
        } else {
            // Same thing for Mac/VLAN and MPLS
            this.sfcL2FlowProgrammer.configureNextHopFlow(sffNodeName, pathId, srcMac, dstMac, this.addFlow);
        }
    }

    // TODO I think isSf can be removed from this one, and just pass true to the final signature
    // Simple pass-through method that calculates the srcOfsPort
    // and srcMac from the srcSffSfDict, and the dstMac from the dstDpl
    private void configureSffTransportEgressFlow(final String sffName,
                                                 ServiceFunctionDictionary srcSffSfDict,
                                                 SfDataPlaneLocator dstSfDpl,
                                                 DataPlaneLocator hopDpl,
                                                 long pathId,
                                                 short serviceIndex,
                                                 boolean isSf) {
        DataPlaneLocator srcDpl = srcSffSfDict.getSffSfDataPlaneLocator();
        String srcOfsPortStr = getDictPortInfoPort(srcSffSfDict);
        int srcOfsPort = Integer.valueOf(srcOfsPortStr);
        String srcMac = getDictPortInfoMac(srcSffSfDict);
        String dstMac = getSfDplMac(dstSfDpl);
        configureSffTransportEgressFlow(
                sffName, srcDpl, dstSfDpl, hopDpl, srcOfsPort, srcMac, dstMac, pathId, serviceIndex, isSf);
    }

    // TODO I think isSf can be removed from this one, and just pass false to the final signature
    // Simple pass-through method that calculates the srcOfsPort
    // and srcMac from the srcDpl, and the dstMac from the dstDpl
    private void configureSffTransportEgressFlow(final String sffName,
                                                 SffDataPlaneLocator srcDpl,
                                                 SffDataPlaneLocator dstDpl,
                                                 DataPlaneLocator hopDpl,
                                                 long pathId,
                                                 short serviceIndex,
                                                 boolean isSf) {
        String srcOfsPortStr = getDplPortInfoPort(srcDpl);
        int srcOfsPort = Integer.valueOf(srcOfsPortStr);
        String srcMac = getDplPortInfoMac(srcDpl);
        String dstMac = getDplPortInfoMac(dstDpl);
        configureSffTransportEgressFlow(sffName,
                srcDpl.getDataPlaneLocator(),
                ((dstDpl == null) ? null : dstDpl.getDataPlaneLocator()),
                hopDpl, srcOfsPort, srcMac, dstMac, pathId, serviceIndex, isSf);
    }

    // This version is only used by the previous 2 configureSffTransportEgressFlow() signatures
    private void configureSffTransportEgressFlow(final String sffName,
                                                 DataPlaneLocator srcDpl,
                                                 DataPlaneLocator dstDpl,
                                                 DataPlaneLocator hopDpl,
                                                 int srcOfsPort,
                                                 String srcMac,
                                                 String dstMac,
                                                 long pathId,
                                                 short serviceIndex,
                                                 boolean isSf) {
        if(hopDpl == null) {
            LOG.error("configureSffTransportEgressFlow SFF [{}] hopDpl is null", sffName);
            return;
        }

        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
        if(sff == null) {
            LOG.error("configureSffTransportEgressFlow SFF [{}] does not exist", sffName);
            return;
        }

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

        LocatorType hopLocatorType = hopDpl.getLocatorType();
        LocatorType srcLocatorType = srcDpl.getLocatorType();
        LocatorType dstLocatorType = (dstDpl == null ? null : dstDpl.getLocatorType());
        Class<? extends DataContainer> implementedInterface = hopLocatorType.getImplementedInterface();

        if (implementedInterface.equals(Mac.class)) {
            // Mac and possibly VLAN
            Integer vlanTag = ((MacAddressLocator) hopLocatorType).getVlanId();
            if(vlanTag == null) {
                this.sfcL2FlowProgrammer.configureMacTransportEgressFlow(
                        sffNodeName, srcMac, dstMac, srcOfsPort, pathId, isSf, this.addFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanTransportEgressFlow(
                        sffNodeName, srcMac, dstMac, vlanTag, srcOfsPort, pathId, isSf, this.addFlow);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            long mplsLabel = ((MplsLocator) hopLocatorType).getMplsLabel();
            this.sfcL2FlowProgrammer.configureMplsTransportEgressFlow(
                    sffNodeName, srcMac, dstMac, mplsLabel, srcOfsPort, pathId, isSf, this.addFlow);
        } else if (implementedInterface.equals(Ip.class)) {
           //VxLAN-gpe, it is IP/UDP flow with VLAN tag
           if (srcDpl != null && srcDpl.getTransport().equals(VxlanGpe.class)) {
                String srcIp = ((IpPortLocator) srcLocatorType).getIp().toString();
                String dstIp = ((IpPortLocator) dstLocatorType).getIp().toString();
                long nsp = pathId;
                short nsi = serviceIndex;
                this.sfcL2FlowProgrammer.configureVxlanGpeTransportEgressFlow(
                        sffNodeName, srcIp, dstIp, nsp, nsi, srcOfsPort, pathId, isSf, this.addFlow);
           }
        }
    }


    /****************************************************************
     *
     *                 Process SFF DPL methods
     *
     ****************************************************************/

    private boolean processSffDpls(SffGraph sffGraph, final String rspTransport) {

        // Iterate the entries in the SFF Graph
        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
                continue;
            }

            ServiceFunctionForwarder srcSff = getServiceFunctionForwarder(entry.getSrcSff());
            ServiceFunctionForwarder dstSff = getServiceFunctionForwarder(entry.getDstSff()); // may be null if its EGRESS
            if(dstSff != null) {
                // Set the SFF-SFF Hop DPL
                if(!setSffHopDataPlaneLocators(srcSff, dstSff, rspTransport, entry.getPathId(), sffGraph)) {
                    LOG.error("Unable to get SFF HOP DPLs srcSff [{}] dstSff [{}] transport [{}] pathId [{}]",
                            entry.getSrcSff(), entry.getDstSff(), rspTransport, entry.getPathId());
                    return false;
                }
            }

            if(entry.getDstSff().equals(SffGraph.EGRESS)) {
                // The srcSff ingress DPL was set in the previous loop
                // iteration, now we need to set its egress DPL
                SffDataPlaneLocator srcSffIngressDpl =
                        getSffDataPlaneLocator(srcSff, sffGraph.getSffIngressDpl(entry.getSrcSff(), entry.getPathId()));
                if(!setSffRemainingHopDataPlaneLocator(srcSff, rspTransport, srcSffIngressDpl, true, entry.getPathId(), sffGraph)) {
                    LOG.error("Unable to get SFF egress DPL srcSff [{}] transport [{}] pathId [{}]",
                            entry.getSrcSff(), entry.getDstSff(), rspTransport, entry.getPathId());
                    return false;
                }
            } else {
                // The srcSff egress DPL was just set above, now set its ingress DPL
                SffDataPlaneLocator srcSffEgressDpl =
                        getSffDataPlaneLocator(srcSff, sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
                if(!setSffRemainingHopDataPlaneLocator(srcSff, rspTransport, srcSffEgressDpl, false, entry.getPathId(), sffGraph)) {
                    LOG.error("Unable to get SFF HOP ingress DPL srcSff [{}] transport [{}] pathId [{}]",
                            entry.getSrcSff(), entry.getDstSff(), rspTransport, entry.getPathId());
                    return false;
                }
            }
        }

        return true;
    }

    /*
     * For the given sff that has either the ingress or egress DPL set, as indicated
     * by ingressDplSet, iterate the SFF DPLs looking for the one that has the same
     * transportType as the already set DPL. Once found set it as the SFF ingress/egress DPL.
     * For example, if ingressDplSet is true, then the SFF ingress DPL has already been
     * set, so set the SFF egress DPL that has the same transportType as the ingress DPL.
     */
    private boolean setSffRemainingHopDataPlaneLocator(final ServiceFunctionForwarder sff,
                                                       final String rspTransport,
                                                       SffDataPlaneLocator alreadySetSffDpl,
                                                       boolean ingressDplSet,
                                                       final long pathId,
                                                       SffGraph sffGraph) {
        List<SffDataPlaneLocator> sffDplList = sff.getSffDataPlaneLocator();
        for(SffDataPlaneLocator sffDpl : sffDplList) {
            if(sffDpl.getName().equals(alreadySetSffDpl.getName())) {
                continue;
            }
            if(sffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                if(ingressDplSet) {
                    // the SFF ingressDpl was already set, so we need to set the egress
                    sffGraph.setSffEgressDpl(sff.getName(), pathId, sffDpl.getName());
                } else {
                    sffGraph.setSffIngressDpl(sff.getName(), pathId, sffDpl.getName());
                }

                return true;
            }
        }

        return false;
    }

    // Returns true if it was possible to set the SFF DPLs
    private boolean setSffHopDataPlaneLocators(final ServiceFunctionForwarder prevSff,
                                               final ServiceFunctionForwarder curSff,
                                               final String rspTransport,
                                               final long pathId,
                                               SffGraph sffGraph) {
        List<SffDataPlaneLocator> prevSffDplList = prevSff.getSffDataPlaneLocator();
        List<SffDataPlaneLocator> curSffDplList = curSff.getSffDataPlaneLocator();

        // This is an O(n squared) search, can be improved using a hash table.
        // Considering there should only be 3-4 DPLs, its not worth the extra
        // code to improve it.
        for(SffDataPlaneLocator prevSffDpl : prevSffDplList) {
            if(!prevSffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                // Only check the transport types that the RSP uses
                continue;
            }

            for(SffDataPlaneLocator curSffDpl : curSffDplList) {
                if(!curSffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                    // Only check the transport types that the RSP uses
                    continue;
                }

                LocatorType prevLocatorType = prevSffDpl.getDataPlaneLocator().getLocatorType();
                LocatorType curLocatorType = curSffDpl.getDataPlaneLocator().getLocatorType();

                if(compareLocatorTypes(prevLocatorType, curLocatorType)) {
                    sffGraph.setSffEgressDpl(prevSff.getName(), pathId, prevSffDpl.getName());
                    sffGraph.setSffIngressDpl(curSff.getName(), pathId, curSffDpl.getName());
                    return true;
                }
            }
        }

        return false;
    }

    private boolean compareLocatorTypes(LocatorType lhs, LocatorType rhs) {
        if(lhs.getImplementedInterface() != rhs.getImplementedInterface()) {
            return false;
        }
        String type = lhs.getImplementedInterface().getSimpleName().toLowerCase();

        switch (type) {
        case IP:
            // TODO what makes 2 NSH IP DPLs equal? Assuming its the Port, as each IP will be different
            if(((Ip) lhs).getPort().getValue().intValue() == ((Ip) rhs).getPort().getValue().intValue()) {
                return true;
            }
            break;
        case MAC:
            // TODO for now only checking VLAN Id, if present
            if(((Mac) lhs).getVlanId() != null && ((Mac) rhs).getVlanId() != null) {
                if(((Mac) lhs).getVlanId().intValue() == ((Mac) rhs).getVlanId().intValue()) {
                    return true;
                }
            }
            break;
        case MPLS:
            if(((Mpls) lhs).getMplsLabel().longValue() == ((Mpls) rhs).getMplsLabel().longValue()) {
                return true;
            }
            break;

        case FUNCTION:
        case LISP:
        default:
            break;
        }

        return false;
    }

    //
    // Internally generate the transport details per RSP and per RSP hop
    //
    private void setRspTransports(SffGraph sffGraph, Class<? extends SlTransportType> rspTransport, long pathId) {
        int transportData = 0;
        int hopIncrement = 1;
        boolean isMpls = false;
        boolean isMac = false;

        if(rspTransport.getName().equals(
                org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls.class.getName())) {

            transportData = this.lastMplsLabel + MPLS_LABEL_INCR_RSP;
            this.lastMplsLabel = transportData;
            hopIncrement = MPLS_LABEL_INCR_HOP;
            isMpls = true;
        } else if(rspTransport.getName().equals(
                org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac.class.getName())) {

            transportData = this.lastVlanId + VLAN_ID_INCR_RSP;
            this.lastVlanId = transportData;
            hopIncrement = VLAN_ID_INCR_HOP;
            isMac = true;
        }

        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
        while (sffGraphIter.hasNext()) {
            DataPlaneLocatorBuilder dpl = new DataPlaneLocatorBuilder();
            dpl.setTransport(rspTransport);
            if(isMpls) {
                MplsBuilder mplsBuilder = new MplsBuilder();
                mplsBuilder.setMplsLabel((long) transportData);
                dpl.setLocatorType(mplsBuilder.build());
            } else if(isMac) {
                MacBuilder macBuilder = new MacBuilder();
                macBuilder.setVlanId(transportData);
                dpl.setLocatorType(macBuilder.build());
            }

            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            if(entry.getDstSff().equals(SffGraph.EGRESS)) {
                sffGraph.setPathEgressDpl(pathId, dpl.build());
            } else {
                sffGraph.setHopIngressDpl(entry.getDstSff(), pathId, dpl.build());
            }
            transportData += hopIncrement;
        }
    }


    /****************************************************************
     *
     *                 General SFF and related util methods
     *
     ****************************************************************/


    private OfsPort getSffPortInfoFromDpl(final SffDataPlaneLocator sffDpl) {
        if(sffDpl == null) {
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = sffDpl.getAugmentation(SffDataPlaneLocator1.class);
        if(ofsDpl == null) {
            LOG.error("No OFS DPL available for dpl [{}]", sffDpl.getName());
            return null;
        }

        return ofsDpl.getOfsPort();
    }

    private OfsPort getSffPortInfoFromSffSfDict(final ServiceFunctionDictionary sffSfDict) {
        if(sffSfDict == null) {
            return null;
        }
        ServiceFunctionDictionary1 ofsSffSfDict = sffSfDict.getAugmentation(ServiceFunctionDictionary1.class);
        if(ofsSffSfDict == null) {
            LOG.error("No OFS SffSf Dictionary available for dict [{}]", sffSfDict.getName());
            return null;
        }

        return ofsSffSfDict.getOfsPort();
    }

    private String getDplPortInfoPort(final SffDataPlaneLocator dpl) {
        OfsPort ofsPort = getSffPortInfoFromDpl(dpl);

        if(ofsPort == null) {
            return null;
        }

        return ofsPort.getPortId();
    }

    private String getDplPortInfoMac(final SffDataPlaneLocator dpl) {
        OfsPort ofsPort = getSffPortInfoFromDpl(dpl);

        if(ofsPort == null) {
            return null;
        }

        if(ofsPort.getMacAddress() == null) {
            return null;
        }

        return ofsPort.getMacAddress().getValue();
    }

    private String getDictPortInfoPort(final ServiceFunctionDictionary dict) {
        OfsPort ofsPort = getSffPortInfoFromSffSfDict(dict);

        if(ofsPort == null) {
            return null;
        }

        return ofsPort.getPortId();
    }

    private String getDictPortInfoMac(final ServiceFunctionDictionary dict) {
        OfsPort ofsPort = getSffPortInfoFromSffSfDict(dict);

        if(ofsPort == null) {
            return null;
        }

        if(ofsPort.getMacAddress() == null) {
            return null;
        }

        return ofsPort.getMacAddress().getValue();
    }

    String getSfDplMac(SfDataPlaneLocator sfDpl) {
        String sfMac = null;

        LocatorType sffLocatorType = sfDpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            if(((MacAddressLocator) sffLocatorType).getMac() == null) {
                sfMac = ((MacAddressLocator) sffLocatorType).getMac().getValue();
            }
        }

        return sfMac;
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
    private ServiceFunctionDictionary getSffSfDictionary(ServiceFunctionForwarder sff, String sfName) {
        ServiceFunctionDictionary sffSfDict = null;

        List<ServiceFunctionDictionary> sffSfDictList = sff.getServiceFunctionDictionary();
        for (ServiceFunctionDictionary dict : sffSfDictList) {
            if(dict.getName().equals(sfName)) {
                sffSfDict = dict;
                break;
            }
        }

        return sffSfDict;
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


    private String getSffServiceNodeName(final String sffName) {
        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
        if(sff == null) {
            return null;
        }

        return sff.getServiceNode();
    }
}
