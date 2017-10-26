/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.listeners;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.scfofrenderer.processors.ClassifierRspUpdateProcessor;
import org.opendaylight.sfc.scfofrenderer.rspupdatelistener.ClassifierRspUpdateDataGetter;
import org.opendaylight.sfc.util.openflow.writer.FlowDetails;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes on the Rendered Service Paths (RSPs) to reprogram classifiers accordingly.
 */
@Singleton
public class ClassifierRspsUpdateListener extends AbstractSyncDataTreeChangeListener<RenderedServicePath> {

    private static final Logger LOG = LoggerFactory.getLogger(ClassifierRspsUpdateListener.class);

    private final ClassifierRspUpdateProcessor classifierProcessor;
    private final SfcOfFlowWriterInterface openflowWriter;
    private final ClassifierRspUpdateDataGetter updateDataGetter;
    private final LogicalClassifierDataGetter dataGetter;

    @Inject
    public ClassifierRspsUpdateListener(DataBroker dataBroker,
                                        ClassifierRspUpdateProcessor classifierRspUpdateProcessor,
                                        SfcOfFlowWriterInterface sfcOfFlowWriterInterface,
                                        ClassifierRspUpdateDataGetter classifierRspUpdateDataGetter,
                                        LogicalClassifierDataGetter logicalClassifierDataGetter) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, SfcInstanceIdentifiers.RSP_ENTRY_IID);
        this.classifierProcessor = classifierRspUpdateProcessor;
        this.openflowWriter = sfcOfFlowWriterInterface;
        this.updateDataGetter = classifierRspUpdateDataGetter;
        this.dataGetter = logicalClassifierDataGetter;
    }

    @Override
    public void add(@Nonnull RenderedServicePath renderedServicePath) {
        // nothing to do when RSPs are added
    }

    @Override
    public void remove(@Nonnull RenderedServicePath removedRenderedServicePath) {
        // nothing to do when RSPs are deleted
    }

    @Override
    public void update(@Nonnull RenderedServicePath originalRenderedServicePath,
                       RenderedServicePath updatedRenderedServicePath) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("update - old rspId: {}; new rspId: {}", originalRenderedServicePath, updatedRenderedServicePath);
        }

        Optional<DpnIdType> originalDataplaneId = dataGetter.getFirstHopDataplaneId(originalRenderedServicePath);
        Optional<DpnIdType> updatedDataplaneId = dataGetter.getFirstHopDataplaneId(updatedRenderedServicePath);

        if (originalDataplaneId.isPresent() && updatedDataplaneId.isPresent() && !originalDataplaneId.get()
                .equals(updatedDataplaneId.get())) {
            LOG.info("update - First SF of RSP [{}] *moved*. Old DPNID: {}; New DPNID: {}",
                     originalRenderedServicePath.getPathId(), originalDataplaneId.get(), updatedDataplaneId.get());
            RspName theRspName = originalRenderedServicePath.getName();
            // first SF moved elsewhere; delete old flows, then create new flows
            // (for now, separate transactions)
            // keep in mind that the only flows in the openflow are classifier
            // flows - the RSP flows are in another
            // of writer instance
            openflowWriter.deleteRspFlows(originalRenderedServicePath.getPathId());
            openflowWriter.deleteFlowSet();
            List<Acl> theAcls = updateDataGetter.filterAclsByRspName(theRspName);

            theAcls.forEach(acl -> {
                List<SclServiceFunctionForwarder> theClassifierObjects = updateDataGetter
                        .filterClassifierNodesByAclName(acl.getAclName());
                theClassifierObjects.forEach(classifier -> {
                    List<FlowDetails> allFlows = classifierProcessor
                            .processClassifier(classifier, acl, updatedRenderedServicePath);
                    if (!allFlows.isEmpty()) {
                        LOG.debug("update - Flows generated; gonna write them into the DS ");
                        openflowWriter.writeFlows(allFlows);
                        openflowWriter.flushFlows();
                    }
                });
            });
        }
    }
}
