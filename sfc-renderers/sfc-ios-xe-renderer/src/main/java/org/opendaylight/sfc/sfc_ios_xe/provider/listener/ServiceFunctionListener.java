/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.listener;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfc_ios_xe.provider.renderer.IosXeServiceFunctionMapper;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class ServiceFunctionListener implements DataTreeChangeListener<ServiceFunctions> {

    private final IosXeServiceFunctionMapper sfManager;
    private final ListenerRegistration iosXeSfListenerRegistration;

    public ServiceFunctionListener(DataBroker dataBroker, IosXeServiceFunctionMapper sfManager) {
        this.sfManager = sfManager;
        // Register listener
        iosXeSfListenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(ServiceFunctions.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ServiceFunctions>> changes) {
        for (DataTreeModification<ServiceFunctions> modification : changes) {
            DataObjectModification<ServiceFunctions> rootNode = modification.getRootNode();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if (rootNode.getDataAfter() != null && rootNode.getDataAfter().getServiceFunction() != null) {
                        sfManager.syncFunctions(rootNode.getDataAfter().getServiceFunction(), false);
                    }
                    break;
                case DELETE:
                    if (rootNode.getDataBefore() != null && rootNode.getDataBefore().getServiceFunction() != null) {
                        sfManager.syncFunctions(rootNode.getDataBefore().getServiceFunction(), true);
                    }
                    break;
            }
        }
    }

    public ListenerRegistration getRegistrationObject() {
        return iosXeSfListenerRegistration;
    }

}
