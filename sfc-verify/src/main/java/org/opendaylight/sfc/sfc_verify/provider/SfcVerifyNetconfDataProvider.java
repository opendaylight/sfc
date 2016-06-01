/*
 * Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;

import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.NotificationService;

public class SfcVerifyNetconfDataProvider implements BindingAwareConsumer {

    private MountPointService mountService;
    private NotificationService notificationService;
    private static SfcVerifyNetconfDataProvider sfcVerifyNetconfDataProvider;

    static {
        sfcVerifyNetconfDataProvider = new SfcVerifyNetconfDataProvider();
    }

    /*
     * Factory method
     */
    public static SfcVerifyNetconfDataProvider getNetconfDataProvider() {
        return sfcVerifyNetconfDataProvider;
    }

    public MountPointService getMountService() {
        return mountService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        mountService = session.getSALService(MountPointService.class);
        notificationService = session.getSALService(NotificationService.class);
    }
}
