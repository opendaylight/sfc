/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetflocProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NetflocProvider.class);
    private ConfigActivator activator;
    private BundleContext bundleContext = null;

    public NetflocProvider(BundleContext bundleContext) {
    	this.bundleContext = bundleContext;
    	LOG.info("NetflocProvider bundleContext: {}", bundleContext);
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("NetflocProvider Session Initiated");
        this.activator = new ConfigActivator(session);
        try {
            activator.start(bundleContext);
        } catch (Exception e) {
            LOG.warn("Failed to start NetflocProvider: ", e);
        }
    }

    @Override
    public void close() throws Exception {
        LOG.info("NetflocProvider Closed");
    }

}
