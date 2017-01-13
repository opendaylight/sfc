/*
 * Copyright (c) 2017 Ericsson, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the SFC SFF-DPL config datastore
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @version 0.1
 * @since 2017-01-18
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcSffToOvsMappingAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOvsSffDplListener extends AbstractDataTreeChangeListener<SffDataPlaneLocator> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsSffDplListener.class);
    private final DataBroker dataBroker;
    private ListenerRegistration<SfcOvsSffDplListener> listenerRegistration;

    // TODO is this necessary????
    protected static ExecutorService executor = Executors.newFixedThreadPool(5);


    public SfcOvsSffDplListener(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        LOG.debug("Initializing...");
        registerListeners();
    }

    private void registerListeners() {
        final DataTreeIdentifier<SffDataPlaneLocator> treeId = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class)
                .child(SffDataPlaneLocator.class));
        listenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

    @Override
    protected void add(SffDataPlaneLocator sffDpl) {
        LOG.info("\nCreated SFF DPL: {}", sffDpl.toString());

        if(!isSffDplOvsIpLocator(sffDpl)) {
            // we can skip this DPL
            return;
        }

        modifySffDplOvsdbTerminationPoint(sffDpl, true);
    }

    @Override
    protected void remove(SffDataPlaneLocator sffDpl) {
        LOG.info("\nDeleted SFF DPL: {}", sffDpl.toString());

        // Only delete the port if this SFF is OVS augmented and the transport is VxGpe
        if(!isSffDplOvsIpLocator(sffDpl)) {
            // we can skip this DPL
            return;
        }

        modifySffDplOvsdbTerminationPoint(sffDpl, false);
    }

    @Override
    protected void update(SffDataPlaneLocator originalSffDpl, SffDataPlaneLocator updatedSffDpl) {
        LOG.info("\nModified SFF DPL: {}", originalSffDpl.toString());

        if(!isSffDplOvsIpLocator(updatedSffDpl)) {
            // we can skip this DPL
            return;
        }

        modifySffDplOvsdbTerminationPoint(updatedSffDpl, true);
    }

    private boolean isSffDplOvsIpLocator(SffDataPlaneLocator sffDpl) {
        SffOvsLocatorOptionsAugmentation sffOvsOptions = sffDpl.getAugmentation(SffOvsLocatorOptionsAugmentation.class);
        if(sffOvsOptions == null || !sffDpl.getDataPlaneLocator().getTransport().equals(VxlanGpe.class)) {
            // we can skip this DPL
            return false;
        }

        return true;
    }

    private void modifySffDplOvsdbTerminationPoint(SffDataPlaneLocator sffDpl, boolean doAdd) {
        // We need to get the SFF for this SFF-DPL, so look for it based on the SFF-DPL IP
        CheckedFuture<Optional<ServiceFunctionForwarders>, ReadFailedException> exitsingSffs = readServiceFunctionForwarders();
        IpPortLocator ipLocator = (IpPortLocator) sffDpl.getDataPlaneLocator().getLocatorType();
        Futures.addCallback(exitsingSffs, new FutureCallback<Optional<ServiceFunctionForwarders>>() {

            @Override
            public void onSuccess(Optional<ServiceFunctionForwarders> optionalSffs) {
                if (optionalSffs.isPresent()) {
                    ServiceFunctionForwarder sff = SfcOvsUtil.findSffByIp(optionalSffs.get(), ipLocator.getIp());
                    if(sff != null) {
                        if(doAdd) {
                            // delete OvsdbTerminationPoint
                            NodeId ovsdbBridgeNodeId = SfcOvsUtil.getOvsdbAugmentationNodeIdBySff(sff);
                            SfcOvsUtil.deleteOvsdbTerminationPoint(
                                    SfcOvsUtil.buildOvsdbTerminationPointIID(ovsdbBridgeNodeId, sffDpl.getName().getValue()),
                                    executor);
                        } else {
                            // put Termination Points
                            OvsdbBridgeAugmentation ovsdbBridge = SfcSffToOvsMappingAPI.buildOvsdbBridgeAugmentation(sff, executor);
                            SfcOvsUtil.putOvsdbTerminationPoints(ovsdbBridge, sff.getSffDataPlaneLocator(), executor);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Failed to read SFFs from data store.");
            }
        });
    }

    private CheckedFuture<Optional<ServiceFunctionForwarders>, ReadFailedException> readServiceFunctionForwarders() {
        ReadTransaction rTx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<ServiceFunctionForwarders> sffIid = InstanceIdentifier.builder(
                ServiceFunctionForwarders.class).build();
        return rTx.read(LogicalDatastoreType.CONFIGURATION, sffIid);
    }
}
