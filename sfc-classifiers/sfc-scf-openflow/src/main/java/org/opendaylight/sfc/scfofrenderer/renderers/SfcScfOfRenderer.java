/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.renderers;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.BareClassifier;
import org.opendaylight.sfc.scfofrenderer.flowgenerators.LogicallyAttachedClassifier;
import org.opendaylight.sfc.scfofrenderer.listeners.ClassifierRspsUpdateListener;
import org.opendaylight.sfc.scfofrenderer.listeners.SfcScfOfDataListener;
import org.opendaylight.sfc.scfofrenderer.logicalclassifier.LogicalClassifierDataGetter;
import org.opendaylight.sfc.scfofrenderer.processors.ClassifierRspUpdateProcessor;
import org.opendaylight.sfc.scfofrenderer.processors.OpenflowClassifierProcessor;
import org.opendaylight.sfc.scfofrenderer.processors.SfcScfOfProcessor;
import org.opendaylight.sfc.scfofrenderer.rspupdatelistener.ClassifierRspUpdateDataGetter;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterImpl;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// General entry point for the sfc-scf-openflow feature/plugin
@Singleton
public class SfcScfOfRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfRenderer.class);
    private final ClassifierRspsUpdateListener classifierRspsUpdateListener;
    private final SfcScfOfDataListener sfcScfDataListener;

    @Inject
    public SfcScfOfRenderer(DataBroker dataBroker, NotificationProviderService notificationService,
                            RpcConsumerRegistry rpcRegistry) {
        LOG.info("SfcScfOfRenderer starting the SfcScfOfRenderer plugin...");

        // this transaction object will be afterwards injected into the
        // SfcOfFlowWriterInterface
        WriteTransaction theTx = dataBroker.newWriteOnlyTransaction();

        SfcOfFlowWriterInterface openflowWriter = new SfcOfFlowWriterImpl(dataBroker);

        LogicalClassifierDataGetter dataGetter = new LogicalClassifierDataGetter(
                new SfcGeniusRpcClient(rpcRegistry));

        LogicallyAttachedClassifier logicalClassifier = new LogicallyAttachedClassifier(dataGetter);

        OpenflowClassifierProcessor logicalClassifierHandler = new OpenflowClassifierProcessor(theTx, logicalClassifier,
                new BareClassifier());

        // register the classifierProcessor as a listener of the transaction
        // within the OF writer
        openflowWriter.registerTransactionListener(logicalClassifierHandler);

        // this over-writes the transaction within the openflow writer, which
        // makes the openflowWriter &
        // the classifierProcessor share the same transaction object
        openflowWriter.injectTransaction(theTx);

        classifierRspsUpdateListener = new ClassifierRspsUpdateListener(dataBroker,
                new ClassifierRspUpdateProcessor(logicalClassifier), openflowWriter,
                new ClassifierRspUpdateDataGetter(), dataGetter);
        sfcScfDataListener = new SfcScfOfDataListener(dataBroker,
                new SfcScfOfProcessor(openflowWriter, logicalClassifierHandler));
        classifierRspsUpdateListener.register();
        sfcScfDataListener.register();

        LOG.info("SfcScfOfRenderer successfully started the SfcScfOfRenderer plugin");
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close()  {
        classifierRspsUpdateListener.close();
        sfcScfDataListener.close();
        LOG.info("SfcScfOfRenderer auto-closed");
    }
}
