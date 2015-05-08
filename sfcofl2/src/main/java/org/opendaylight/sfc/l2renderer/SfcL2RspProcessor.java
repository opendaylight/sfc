/**
 * Copyright (c) 2014 by Ericsson and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */


package org.opendaylight.sfc.l2renderer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionGroupAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MplsLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MplsBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.port.details.OfsPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcL2RspProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SfcL2RspProcessor.class);
    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;
    // each time onDataChanged() is called, store the SFs and SFFs internally
    // so we dont have to query the DataStore repeatedly for the same thing
    private Map<String, ServiceFunction> serviceFunctions;
    private Map<String, ServiceFunctionGroup> serviceFunctionGroups;
    private Map<String, ServiceFunctionForwarder> serviceFunctionFowarders;
    private Map<String, Boolean> sffInitialized;
    private boolean addFlow;
    private int lastMplsLabel;
    private int lastVlanId;

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
        this.serviceFunctionGroups = new HashMap<String, ServiceFunctionGroup>();
        this.serviceFunctionFowarders = new HashMap<String, ServiceFunctionForwarder>();
        this.sffInitialized = new HashMap<String, Boolean>();
        this.lastMplsLabel = 0;
        this.lastVlanId = 0;
    }

    // if this method takes too long, consider launching it in a thread
    public void processRenderedServicePath(RenderedServicePath rsp, boolean addFlow) {
        // Reset the internal SF and SFF storage
        this.serviceFunctions.clear();
        this.serviceFunctionGroups.clear();
        this.serviceFunctionFowarders.clear();
        this.addFlow = addFlow;

        // Setting to INGRESS for the first graph entry, which is the RSP Ingress
        String prevSffName = SffGraph.INGRESS;
        SffGraph sffGraph = new SffGraph();

        // TODO we never consider the case where traffic just flows
        //      through the SFF and doesnt go to an SF on the SFF

        try {
            //
            // Populate the SFF Connection Graph
            //
            Iterator<RenderedServicePathHop> servicePathHopIter = rsp.getRenderedServicePathHop().iterator();
            String sfName = null;
            String sfgName = null;
            while (servicePathHopIter.hasNext()) {
                RenderedServicePathHop rspHop = servicePathHopIter.next();
                String curSffName = rspHop.getServiceFunctionForwarder();
                sfName = rspHop.getServiceFunctionName();
                sfgName = rspHop.getServiceFunctionGroupName();

                LOG.info("processRenderedServicePath pathId [{}] renderedServicePathHop [{}]",
                        rsp.getPathId(), rspHop.getHopNumber());

                sffGraph.addGraphEntry(prevSffName, curSffName, sfName, sfgName, rsp.getPathId(), rspHop.getServiceIndex());
                prevSffName = curSffName;
            }
            // Add the final connection, which will be the RSP Egress
            // Using the previous sfName as the SrcSf
            sffGraph.addGraphEntry(prevSffName, SffGraph.EGRESS, sfName, sfgName, rsp.getPathId(), (short)0);

            //
            // Populate the SFF ingress and egress DPLs from the sffGraph
            //
            processSffDpls(sffGraph, rsp.getTransportType().getName());

            //
            // Internally calculate and set the RSP transport values
            //
            setRspTransports(sffGraph, rsp.getTransportType(), rsp.getPathId());

            //
            // Now process the entries in the SFF Graph and populate the flow tables
            //
            Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
            while (sffGraphIter.hasNext()) {
                SffGraph.SffGraphEntry entry = sffGraphIter.next();
                LOG.debug("build flows of entry: {}", entry);

                if(!entry.getDstSff().equals(SffGraph.EGRESS)) {
                    initializeSff(entry.getDstSff());
                }
                configureSffIngress(entry, sffGraph);
                if(entry.getSf() != null) {
                    configureSffEgress(entry, sffGraph);
                } else if(entry.getSfg() != null){
                    configureSffEgressForGroup(entry, sffGraph);
                }
            }
        } catch(RuntimeException e) {
            // TODO once this class is more formalized, dont dump the stack trace
            LOG.error("RuntimeException in processRenderedServicePath: ", e.getMessage(), e);
        }
    }

    private void configureSffEgressForGroup(SffGraph.SffGraphEntry entry, SffGraph sffGraph) {
        LOG.debug("configureSffEgressForGroup srcSff [{}] dstSff [{}] sfg [{}] pathId [{}] serviceIndex [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSfg(), entry.getPathId(), entry.getServiceIndex());


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
        LOG.debug("configureSffIngress srcSff [{}] dstSff [{}] sf [{}] sfg [{}] pathId [{}] serviceIndex [{}]",
                entry.getSrcSff(), entry.getDstSff(), entry.getSf(), entry.getSfg(), entry.getPathId(), entry.getServiceIndex());

        final String sffDstName = entry.getDstSff();

        if(sffDstName.equals(SffGraph.EGRESS)) {
            // Nothing to be done for the ingress tables, skip it
            return;
        }

        ServiceFunctionForwarder sffDst = getServiceFunctionForwarder(sffDstName);
        SffDataPlaneLocator sffDstIngressDpl =
                getSffDataPlaneLocator(sffDst, sffGraph.getSffIngressDpl(sffDstName, entry.getPathId()));
        if(sffDstIngressDpl == null) {
            throw new RuntimeException(
                    "configureSffIngress SFF [" + sffDstName + "] does not have a DataPlaneLocator for pathId [" + entry.getPathId() + "]");
        }

        // This is the HOP DPL details between srcSFF and dstSFF, for example: VLAN ID 100
        DataPlaneLocator dstHopIngressDpl = sffGraph.getHopIngressDpl(sffDstName, entry.getPathId());
        if(dstHopIngressDpl == null) {
            throw new RuntimeException(
                    "configureSffIngress SFF [" + sffDstName + "] Hop Ingress DPL is null for pathId [" +
                    entry.getPathId() + "]");
        }

        // Configure the SF related flows
        if(entry.getSf() != null){
            SffSfDataPlaneLocator sfDpl = getSffSfDataPlaneLocator(sffDst, entry.getSf());
            if(sfDpl == null) {
                throw new RuntimeException(
                        "Cant find SFF [" + sffDstName + "] to SF [" + entry.getSf() + "] DataPlaneLocator");
            }
            configureSingleSfIngressFlow(entry, sffDstName, dstHopIngressDpl, sfDpl);
        } else if(entry.getSfg() != null){
            LOG.debug("configure ingress flow for SFG {}", entry.getSfg());
            ServiceFunctionGroup sfg = getServiceFunctionGroup(entry.getSfg());
            List<SfcServiceFunction> sfgSfs = sfg.getSfcServiceFunction();
            for (SfcServiceFunction sfgSf : sfgSfs) {
                LOG.debug("configure ingress flow for SF {}", sfgSf);
                SffSfDataPlaneLocator sfDpl = getSffSfDataPlaneLocator(sffDst, sfgSf.getName());
                if(sfDpl == null) {
                    throw new RuntimeException(
                            "Cant find SFF [" + sffDstName + "] to SF [" + sfgSf + "] DataPlaneLocator");
                }
                configureSingleSfIngressFlow(entry, sffDstName, dstHopIngressDpl, sfDpl);

            }
        }

        // Configure the Service Chain Ingress flow(s)
        if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // configure the SFF Transport Ingress Flow using the dstHopIngressDpl
            configureSffTransportIngressFlow(sffDstName, dstHopIngressDpl);
            return;
        }

        // configure SFF-SFF-SF ingress flow using dstHopIngressDpl
        configureSffIngressFlow(sffDstName, false, dstHopIngressDpl, entry.getPathId(), entry.getServiceIndex());
    }

    private void configureSingleSfIngressFlow(SffGraph.SffGraphEntry entry, final String sffDstName,
            DataPlaneLocator dstHopIngressDpl, SffSfDataPlaneLocator sfDpl) {
        // configure the Ingress-SFF-SF ingress Flow
        configureSffIngressFlow(sffDstName, false, dstHopIngressDpl, entry.getPathId(), entry.getServiceIndex());
        // configure the SF Transport Ingress Flow
        configureSffTransportIngressFlow(sffDstName, sfDpl);
        // configure the SF Ingress Flow, setting negative pathId so it wont
        // set metadata and will goto classification table instead of NextHop
        configureSffIngressFlow(sffDstName, true, sfDpl, entry.getPathId(), entry.getServiceIndex());
    }

    /**
     * Populate the NextHop and TransportEgress Flow Tables
     * @param entry
     */
    private void configureSffEgress(SffGraph.SffGraphEntry entry, SffGraph sffGraph) {
        LOG.debug("configureSffEgress srcSff [{}] dstSff [{}] sf [{}] pathId [{}] serviceIndex [{}]",
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
        ServiceFunction sfDst = getServiceFunction(entry.getSf());
        SfDataPlaneLocator sfDstDpl = getSfDataPlaneLocator(sfDst);

        if(entry.getDstSff().equals(SffGraph.EGRESS)) {
            sffSrcEgressDpl =
                    getSffDataPlaneLocator(sffSrc, sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));

            // Configure the SF-SFF-GW NextHop, we dont have the GW mac, leaving it blank
            configureSffNextHopFlow(entry.getSrcSff(),
                    sfDstDpl,
                    (SffDataPlaneLocator) null,
                    entry.getPathId(),
                    entry.getServiceIndex());

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
        if(sfDstDpl != null) {
            if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
                // Configure the GW-SFF-SF NextHop using sfDpl
                configureSffNextHopFlow(entry.getDstSff(), (SffDataPlaneLocator) null, sfDstDpl, entry.getPathId(), entry.getServiceIndex());
            } else {
                sffSrcEgressDpl =
                        getSffDataPlaneLocator(sffSrc, sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
                // Configure the SFF-SFF-SF NextHop using sfDpl
                configureSffNextHopFlow(entry.getDstSff(), sffSrcEgressDpl, sfDstDpl, entry.getPathId(), entry.getServiceIndex());
            }

            ServiceFunctionDictionary sffSfDict = SfcL2Utils.getSffSfDictionary(sffDst, entry.getSf());
            // Configure the SFF-SF Transport Egress using sfDpl
            configureSffTransportEgressFlow(
                    entry.getDstSff(), sffSfDict, sfDstDpl, sfDstDpl, entry.getPathId(), entry.getServiceIndex(), true);
        }

        if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
            // Nothing else to be done for the egress tables
            return;
        }

        // TODO what if there is more than one SF in the dict?
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
        String sffNodeName = getSffOpenFlowNodeName(sffName);
        if(sffNodeName == null) {
            throw new RuntimeException(
                    "initializeSff SFF [" + sffName + "] does not exist");
        }

        if(!getSffInitialized(sffName)) {
            LOG.debug("Initializing SFF [{}] node [{}]", sffName, sffNodeName);
            this.sfcL2FlowProgrammer.configureTransportIngressTableMatchAny( sffNodeName, true, true);
            this.sfcL2FlowProgrammer.configureIngressTableMatchAny(          sffNodeName, false, true);
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
        String sffNodeName = getSffOpenFlowNodeName(sffName);
        if(sffNodeName == null) {
            throw new RuntimeException(
                    "configureSffTransportIngressFlow SFF [" + sffName + "] does not exist");
        }

        LOG.debug("configureSffTransportIngressFlow sff [{}] node [{}]", sffName, sffNodeName);

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
                this.sfcL2FlowProgrammer.configureVxlanGpeTransportIngressFlow(sffNodeName, this.addFlow);
           }
        }
    }

    private void configureSffIngressFlow(
            final String sffName, boolean isSf, DataPlaneLocator hopDpl, final long pathId, final short serviceIndex) {
        String sffNodeName = getSffOpenFlowNodeName(sffName);
        if(sffNodeName == null) {
            throw new RuntimeException(
                    "configureSffIngressFlow SFF [" + sffName + "] does not exist");
        }

        LOG.debug("configureSffIngressFlow sff [{}] node [{}] pathId [{}] serviceIndex [{}]",
                sffName, sffNodeName, pathId, serviceIndex);

        LocatorType sffLocatorType = hopDpl.getLocatorType();
        if(sffLocatorType == null) {
            throw new RuntimeException(
                    "configureSffIngressFlow hopDpl locatorType is null for sff: " + sffName);
        }
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            Integer vlanTag = ((MacAddressLocator) sffLocatorType).getVlanId();
            MacAddress mac = ((MacAddressLocator) sffLocatorType).getMac();
            if(vlanTag == null) {
                this.sfcL2FlowProgrammer.configureMacIngressFlow(sffNodeName, mac.getValue(), pathId, isSf, this.addFlow);
            } else {
                this.sfcL2FlowProgrammer.configureVlanIngressFlow(sffNodeName, vlanTag, pathId, isSf, this.addFlow);
            }
        } else if (implementedInterface.equals(Mpls.class)) {
            // MPLS
            long mplsLabel = ((MplsLocator) sffLocatorType).getMplsLabel();
            this.sfcL2FlowProgrammer.configureMplsIngressFlow(sffNodeName, mplsLabel, pathId, isSf, this.addFlow);
        }
        // Nothing to be done for IP/UDP VxLAN-gpe, as the RSP ID is the NSH.nsp
        // else if (implementedInterface.equals(Ip.class)) {
        //   if (hopDpl.getTransport().equals(VxlanGpe.class))

    }

    // Simple pass-through method
    // This is the case of either SFF-SFF-SF or GW-SFF-SF, where the source
    // info is in the SFF Egress DPL and the dstDpl is the SF DPL
    private void configureSffNextHopFlow(final String sffName,
                                         SffDataPlaneLocator srcSffDpl,
                                         SfDataPlaneLocator dstSfDpl,
                                         final long pathId,
                                         final short serviceIndex) {
        DataPlaneLocator srcDpl = null;
        String srcMac = null;
        if(srcSffDpl != null) {
            srcDpl = srcSffDpl.getDataPlaneLocator();
            srcMac = getDplPortInfoMac(srcSffDpl);
        }
        String dstMac = getSfDplMac(dstSfDpl);
        configureSffNextHopFlow(sffName, srcDpl, dstSfDpl, srcMac, dstMac, pathId, serviceIndex);
    }

    // Simple pass-through method
    // This is the case of SF-SFF-SFF flows
    private void configureSffNextHopFlow(final String sffName,
                                         SfDataPlaneLocator srcSfDpl,
                                         SffDataPlaneLocator dstSffDpl,
                                         final long pathId,
                                         final short serviceIndex) {
        String srcMac = getSfDplMac(srcSfDpl);
        String dstMac = getDplPortInfoMac(dstSffDpl);
        DataPlaneLocator dstDpl = ((dstSffDpl == null) ? null : dstSffDpl.getDataPlaneLocator());
        configureSffNextHopFlow(sffName, srcSfDpl, dstDpl, srcMac, dstMac, pathId, serviceIndex);
    }

    // This version is only used by the previous 2 configureSffNextHopFlow() signatures
    private void configureSffNextHopFlow(final String sffName,
                                         DataPlaneLocator srcDpl,
                                         DataPlaneLocator dstDpl,
                                         String srcMac,
                                         String dstMac,
                                         final long pathId,
                                         final short serviceIndex) {

        String sffNodeName = getSffOpenFlowNodeName(sffName);
        if(sffNodeName == null) {
            throw new RuntimeException(
                    "configureSffNextHopFlow SFF [" + sffName + "] does not exist");
        }

        LOG.debug("configureSffNextHopFlow sff [{}] pathId [{}] serviceIndex [{}] srcMac [{}] dstMac [{}]",
                sffName, pathId, serviceIndex, srcMac, dstMac);

        LocatorType srcSffLocatorType = (srcDpl != null) ? srcDpl.getLocatorType() : null;
        LocatorType dstSffLocatorType = (dstDpl != null) ? dstDpl.getLocatorType() : null;
            // Assuming srcDpl and dstDpl are of the same type
        Class<? extends DataContainer> implementedInterface =
                (srcDpl != null) ?
                        srcSffLocatorType.getImplementedInterface() :
                        dstSffLocatorType.getImplementedInterface();

        if (implementedInterface.equals(Ip.class)) {
            //VxLAN-gpe, it is IP/UDP flow with VLAN tag
            if(dstDpl != null) {
                if(dstDpl.getTransport().equals(VxlanGpe.class)) {
                    String dstIp = String.valueOf(((IpPortLocator) dstSffLocatorType).getIp().getValue());
                    long nsp = pathId;
                    short nsi = serviceIndex;
                    this.sfcL2FlowProgrammer.configureVxlanGpeNextHopFlow(sffNodeName, dstIp, nsp, nsi, this.addFlow);
                }
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
        String srcOfsPortStr = SfcL2Utils.getDictPortInfoPort(srcSffSfDict);
        if(srcOfsPortStr == null) {
            throw new RuntimeException(
                    "configureSffTransportEgressFlow OFS port not avail for SFF [" + sffName +
                    "] sffSfDict [" + srcSffSfDict.getName() + "]");
        }
        String srcMac = getDictPortInfoMac(srcSffSfDict);
        String dstMac = getSfDplMac(dstSfDpl);
        configureSffTransportEgressFlow(
                sffName, srcDpl, dstSfDpl, hopDpl, srcOfsPortStr, srcMac, dstMac, pathId, serviceIndex, isSf);
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
        if(srcOfsPortStr == null) {
            throw new RuntimeException(
                    "configureSffTransportEgressFlow OFS port not avail for SFF [" + sffName +
                    "] srcDpl [" + srcDpl.getName() + "]");
        }
        String srcMac = getDplPortInfoMac(srcDpl);
        String dstMac = getDplPortInfoMac(dstDpl);
        configureSffTransportEgressFlow(sffName,
                srcDpl.getDataPlaneLocator(),
                ((dstDpl == null) ? null : dstDpl.getDataPlaneLocator()),
                hopDpl, srcOfsPortStr, srcMac, dstMac, pathId, serviceIndex, isSf);
    }

    // This version is only used by the previous 2 configureSffTransportEgressFlow() signatures
    private void configureSffTransportEgressFlow(final String sffName,
                                                 DataPlaneLocator srcDpl,
                                                 DataPlaneLocator dstDpl,
                                                 DataPlaneLocator hopDpl,
                                                 String srcOfsPort,
                                                 String srcMac,
                                                 String dstMac,
                                                 long pathId,
                                                 short serviceIndex,
                                                 boolean isSf) {
        if(hopDpl == null) {
            throw new RuntimeException(
                    "configureSffTransportEgressFlow SFF [" + sffName + "] hopDpl is null");
        }

        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
        if(sff == null) {
            throw new RuntimeException(
                    "configureSffTransportEgressFlow SFF [" + sffName + "] does not exist");
        }

        String sffNodeName = getSffOpenFlowNodeName(sffName);
        if(sffNodeName == null) {
            throw new RuntimeException(
                    "configureSffTransportEgressFlow Sff Node name for SFF [" + sffName + "] does not exist");
        }

        LOG.debug("configureSffTransportEgressFlow sff [{}] node [{}]", sffName, sffNodeName);

        LocatorType hopLocatorType = hopDpl.getLocatorType();
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
           if (hopDpl.getTransport().equals(VxlanGpe.class)) {
                long nsp = pathId;
                short nsi = serviceIndex;
                this.sfcL2FlowProgrammer.configureVxlanGpeTransportEgressFlow(
                        sffNodeName, nsp, nsi, srcOfsPort, this.addFlow);
           }
        }
    }


    /****************************************************************
     *
     *                 Process SFF DPL methods
     *
     ****************************************************************/

    private void processSffDpls(SffGraph sffGraph, final String rspTransport) {

        // Iterate the entries in the SFF Graph
        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            if(entry.getSrcSff().equals(SffGraph.INGRESS)) {
                continue;
            }
            LOG.debug("processSffDpl - handling entry {}", entry);
            ServiceFunctionForwarder srcSff = getServiceFunctionForwarder(entry.getSrcSff());
            if(srcSff == null) {
                throw new RuntimeException(
                        "processSffDpls srcSff is null [" + entry.getSrcSff() + "]");
            }

            ServiceFunctionForwarder dstSff = getServiceFunctionForwarder(entry.getDstSff()); // may be null if its EGRESS
            if(dstSff != null) {
                // Set the SFF-SFF Hop DPL
                if(!setSffHopDataPlaneLocators(srcSff, dstSff, rspTransport, entry.getPathId(), sffGraph)) {
                    throw new RuntimeException(
                            "Unable to get SFF HOP DPLs srcSff [" + entry.getSrcSff() +
                            "] dstSff [" + entry.getDstSff() + "] transport [" + rspTransport +
                            "] pathId [" + entry.getPathId() + "]");
                }
            }

            if(entry.getDstSff().equals(SffGraph.EGRESS)) {
                // The srcSff ingress DPL was set in the previous loop
                // iteration, now we need to set its egress DPL
                SffDataPlaneLocator srcSffIngressDpl =
                        getSffDataPlaneLocator(srcSff, sffGraph.getSffIngressDpl(entry.getSrcSff(), entry.getPathId()));
                if(!setSffRemainingHopDataPlaneLocator(srcSff, rspTransport, srcSffIngressDpl, true, entry.getPathId(), sffGraph)) {
                    throw new RuntimeException(
                            "Unable to get SFF egress DPL srcSff [" + entry.getSrcSff() +
                            "] transport [" + rspTransport +
                            "] pathId [" + entry.getPathId() + "]");
                }
            } else {
                // The srcSff egress DPL was just set above, now set its ingress DPL
                SffDataPlaneLocator srcSffEgressDpl =
                        getSffDataPlaneLocator(srcSff, sffGraph.getSffEgressDpl(entry.getSrcSff(), entry.getPathId()));
                if(!setSffRemainingHopDataPlaneLocator(srcSff, rspTransport, srcSffEgressDpl, false, entry.getPathId(), sffGraph)) {
                    throw new RuntimeException(
                            "Unable to get SFF HOP ingress DPL srcSff [" + entry.getSrcSff() +
                            "] transport ["+ rspTransport +
                            "] pathId [" + entry.getPathId() + "]");
                }
            }
        }
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
        if(sffDplList.size() == 1) {
            // Nothing to be done here
            return true;
        }

        for(SffDataPlaneLocator sffDpl : sffDplList) {
            LOG.debug("try to match sffDpl name: {}, type: {}", sffDpl.getName(), sffDpl.getDataPlaneLocator().getTransport().getName());
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
        boolean hasSingleDpl = false;

        // If the prevSffDplList has just one DPL, nothing special needs to be done
        // Just check that its DPL transport matches the RSP transport
        if(prevSffDplList.size() == 1) {
            SffDataPlaneLocator prevSffDpl = prevSffDplList.get(0);
            if(!prevSffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                LOG.warn("SFF [{}] transport type [{}] does not match the RSP DPL transport type [{}]",
                        prevSff,
                        prevSffDpl.getDataPlaneLocator().getTransport().getName(),
                        rspTransport);
                return false;
            }
            sffGraph.setSffEgressDpl(prevSff.getName(), pathId, prevSffDpl.getName());
            sffGraph.setSffIngressDpl(prevSff.getName(), pathId, prevSffDpl.getName());

            // Nothing else needs to be done
            hasSingleDpl = true;
        }

        if(curSffDplList.size() == 1) {
            SffDataPlaneLocator curSffDpl = curSffDplList.get(0);
            if(!curSffDpl.getDataPlaneLocator().getTransport().getName().equals(rspTransport)) {
                LOG.warn("SFF [{}] transport type [{}] does not match the RSP DPL transport type [{}]",
                        curSff,
                        curSffDpl.getDataPlaneLocator().getTransport().getName(),
                        rspTransport);
                return false;
            }
            sffGraph.setSffEgressDpl(curSff.getName(), pathId, curSffDpl.getName());
            sffGraph.setSffIngressDpl(curSff.getName(), pathId, curSffDpl.getName());

            // Nothing else needs to be done
            hasSingleDpl = true;
        }

        if(hasSingleDpl) {
            return true;
        }

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
                LOG.debug("comparing prev locator {} : {} to {} : {}", prevSffDpl.getName(), prevLocatorType, curSffDpl.getName(), curLocatorType);
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
        boolean isVxlanGpe = false;

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
        } else if(rspTransport.getName().equals(
                org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe.class.getName())) {

            isVxlanGpe = true;
        }

        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            LOG.debug("RspTransport entry: {}", entry);
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
            } else if(isVxlanGpe) {
                ServiceFunctionForwarder sff;
                if(entry.getDstSff().equals(SffGraph.EGRESS)) {
                    sff = getServiceFunctionForwarder(entry.getSrcSff());
                } else {
                    sff = getServiceFunctionForwarder(entry.getDstSff());;
                }
                String sffEgressDplName = sffGraph.getSffEgressDpl(sff.getName(), pathId);
                LocatorType loc = getSffDataPlaneLocator(sff, sffEgressDplName).getDataPlaneLocator().getLocatorType();
                IpBuilder ipBuilder = new IpBuilder();
                ipBuilder.setIp(((Ip) loc).getIp());
                ipBuilder.setPort(((Ip) loc).getPort());
                dpl.setLocatorType(ipBuilder.build());
            }

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
            LOG.debug("No OFS DPL available for dpl [{}]", sffDpl.getName());
            return null;
        }

        return ofsDpl.getOfsPort();
    }

    private String getDplPortInfoPort(final SffDataPlaneLocator dpl) {
        OfsPort ofsPort = getSffPortInfoFromDpl(dpl);

        if(ofsPort == null) {
            // This case is most likely because the sff-of augmentation wasnt used
            // assuming the packet should just be sent on the same port it was received on
            return OutputPortValues.INPORT.toString();
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


    private String getDictPortInfoMac(final ServiceFunctionDictionary dict) {
        OfsPort ofsPort = SfcL2Utils.getSffPortInfoFromSffSfDict(dict);

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
            if(((MacAddressLocator) sffLocatorType).getMac() != null) {
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


    private String getSffOpenFlowNodeName(final String sffName) {
        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
        return SfcL2Utils.getSffOpenFlowNodeName(sff);
    }

    private ServiceFunctionGroup getServiceFunctionGroup(final String sfgName) {
        ServiceFunctionGroup sfg = this.serviceFunctionGroups.get(sfgName);
        if (sfg == null) {
            sfg = SfcProviderServiceFunctionGroupAPI.readServiceFunctionGroupExecutor(sfgName);
            if (sfg != null) {
                this.serviceFunctionGroups.put(sfgName, sfg);
            }
        }

        return sfg;
    }

}
