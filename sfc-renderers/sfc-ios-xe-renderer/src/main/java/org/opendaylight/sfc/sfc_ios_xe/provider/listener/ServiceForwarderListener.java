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
import org.opendaylight.sfc.sfc_ios_xe.provider.renderer.IosXeServiceForwarderMapper;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class ServiceForwarderListener implements DataTreeChangeListener<ServiceFunctionForwarders> {

    private final ListenerRegistration iosXeSffListenerRegistration;
    private final IosXeServiceForwarderMapper sffManager;

    public ServiceForwarderListener(DataBroker dataBroker, IosXeServiceForwarderMapper sffManager) {
        this.sffManager = sffManager;
        // Register listener
        iosXeSffListenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(ServiceFunctionForwarders.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ServiceFunctionForwarders>> changes) {
        for (DataTreeModification<ServiceFunctionForwarders> modification : changes) {
            DataObjectModification<ServiceFunctionForwarders> rootNode = modification.getRootNode();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if (rootNode.getDataAfter() != null
                            && rootNode.getDataAfter().getServiceFunctionForwarder() != null) {
                        sffManager.syncForwarders(rootNode.getDataAfter().getServiceFunctionForwarder(), false);
                    }
                    break;
                case DELETE:
                    if (rootNode.getDataBefore() != null
                            && rootNode.getDataBefore().getServiceFunctionForwarder() != null) {
                        sffManager.syncForwarders(rootNode.getDataBefore().getServiceFunctionForwarder(), true);
                    }
                    break;
            }
        }
    }

    public ListenerRegistration getRegistrationObject() {
        return iosXeSffListenerRegistration;
    }
}
