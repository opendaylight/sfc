/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ofsfc.provider;


import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenflowSfcRenderer implements AutoCloseable {

    protected ExecutorService executor;
    protected DataBroker dataBroker;
    protected RpcProviderRegistry rpcProvider;
    private static OpenflowSfcRenderer openflowSfcRendererObj;
    // TODO why are these defined as class members if only used once in constructor??? Can the be removed??
    private OpenflowSfpDataListener openflowSfpDataListener;
    private OpenflowAclDataListener openflowAclDataListener;

    public OpenflowSfcRenderer(DataBroker dataBroker, RpcProviderRegistry rpcProvider) {
        executor = Executors.newFixedThreadPool(1);
        openflowSfcRendererObj = this;
        setDataBroker(dataBroker);
        setRpcProvider(rpcProvider);
        openflowSfpDataListener = new OpenflowSfpDataListener(dataBroker);
        openflowAclDataListener = new OpenflowAclDataListener(dataBroker);
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public DataBroker getDataProvider() {
        return dataBroker;
    }

    public void setRpcProvider(RpcProviderRegistry rpcProvider) {
        this.rpcProvider = rpcProvider;
    }

    public RpcProviderRegistry getRpcProvider() {
        return rpcProvider;
    }

    public static OpenflowSfcRenderer getOpendaylightSfcObj() {
        return OpenflowSfcRenderer.openflowSfcRendererObj;
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!
    }
}