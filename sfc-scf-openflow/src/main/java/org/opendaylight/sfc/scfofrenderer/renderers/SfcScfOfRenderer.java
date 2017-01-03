/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.renderers;

import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.scfofrenderer.listeners.SfcScfOfDataListener;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.BareClassifier;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.LogicallyAttachedClassifier;
import org.opendaylight.sfc.scfofrenderer.processors.OpenflowClassifierProcessor;
import org.opendaylight.sfc.scfofrenderer.processors.SfcScfOfProcessor;
import org.opendaylight.sfc.scfofrenderer.rspupdatelistener.ClassifierRspUpdateDataGetter;
import org.opendaylight.sfc.scfofrenderer.processors.ClassifierRspUpdateProcessor;
import org.opendaylight.sfc.scfofrenderer.listeners.ClassifierRspsUpdateListener;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterImpl;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// General entry point for the sfc-scf-openflow feature/plugin

public class SfcScfOfRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfRenderer.class);
    private SfcScfOfDataListener sfcScfDataListener = null;
    private ClassifierRspsUpdateListener classifierRspsUpdateListener;

    public SfcScfOfRenderer(DataBroker dataBroker,
                            NotificationProviderService notificationService,
                            RpcProviderRegistry theRpcProvider) {
        LOG.info("SfcScfOfRenderer starting the SfcScfOfRenderer plugin...");

        // this transaction object will be afterwards injected into the SfcOfFlowWriterInterface
        WriteTransaction theTx = dataBroker.newWriteOnlyTransaction();

        SfcOfFlowWriterInterface openflowWriter = new SfcOfFlowWriterImpl(dataBroker);

        LogicalClassifierDataGetter dataGetter =
                new LogicalClassifierDataGetter(new SfcGeniusRpcClient(theRpcProvider));

        LogicallyAttachedClassifier logicalClassifier = new LogicallyAttachedClassifier(dataGetter);

        OpenflowClassifierProcessor logicalClassifierHandler =
                new OpenflowClassifierProcessor(theTx, logicalClassifier, new BareClassifier());

        // register the classifierProcessor as a listener of the transaction within the OF writer
        openflowWriter.registerTransactionListener(logicalClassifierHandler);

        // this over-writes the transaction within the openflow writer, which makes the openflowWriter &
        // the classifierProcessor share the same transaction object
        openflowWriter.injectTransaction(theTx);

        classifierRspsUpdateListener = new ClassifierRspsUpdateListener(dataBroker,
                new ClassifierRspUpdateProcessor(logicalClassifier),
                openflowWriter,
                new ClassifierRspUpdateDataGetter(),
                dataGetter);
        sfcScfDataListener = new SfcScfOfDataListener(dataBroker,
                new SfcScfOfProcessor(openflowWriter, logicalClassifierHandler));

        LOG.info("SfcScfOfRenderer successfully started the SfcScfOfRenderer plugin");
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        LOG.info("SfcScfOfRenderer auto-closed");
        try {
            sfcScfDataListener.close();
            classifierRspsUpdateListener.close();
        } catch(Exception e) {
            LOG.error("SfcScfOfRenderer auto-closed exception {}", e.getMessage());
        }
    }
}
