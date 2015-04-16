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
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcSffToOvsMappingAPI;
import org.opendaylight.sfc.sfc_ovs.provider.util.HopOvsdbBridgePair;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
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

                List<HopOvsdbBridgePair> hopOvsdbBridgePairList =
                        HopOvsdbBridgePair.buildHopOvsdbBridgePairList(
                                renderedServicePath, getOpendaylightSfc().getExecutor());

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

        // RSP UPDATE N/A (RSP CANNOT BE UPDATED)

        // RSP DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof RenderedServicePath) {
                RenderedServicePath deletedRenderedServicePath = (RenderedServicePath) dataObject;
                LOG.debug("\nDeleted Rendered Service Path: {}", deletedRenderedServicePath.toString());

                List<HopOvsdbBridgePair> hopOvsdbBridgePairList =
                        HopOvsdbBridgePair.buildHopOvsdbBridgePairList(
                                deletedRenderedServicePath, getOpendaylightSfc().getExecutor());

                for (int index = 0; index < hopOvsdbBridgePairList.size(); index++) {

                    //delete forward VXLAN tunnel
                    if (index + 1 < hopOvsdbBridgePairList.size()) {
                        String dplName = SfcSffToOvsMappingAPI.buildVxlanTunnelDataPlaneLocatorName(
                                deletedRenderedServicePath,
                                hopOvsdbBridgePairList.get(index), hopOvsdbBridgePairList.get(index + 1));

                        SfcOvsUtil.deleteOvsdbTerminationPoint(
                                SfcOvsUtil.buildOvsdbTerminationPointIID(
                                        hopOvsdbBridgePairList.get(index).renderedServicePathHop.getServiceFunctionForwarder(),
                                        dplName),
                                opendaylightSfc.getExecutor());
                    }

                    //delete backward VXLAN tunnel
                    if (index - 1 >= 0) {
                        String dplName = SfcSffToOvsMappingAPI.buildVxlanTunnelDataPlaneLocatorName(
                                deletedRenderedServicePath,
                                hopOvsdbBridgePairList.get(index), hopOvsdbBridgePairList.get(index - 1));

                        SfcOvsUtil.deleteOvsdbTerminationPoint(
                                SfcOvsUtil.buildOvsdbTerminationPointIID(
                                        hopOvsdbBridgePairList.get(index).renderedServicePathHop.getServiceFunctionForwarder(),
                                        dplName),
                                opendaylightSfc.getExecutor());
                    }
                }

            }
        }
        printTraceStop(LOG);
    }
}
