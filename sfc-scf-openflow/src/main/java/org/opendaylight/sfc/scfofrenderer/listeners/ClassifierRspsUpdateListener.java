/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.listeners;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.scfofrenderer.rspupdatelistener.ClassifierRspUpdateDataGetter;
import org.opendaylight.sfc.scfofrenderer.processors.ClassifierRspUpdateProcessor;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ClassifierRspsUpdateListener extends AbstractDataTreeChangeListener<RenderedServicePath> {
    private static final Logger LOG = LoggerFactory.getLogger(ClassifierRspsUpdateListener.class);
    private final DataBroker dataBroker;
    private ListenerRegistration<AbstractDataTreeChangeListener> listenerRegistration;
    private final ClassifierRspUpdateProcessor classifierProcessor;
    private final SfcOfFlowWriterInterface openflowWriter;
    private final ClassifierRspUpdateDataGetter updateDataGetter;
    private final LogicalClassifierDataGetter dataGetter;

    public ClassifierRspsUpdateListener(DataBroker theDataBroker,
                                        ClassifierRspUpdateProcessor theClassifierProcessor,
                                        SfcOfFlowWriterInterface theOpenflowWriter,
                                        ClassifierRspUpdateDataGetter theUpdateDataGetter,
                                        LogicalClassifierDataGetter theDataGetter) {
        dataBroker = theDataBroker;
        classifierProcessor = theClassifierProcessor;
        openflowWriter = theOpenflowWriter;
        updateDataGetter = theUpdateDataGetter;
        dataGetter = theDataGetter;
        registerListeners();
    }

    private void registerListeners() {
        LOG.info("Registering listener");
        final DataTreeIdentifier<RenderedServicePath> treeId = new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL,
                SfcInstanceIdentifiers.RSP_ENTRY_IID);
        listenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    protected void add(RenderedServicePath newRsp) {
        // nothing to do when RSPs are added
    }

    @Override
    protected void remove(RenderedServicePath deletedRsp) {
        // nothing to do when RSPs are deleted
    }

    @Override
    protected void update(RenderedServicePath originalRsp, RenderedServicePath updatedRsp) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("update - old rspId: {}; new rspId: {}", originalRsp, updatedRsp);
        }

        Optional<DpnIdType> originalDataplaneId = dataGetter.getFirstHopDataplaneId(originalRsp);
        Optional<DpnIdType> updatedDataplaneId = dataGetter.getFirstHopDataplaneId(updatedRsp);

        if (originalDataplaneId.isPresent() &&
                updatedDataplaneId.isPresent() &&
                !originalDataplaneId.get().equals(updatedDataplaneId.get())) {
            LOG.info("update - First SF of RSP [{}] *moved*. Old DPNID: {}; New DPNID: {}",
                    originalRsp.getPathId(),
                    originalDataplaneId.get(),
                    updatedDataplaneId.get());
            RspName theRspName = originalRsp.getName();
            // first SF moved elsewhere; delete old flows, then create new flows (for now, separate transactions)
            // keep in mind that the only flows in the openflow are classifier flows - the RSP flows are in another
            // of writer instance
            openflowWriter.deleteRspFlows(originalRsp.getPathId());
            openflowWriter.deleteFlowSet();
            List<Acl> theAcls = updateDataGetter.filterAclsByRspName(theRspName);

            theAcls.forEach(acl -> {
                List<SclServiceFunctionForwarder> theClassifierObjects =
                        updateDataGetter.filterClassifierNodesByAclName(acl.getAclName());
                theClassifierObjects.forEach(classifier -> {
                    List<FlowDetails> allFlows = classifierProcessor.processClassifier(classifier, acl, updatedRsp);
                    if(!allFlows.isEmpty()) {
                        LOG.debug("update - Flows generated; gonna write them into the DS ");
                        openflowWriter.writeFlows(allFlows);
                        openflowWriter.flushFlows();
                    }
                });
            });
        }
    }

    @Override
    public void close() throws Exception { listenerRegistration.close(); }
}
