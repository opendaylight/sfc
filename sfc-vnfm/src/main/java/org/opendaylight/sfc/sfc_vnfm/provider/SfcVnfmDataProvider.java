/*
 * Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_vnfm.provider;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;

import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;

public class SfcVnfmDataProvider implements BindingAwareConsumer {

    static SfcVnfmDataProvider sfcVnfmDataProvider;

    /*
     * Factory method
     */
    public static SfcVnfmDataProvider GetVnfmDataProvider() {
        if (sfcVnfmDataProvider != null)
            return sfcVnfmDataProvider;
        else {
            sfcVnfmDataProvider = new SfcVnfmDataProvider();
            return sfcVnfmDataProvider;
        }
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        return;
    }

}
