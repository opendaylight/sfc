/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
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
//import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
//import org.opendaylight.sfc.provider.OpendaylightSfc;

public class SfcLispListener implements ISfcLispListener, BindingAwareConsumer {

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        DataBroker dataBrokerService = session.getSALService(DataBroker.class);

//        // ServiceFunctionFowarder Entry
//        SfcLispProviderSffDataListener sfcProviderSffDataListener = new SfcLispProviderSffDataListener();
//        dataBrokerService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.SFF_IID, sfcProviderSffDataListener,
//                DataBroker.DataChangeScope.SUBTREE);
//
//        // ServiceFunction Entry
//        SfcLispProviderSfEntryDataListener sfcProviderSfEntryDataListener = new SfcLispProviderSfEntryDataListener();
//        dataBrokerService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.SF_ENTRY_IID, sfcProviderSfEntryDataListener,
//                DataBroker.DataChangeScope.SUBTREE);

    }

    void setBindingAwareBroker(BindingAwareBroker bindingAwareBroker) {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bindingAwareBroker.registerConsumer(this, bundleContext);
    }

}
