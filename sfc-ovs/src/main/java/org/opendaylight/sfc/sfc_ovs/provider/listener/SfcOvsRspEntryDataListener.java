/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the SFC RSP operational datastore
 * <p/>
 * <p/>
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-04-08
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsDataStoreAPI;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsToSffMappingAPI;
import org.opendaylight.sfc.sfc_ovs.provider.util.HopOvsdbBridgePair;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator2;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator2Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOvsRspEntryDataListener extends SfcOvsAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsRspEntryDataListener.class);

    public static final InstanceIdentifier<RenderedServicePath> RSP_ENTRY_IID =
            InstanceIdentifier.builder(RenderedServicePaths.class)
                    .child(RenderedServicePath.class).build();

    public SfcOvsRspEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(RSP_ENTRY_IID);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        // RSP CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {

            if (entry.getValue() instanceof RenderedServicePath) {
                RenderedServicePath renderedServicePath = (RenderedServicePath) entry.getValue();
                LOG.debug("\nCreated Rendered Service Path: {}", renderedServicePath.toString());

                List<HopOvsdbBridgePair> hopOvsdbBridgePairList = buildHopOvsdbBridgePairList(renderedServicePath);

                for (int index = 0; index < hopOvsdbBridgePairList.size(); index++) {
                    List<SffDataPlaneLocator> sffDataPlaneLocatorList = new ArrayList<>();

                    //build forward VXLAN tunnel
                    if (index + 1 < hopOvsdbBridgePairList.size()) {
                        sffDataPlaneLocatorList.add(
                                buildVxlanTunnelDataPlaneLocator(
                                        renderedServicePath,
                                        hopOvsdbBridgePairList.get(index), hopOvsdbBridgePairList.get(index + 1)));
                    }

                    //build backward VXLAN tunnel
                    if (index - 1 >= 0) {
                        sffDataPlaneLocatorList.add(
                                buildVxlanTunnelDataPlaneLocator(
                                        renderedServicePath,
                                        hopOvsdbBridgePairList.get(index), hopOvsdbBridgePairList.get(index - 1)));
                    }

                    //put TerminationPoints into OVS Datastore
                    SfcOvsUtil.putOvsdbTerminationPoints(hopOvsdbBridgePairList.get(index).ovsdbBridgeAugmentation,
                            sffDataPlaneLocatorList, opendaylightSfc.getExecutor());
                }
            }
        }

//        // SFF UPDATE
//        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
//        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
//            if ((entry.getValue() instanceof ServiceFunctionForwarder)
//                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
//                ServiceFunctionForwarder updatedServiceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
//                LOG.debug("\nModified Service Function Forwarder : {}", updatedServiceFunctionForwarder.toString());
//
//                //build OvsdbBridge
//                OvsdbBridgeAugmentation ovsdbBridge =
//                        SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(updatedServiceFunctionForwarder);
//
//                //put Bridge
//                putOvsdbBridge(ovsdbBridge);
//
//                //put Termination Points
//                putOvsdbTerminationPoints(ovsdbBridge, updatedServiceFunctionForwarder);
//            }
//        }
//
//
//        // SFF DELETION
//        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
//        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
//            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
//            if (dataObject instanceof ServiceFunctionForwarder) {
//                ServiceFunctionForwarder deletedServiceFunctionForwarder = (ServiceFunctionForwarder) dataObject;
//                LOG.debug("\nDeleted Service Function Forwarder: {}", deletedServiceFunctionForwarder.toString());
//
//                KeyedInstanceIdentifier keyedInstanceIdentifier =
//                        (KeyedInstanceIdentifier) instanceIdentifier.firstIdentifierOf(ServiceFunctionForwarder.class);
//                if (keyedInstanceIdentifier != null) {
//                    ServiceFunctionForwarderKey sffKey = (ServiceFunctionForwarderKey) keyedInstanceIdentifier.getKey();
//                    String sffName = sffKey.getName();
//
//                    //delete OvsdbNode
//                    deleteOvsdbNode(SfcOvsUtil.buildOvsdbNodeIID(sffName));
//                }
//
//            } else if (dataObject instanceof SffDataPlaneLocator) {
//                SffDataPlaneLocator sffDataPlaneLocator = (SffDataPlaneLocator) dataObject;
//                LOG.debug("Deleted SffDataPlaneLocator: {}", sffDataPlaneLocator.getName());
//
//                KeyedInstanceIdentifier keyedInstanceIdentifier =
//                        (KeyedInstanceIdentifier) instanceIdentifier.firstIdentifierOf(ServiceFunctionForwarder.class);
//                if (keyedInstanceIdentifier != null) {
//                    ServiceFunctionForwarderKey sffKey = (ServiceFunctionForwarderKey) keyedInstanceIdentifier.getKey();
//                    String sffName = sffKey.getName();
//
//                    //delete OvsdbTerminationPoint
//                    deleteOvsdbTerminationPoint(SfcOvsUtil.buildOvsdbTerminationPointIID(sffName, sffDataPlaneLocator.getName()));
//                }
//            }
//        }
        printTraceStop(LOG);
    }

    private List<HopOvsdbBridgePair> buildHopOvsdbBridgePairList(RenderedServicePath renderedServicePath) {
        Preconditions.checkNotNull(renderedServicePath);

        List<HopOvsdbBridgePair> hopOvsdbBridgePairList = new ArrayList<>();

        for (RenderedServicePathHop hop : renderedServicePath.getRenderedServicePathHop()) {
            Object[] methodParams = {hop.getServiceFunctionForwarder()};
            SfcOvsDataStoreAPI readOvsdbBridge =
                    new SfcOvsDataStoreAPI(
                            SfcOvsDataStoreAPI.Method.READ_OVSDB_BRIDGE,
                            methodParams
                    );

            OvsdbBridgeAugmentation ovsdbBridge =
                    (OvsdbBridgeAugmentation) SfcOvsUtil.submitCallable(readOvsdbBridge, opendaylightSfc.getExecutor());

            hopOvsdbBridgePairList.add(hop.getHopNumber(), new HopOvsdbBridgePair(hop, ovsdbBridge));
        }

        return hopOvsdbBridgePairList;
    }

    private SffDataPlaneLocator buildVxlanTunnelDataPlaneLocator(RenderedServicePath renderedServicePath,
                                                                 HopOvsdbBridgePair hopOvsdbBridgePairFrom,
                                                                 HopOvsdbBridgePair hopOvsdbBridgePairTo) {
        Preconditions.checkNotNull(renderedServicePath);
        Preconditions.checkNotNull(hopOvsdbBridgePairFrom);
        Preconditions.checkNotNull(hopOvsdbBridgePairTo);

        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        //the name will be e.g. RSP1-vxlan-0to1
        sffDataPlaneLocatorBuilder
                .setName(renderedServicePath.getName() + "-vxlan-"
                        + hopOvsdbBridgePairFrom.renderedServicePathHop.getHopNumber() + "to"
                        + hopOvsdbBridgePairTo.renderedServicePathHop.getHopNumber());

        //build IP:Port locator
        IpAddress ipAddress = SfcOvsToSffMappingAPI.getOvsBridgeLocalIp(hopOvsdbBridgePairFrom.ovsdbBridgeAugmentation);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress);
        ipBuilder.setPort(new PortNumber(6633));

        //build Vxlan DataPlane locator
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        dataPlaneLocatorBuilder.setTransport(VxlanGpe.class);
        dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        sffDataPlaneLocatorBuilder.setDataPlaneLocator(dataPlaneLocatorBuilder.build());

        //build OVS Options for Vxlan tunnel
        OvsOptionsBuilder ovsOptionsBuilder = new OvsOptionsBuilder();
        ovsOptionsBuilder.setLocalIp(ipAddress);
        ovsOptionsBuilder.setRemoteIp(SfcOvsToSffMappingAPI.getOvsBridgeLocalIp(hopOvsdbBridgePairTo.ovsdbBridgeAugmentation));
        ovsOptionsBuilder.setDstPort(new PortNumber(6633));
        ovsOptionsBuilder.setNsp(renderedServicePath.getPathId().toString());
        ovsOptionsBuilder.setNsi(hopOvsdbBridgePairFrom.renderedServicePathHop.getServiceIndex().toString());
        ovsOptionsBuilder.setKey(String.valueOf(renderedServicePath.getName().hashCode()));

        //add OVS Options augmentation to SffDataPlaneLocator
        SffDataPlaneLocator2Builder sffDataPlaneLocator2Builder = new SffDataPlaneLocator2Builder();
        sffDataPlaneLocator2Builder.setOvsOptions(ovsOptionsBuilder.build());
        sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator2.class, sffDataPlaneLocator2Builder.build());

        return sffDataPlaneLocatorBuilder.build();
    }
}
