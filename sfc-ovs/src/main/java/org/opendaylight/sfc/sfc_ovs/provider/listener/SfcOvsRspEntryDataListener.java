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

import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
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
}
