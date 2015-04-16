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
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcSffToOvsMappingAPI;
import org.opendaylight.sfc.sfc_ovs.provider.util.HopOvsdbBridgePair;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
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
                                SfcSffToOvsMappingAPI.buildVxlanTunnelDataPlaneLocator(
                                        renderedServicePath,
                                        hopOvsdbBridgePairList.get(index), hopOvsdbBridgePairList.get(index + 1)));
                    }

                    //build backward VXLAN tunnel
                    if (index - 1 >= 0) {
                        sffDataPlaneLocatorList.add(
                                SfcSffToOvsMappingAPI.buildVxlanTunnelDataPlaneLocator(
                                        renderedServicePath,
                                        hopOvsdbBridgePairList.get(index), hopOvsdbBridgePairList.get(index - 1)));
                    }

                    //put TerminationPoints into OVS Datastore
                    SfcOvsUtil.putOvsdbTerminationPoints(hopOvsdbBridgePairList.get(index).ovsdbBridgeAugmentation,
                            sffDataPlaneLocatorList, opendaylightSfc.getExecutor());
                }
            }
        }


        // RSP UPDATE - RSP CANNOT BE UPDATED
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
            Object[] methodParams = {SfcOvsUtil.buildOvsdbBridgeIID(hop.getServiceFunctionForwarder())};
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
}
