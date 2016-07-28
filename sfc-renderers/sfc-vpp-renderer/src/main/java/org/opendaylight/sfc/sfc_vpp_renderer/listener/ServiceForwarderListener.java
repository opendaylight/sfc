/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_vpp_renderer.listener;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfc_vpp_renderer.renderer.VppSffManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceForwarderListener implements DataTreeChangeListener<ServiceFunctionForwarder> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceForwarderListener.class);
    private final ListenerRegistration vppSffListenerRegistration;
    private final VppSffManager sffManager;

    public ServiceForwarderListener(DataBroker dataBroker, VppSffManager sffManager) {
        this.sffManager = sffManager;
        // Register listener
        vppSffListenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ServiceFunctionForwarder>> changes) {
        for (DataTreeModification<ServiceFunctionForwarder> modification : changes) {
            DataObjectModification<ServiceFunctionForwarder> rootNode = modification.getRootNode();
            InstanceIdentifier<ServiceFunctionForwarder> key = modification.getRootPath().getRootIdentifier();
            LOG.info("SFF change event: key = {}, type = {}", key, rootNode.getModificationType());
            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if (rootNode.getDataAfter() != null) {
                        sffManager.disposeSff(rootNode.getDataAfter(), false);
                    }
                    break;
                case DELETE:
                    if (rootNode.getDataBefore() != null) {
                        sffManager.disposeSff(rootNode.getDataBefore(), true);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + rootNode.getModificationType());
            }
        }
    }

    public ListenerRegistration getRegistrationObject() {
        return vppSffListenerRegistration;
    }
}
