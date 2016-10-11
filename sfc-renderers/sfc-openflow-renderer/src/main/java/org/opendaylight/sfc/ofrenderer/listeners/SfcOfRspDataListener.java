/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.listeners;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.ofrenderer.processors.SfcOfRspProcessor;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class has will be notified when changes are mad to Rendered Service Paths.
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @version 0.1
 * <p>
 * @since 2015-01-27
 */
public class SfcOfRspDataListener implements DataTreeChangeListener<RenderedServicePath>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRspDataListener.class);
    private final SfcOfRspProcessor sfcOfRspProcessor;
    private final ListenerRegistration rspListenerRegistration;

    public SfcOfRspDataListener(DataBroker dataBroker, SfcOfRspProcessor sfcOfRspProcessor) {
        rspListenerRegistration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, SfcInstanceIdentifiers.RSP_ENTRY_IID),
                this);
        this.sfcOfRspProcessor = sfcOfRspProcessor;
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<RenderedServicePath>> collection) {
        for (DataTreeModification<RenderedServicePath> modification : collection) {
            DataObjectModification<RenderedServicePath> rootNode = modification.getRootNode();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if (rootNode.getDataBefore() == null && rootNode.getDataAfter() != null) {
                        LOG.info("SfcOfRspDataListener.onDataTreeChanged create RSP {}", rootNode.getDataBefore());
                        sfcOfRspProcessor.processRenderedServicePath(rootNode.getDataAfter());
                    } else if (rootNode.getDataAfter().equals(rootNode.getDataBefore())) {
                        LOG.info("SfcOfRspDataListener.onDataTreeChanged update RSP Before:{} After:{}",
                                rootNode.getDataAfter(),
                                rootNode.getDataBefore());
                        // This clause supports re-rendering of unmodified RSPs
                        sfcOfRspProcessor.deleteRenderedServicePath(rootNode.getDataBefore());
                        sfcOfRspProcessor.processRenderedServicePath(rootNode.getDataAfter());
                    }
                    break;
                case DELETE:
                    if (rootNode.getDataBefore() != null) {
                        LOG.info("SfcOfRspDataListener.onDataTreeChanged delete RSP {}", rootNode.getDataBefore());
                        sfcOfRspProcessor.deleteRenderedServicePath(rootNode.getDataBefore());
                    }
                    break;
            }
        }
    }

    @Override
    public void close() throws Exception {
        rspListenerRegistration.close();
    }
}
