/*
 * Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_netconf.provider;

import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;


public class SfcNetconfDataProvider implements BindingAwareConsumer {

    private MountPointService mountService;
    static SfcNetconfDataProvider sfcNetconfDataProvider;

    /*
     * Factory method
     */
    public static SfcNetconfDataProvider GetNetconfDataProvider() {
        if (sfcNetconfDataProvider != null)
            return sfcNetconfDataProvider;
        else {
            sfcNetconfDataProvider = new SfcNetconfDataProvider();
            return sfcNetconfDataProvider;
        }
    }

    public MountPointService getMountService() {
        return mountService;
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        mountService = session.getSALService(MountPointService.class);
    }

}
