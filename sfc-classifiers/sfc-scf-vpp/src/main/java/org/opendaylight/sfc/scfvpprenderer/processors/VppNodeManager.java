/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfvpprenderer.processors;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;

public class VppNodeManager implements BindingAwareProvider {

    private MountPointService mountService = null;

    public VppNodeManager(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        // Register provider
        ProviderContext providerContext = bindingAwareBroker.registerProvider(this);
        onSessionInitiated(providerContext);
    }

    public MountPointService getMountPointService() {
        return this.mountService;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        mountService = session.getSALService(MountPointService.class);
        Preconditions.checkNotNull(mountService);
    }
}
