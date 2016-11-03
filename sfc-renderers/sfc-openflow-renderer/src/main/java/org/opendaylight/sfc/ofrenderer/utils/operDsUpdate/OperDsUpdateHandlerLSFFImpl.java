/*
 * Copyright (c) 2016 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.utils.operDsUpdate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.sfc.ofrenderer.processors.SffGraph;
import org.opendaylight.sfc.ofrenderer.processors.SffGraph.SffGraphEntry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderStateKey;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.RspLogicalSffAugmentation;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.RspLogicalSffAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.SffLogicalSffAugmentation;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.DpnRsps;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.Dpn;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.DpnBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.DpnKey;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.RspsForDpnid;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.RspsForDpnidBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.rsps._for.dpnid.Rsps;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.rsps._for.dpnid.RspsBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.rsps._for.dpnid.RspsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

/**
 * Implementation of {@link OperDsUpdateHandlerInterface} for the Logical SFF
 * @author Diego Granados (diego.jesus.granados.lopez@ericsson.com)
 *
 */
public class OperDsUpdateHandlerLSFFImpl implements OperDsUpdateHandlerInterface {

    private static final Logger LOG = LoggerFactory.getLogger(OperDsUpdateHandlerLSFFImpl.class);

    private ExecutorService threadPoolExecutorService;
    private DataBroker dataBroker=null;

    public OperDsUpdateHandlerLSFFImpl(DataBroker r) {
        dataBroker=r;
        threadPoolExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Updates dpnid information in the SFF state part of the operational data
     * model (this method performs the addition of dpnids/RSPs for logical SFFs)
     * The changes are added only to the passed transaction; it will be committed
     * in a latter phase, after all datastore changes have been added
     * @param theGraph The graph used for rendering
     * @param rsp  The rendered service path
     * @param transaction  The write transaction to which the datastore operations
     * will be added
     */
    private void updateSffStateWithDpnIds(
            SffGraph theGraph, RenderedServicePath rsp, WriteTransaction trans) {

        LOG.debug(
                "updateSffStateWithDpnIds: starting addition of dpnids-rsps to RSP state");
        Iterator<SffGraphEntry> graphEntries = theGraph.getGraphEntryIterator();
        SffGraphEntry aGraphEntry = null;

        Map<BigInteger, List<Rsps>> valuesMap = new HashMap<>();
        while (graphEntries.hasNext()) {
            aGraphEntry = graphEntries.next();
            if (aGraphEntry.getDstSff().equals(SffGraph.EGRESS)) {
                continue;
            }
            if (aGraphEntry.getDstDpnId() == null) {
                continue;
            }
            ServiceFunctionForwarderStateKey sffKey = new ServiceFunctionForwarderStateKey(
                    aGraphEntry.getDstSff());
            InstanceIdentifier<Dpn> dpnidIif = InstanceIdentifier
                    .builder(ServiceFunctionForwardersState.class)
                    .child(ServiceFunctionForwarderState.class, sffKey)
                    .augmentation(SffLogicalSffAugmentation.class)
                    .child(DpnRsps.class)
                    .child(Dpn.class, new DpnKey(aGraphEntry.getDstDpnId()))
                    .build();

            RspsForDpnidBuilder builder = new RspsForDpnidBuilder();
            List<Rsps> values = valuesMap.get(aGraphEntry.getDstDpnId().getValue());
            if (values == null) {
                values = new ArrayList<>();
                LOG.debug("updateSffStateWithDpnIds: new rsp list for dpnid {}",
                        aGraphEntry.getDstDpnId().getValue());
                valuesMap.put(aGraphEntry.getDstDpnId().getValue(), values);
            } else {
                LOG.debug("updateSffStateWithDpnIds: rsp list already existing for dpnid {}",
                        aGraphEntry.getDstDpnId().getValue());
            }
            values.add(new RspsBuilder().setKey(new RspsKey(new SfpName(rsp.getName().getValue()))).build());
            builder.setRsps(values);
            RspsForDpnid dpnRsps = builder.build();
            Dpn dpnidInfo = new DpnBuilder()
                    .setKey(new DpnKey(aGraphEntry.getDstDpnId()))
                    .setRspsForDpnid(dpnRsps)
                    .build();

            LOG.debug(
                    "updateSffStateWithDpnIds: iid: {}; info: {}", dpnidIif, dpnidInfo);
            trans.merge(LogicalDatastoreType.OPERATIONAL, dpnidIif, dpnidInfo,
                    true);
        }
    }

    /**
     * Updates dpnid information in the SFF state part of the operational data
     * model (this method performs the removal of dpnids/RSPs from logical SFFs)
     * The changes are added only to the passed transaction; it will be committed
     * in a latter phase, after all datastore changes have been added
     * @param rsp  The rendered service path that is being deleted
     * @param transaction  The write transaction to which the datastore operations
     * will be added
     */
    private void deleteRspFromSffState(
            RenderedServicePath rsp, WriteTransaction transaction) {
        LOG.debug(
                "deleteRspFromSffState: starting deletion in dpnids-rsps");

        Iterator<RenderedServicePathHop> hops = rsp.getRenderedServicePathHop().iterator();
        RenderedServicePathHop aHop = null;

        while (hops.hasNext()) {
            aHop = hops.next();
            SffName sffName =  aHop.getServiceFunctionForwarder();
            RspLogicalSffAugmentation lsffAugmentation = aHop.getAugmentation(RspLogicalSffAugmentation.class);
            if (lsffAugmentation == null) {
                continue;
            }
            DpnIdType dpnid = lsffAugmentation.getDpnId();
            if (dpnid == null) {
                continue;
            }
           ServiceFunctionForwarderStateKey sffKey = new ServiceFunctionForwarderStateKey(
                   sffName);
            RspsForDpnidBuilder builder = new RspsForDpnidBuilder();
            List<Rsps>values = new ArrayList<>();
            values.add(new RspsBuilder().setKey(new RspsKey(new SfpName(rsp.getName().getValue()))).build());
            builder.setRsps(values);

            InstanceIdentifier<Rsps> dpnidIif = InstanceIdentifier
                    .builder(ServiceFunctionForwardersState.class)
                    .child(ServiceFunctionForwarderState.class, sffKey)
                    .augmentation(SffLogicalSffAugmentation.class)
                    .child(DpnRsps.class)
                    .child(Dpn.class, new DpnKey(dpnid))
                    .child(RspsForDpnid.class)
                    .child(Rsps.class, new RspsKey(new SfpName(rsp.getName().getValue())))
                    .build();

            LOG.debug(
                    "deleteRspFromSffState: iid: {}; ", dpnidIif);
            transaction.delete(LogicalDatastoreType.OPERATIONAL, dpnidIif);
        }
    }

    /**
     * Updates dpnid information in the RSP state part of the operational data
     * model. This method just adds the changes to the passed transaction; the
     * transaction will later be committed after all datastore changes have been
     * added
     * @param theGraph The graph used for rendering
     * @param rsp  The rendered service path
     * @param transaction  The write transaction to which the datastore operations
     * will be added
     */
    private void updateRenderedServicePathOperationalStateWithDpnIds(
            SffGraph theGraph, RenderedServicePath rsp, WriteTransaction transaction) {

        LOG.debug("updateRenderedServicePathOperationalStateWithDpnIds: "
                + "starting addition of dpnids to the RSP");
        Iterator<SffGraphEntry> graphEntries = theGraph.getGraphEntryIterator();
        SffGraphEntry aGraphEntry = null;
        RenderedServicePathKey rspKey = new RenderedServicePathKey(
                rsp.getName());
        short hopIndex = 0;
        while (graphEntries.hasNext()) {
            aGraphEntry = graphEntries.next();
            if (aGraphEntry.getDstSff().equals(SffGraph.EGRESS)) {
                continue;
            }
            InstanceIdentifier<RspLogicalSffAugmentation> iidRspHop = InstanceIdentifier
                    .builder(RenderedServicePaths.class)
                    .child(RenderedServicePath.class, rspKey)
                    .child(RenderedServicePathHop.class,
                            new RenderedServicePathHopKey(hopIndex++))
                    .augmentation(RspLogicalSffAugmentation.class).build();
            RspLogicalSffAugmentation augm = new RspLogicalSffAugmentationBuilder()
                    .setDpnId(aGraphEntry.getDstDpnId()).build();
            LOG.debug(
                    "updateRenderedServicePathOperationalStateWithDpnIds: iid: {}; agumentation: {}", iidRspHop, augm);
            transaction.put(LogicalDatastoreType.OPERATIONAL, iidRspHop, augm, true);
        }
    }

    /**
     * Asynchronous commit of the passed transaction
     * @param trans The transaction to submit
     */
    private void commitChangesAsync(WriteTransaction trans) {
        threadPoolExecutorService.submit( () -> {
            CheckedFuture<Void, TransactionCommitFailedException> submitFuture = trans
                    .submit();
            try {
                submitFuture.checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("commitChangesAsync: Transaction failed. Message: {}",
                        e.getMessage(), e);
            }
        });
    }

    @Override
    public void onRspCreation(SffGraph theGraph, RenderedServicePath rsp) {

        WriteTransaction trans = dataBroker.newWriteOnlyTransaction();
        updateRenderedServicePathOperationalStateWithDpnIds(theGraph, rsp, trans);
        updateSffStateWithDpnIds(theGraph, rsp, trans);
        commitChangesAsync(trans);
    }

    @Override
    public void onRspDeletion(RenderedServicePath rsp) {

        WriteTransaction trans = dataBroker.newWriteOnlyTransaction();
        deleteRspFromSffState(rsp, trans);
        commitChangesAsync(trans);
    }
}
