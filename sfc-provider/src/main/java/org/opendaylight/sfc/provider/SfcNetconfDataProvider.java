/*
* Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.sfc.provider;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;

public class SfcNetconfDataProvider implements Provider {

    DOMMountPointService mountService;
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

    public DOMMountPointService getMountService() {
        return mountService;
    }

    public void setMountService(DOMMountPointService mountService) {
        this.mountService = mountService;
    }

    @Override
    public Collection<Provider.ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession session) {
        mountService = session.getService(DOMMountPointService.class);
    }
}
