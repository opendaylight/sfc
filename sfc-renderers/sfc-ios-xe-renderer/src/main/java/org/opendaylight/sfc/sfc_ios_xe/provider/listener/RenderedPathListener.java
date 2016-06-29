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
import org.opendaylight.sfc.sfc_ios_xe.provider.renderer.IosXeRspProcessor;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class RenderedPathListener implements DataTreeChangeListener<RenderedServicePaths> {

    private final IosXeRspProcessor rspProcessor;
    private final ListenerRegistration iosXeRspListenerRegistration;

    public RenderedPathListener(DataBroker dataBroker, IosXeRspProcessor rspProcessor) {
        this.rspProcessor = rspProcessor;
        // Register listener
        iosXeRspListenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(RenderedServicePaths.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<RenderedServicePaths>> changes) {
        for (DataTreeModification<RenderedServicePaths> modification : changes) {
            DataObjectModification<RenderedServicePaths> rootNode = modification.getRootNode();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if (rootNode.getDataAfter() != null && rootNode.getDataAfter().getRenderedServicePath() != null) {
                        rootNode.getDataAfter().getRenderedServicePath().forEach(rspProcessor::updateRsp);
                    }
                    break;
                case DELETE:
                    if (rootNode.getDataBefore() != null && rootNode.getDataBefore().getRenderedServicePath() != null) {
                        rootNode.getDataBefore().getRenderedServicePath().forEach(rspProcessor::deleteRsp);
                    }
                    break;
            }
        }
    }

    public ListenerRegistration getRegistrationObject() {
        return iosXeRspListenerRegistration;
    }
}
