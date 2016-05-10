/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider;

import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;

public class SfcIosXeDataProvider implements BindingAwareProvider {

    private MountPointService mountService;
    private static SfcIosXeDataProvider sfcIosXeDataProvider;

    public static SfcIosXeDataProvider GetNetconfDataProvider() {
        if (sfcIosXeDataProvider != null)
            return sfcIosXeDataProvider;
        else {
            sfcIosXeDataProvider = new SfcIosXeDataProvider();
            return sfcIosXeDataProvider;
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
