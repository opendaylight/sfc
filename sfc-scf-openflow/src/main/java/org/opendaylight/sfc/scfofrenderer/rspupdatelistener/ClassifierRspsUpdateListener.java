/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.rspupdatelistener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.provider.listeners.AbstractDataTreeChangeListener;
import org.opendaylight.sfc.scfofrenderer.OpenflowClassifierProcessor;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassifierRspsUpdateListener extends AbstractDataTreeChangeListener<RenderedServicePath> {
    private static final Logger LOG = LoggerFactory.getLogger(ClassifierRspsUpdateListener.class);
    private final DataBroker dataBroker;
    private ListenerRegistration<AbstractDataTreeChangeListener> listenerRegistration;
    private OpenflowClassifierProcessor classifierProcessor;
    private SfcOfFlowWriterInterface openflowWriter;
    private ClassifierRspUpdateDataGetter updateDataGetter;
    private LogicalClassifierDataGetter dataGetter;

    public ClassifierRspsUpdateListener(DataBroker theDataBroker,
                                        OpenflowClassifierProcessor theClassifierProcessor,
                                        SfcOfFlowWriterInterface theOpenflowWriter,
                                        ClassifierRspUpdateDataGetter theUpdateDataGetter,
                                        LogicalClassifierDataGetter theDataGetter) {
        dataBroker = theDataBroker;
        classifierProcessor = theClassifierProcessor;
        openflowWriter = theOpenflowWriter;
        updateDataGetter = theUpdateDataGetter;
        dataGetter = theDataGetter;
        // TODO - uncomment line below to activate listeners
//        registerListeners();
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
        // classifier doesn't care about added RSPs
    }

    @Override
    protected void remove(RenderedServicePath deletedRsp) {
        // TODO - should we delete the classifier flows related to a deleted RSP?...
        // I'm inclined to say no, because we don't do anything when an RSP is added
    }

    @Override
    protected void update(RenderedServicePath originalRsp, RenderedServicePath updatedRsp) {
        // TODO - identify relevant updates
        // TODO - trigger re-rendering of classifier flows
    }

    @Override
    public void close() throws Exception { listenerRegistration.close(); }
}
