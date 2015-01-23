/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ofsfc.provider;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
// This class is instantiated from:
//      org.opendaylight.controller.config.yang.config.sfcofl2_provider.impl.SfcOFL2ProviderModule.createInstance()
// It is a general entry point for the sfcofl2 feature/plugin
//

public class SfcL2Renderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2Renderer.class);
    protected ExecutorService executor;

    // TODO both dataBroker and rpcProvider are temporary. When the SfcL2FlowProgrammer is
    //      refactored to use the SfcProvider.SfcDataStoreAPI, then both attributes will be removed

    public SfcL2Renderer(DataBroker dataBroker, RpcProviderRegistry rpcProvider) {
        LOG.info("SfcL2Renderer starting the SfcL2Renderer plugin...");

        executor = Executors.newFixedThreadPool(1); // TODO this may no longer be needed

        SfcL2FlowProgrammer sfcL2FlowProgrammer = new SfcL2FlowProgrammer(rpcProvider);
        SfcL2SfpDataListener openflowSfpDataListener = new SfcL2SfpDataListener(dataBroker, sfcL2FlowProgrammer);
        SfcL2AclDataListener openflowAclDataListener = new SfcL2AclDataListener(dataBroker, sfcL2FlowProgrammer);

        LOG.info("SfcL2Renderer successfully started the SfcL2Renderer plugin");
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        // TODO When we close this service we need to shutdown our executor!
        LOG.info("SfcL2Renderer auto-closed");
    }
}