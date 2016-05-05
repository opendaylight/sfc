/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_netconf.provider;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;

public class SfcNetconfDataProvider implements BindingAwareProvider {

    private MountPointService mountService;
    private static SfcNetconfDataProvider sfcNetconfDataProvider;

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
    public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
        mountService = session.getSALService(MountPointService.class);
    }
}
