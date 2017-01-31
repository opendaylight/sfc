/*
 * Copyright (c) 2014, 2017 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.lisp;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class SfcLispListener implements ISfcLispListener, BindingAwareConsumer {

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        DataBroker dataBrokerService = session.getSALService(DataBroker.class);
    }

    void setBindingAwareBroker(BindingAwareBroker bindingAwareBroker) {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bindingAwareBroker.registerConsumer(this, bundleContext);
    }
}
