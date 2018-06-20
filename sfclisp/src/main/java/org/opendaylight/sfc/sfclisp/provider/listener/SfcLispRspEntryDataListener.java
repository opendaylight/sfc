/*
 * Copyright (c) 2015, 2018 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfclisp.provider.listener;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sfclisp.provider.LispUpdater;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcLispRspEntryDataListener extends SfcLispAbstractDataListener<RenderedServicePath> {
    private static final Logger LOG = LoggerFactory.getLogger(SfcLispRspEntryDataListener.class);
    private final LispUpdater lispUpdater;

    public SfcLispRspEntryDataListener(DataBroker dataBroker, LispUpdater lispUpdater) {
        super(dataBroker, SfcInstanceIdentifiers.RSP_ENTRY_IID, LogicalDatastoreType.OPERATIONAL);
        this.lispUpdater = lispUpdater;

        LOG.info("Initialized RSP listener");
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<RenderedServicePath>> changes) {
        for (DataTreeModification<RenderedServicePath> change: changes) {
            DataObjectModification<RenderedServicePath> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    if (rootNode.getDataBefore() == null) {
                        RenderedServicePath renderedServicePath = rootNode.getDataAfter();
                        LOG.debug("\nCreated Rendered Service Path: {}", renderedServicePath.toString());
                        lispUpdater.registerPath(renderedServicePath);
                    } else {
                        RenderedServicePath newRsp = rootNode.getDataAfter();
                        RenderedServicePath oldRsp = rootNode.getDataBefore();
                        LOG.debug("\nUpdated Rendered Service Path \nnew: {} \noriginal: {}", newRsp, oldRsp);
                        lispUpdater.updatePath(newRsp, oldRsp);
                    }
                    break;
                case DELETE:
                    RenderedServicePath renderedServicePath = rootNode.getDataBefore();
                    LOG.debug("\nRemoved Rendered Service Path: {}", renderedServicePath);
                    lispUpdater.deletePath(renderedServicePath);
                    break;
                default:
                    break;
            }
        }
    }
}
