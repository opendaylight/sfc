/*
 * Copyright (c) 2015 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * This class is the DataListener for RSP changes.
 *
 * @author Florin Coras (fcoras@cisco.com)
 */

package org.opendaylight.sfc.sfc_lisp.provider.listener;

import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_lisp.provider.LispUpdater;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcLispRspEntryDataListener extends SfcLispAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcLispRspEntryDataListener.class);
    private LispUpdater lispUpdater;

    public SfcLispRspEntryDataListener(OpendaylightSfc odlSfc, LispUpdater lispUpdater) {
        this.lispUpdater = lispUpdater;
        setOpendaylightSfc(odlSfc);
        setDataBroker(odlSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.RSP_ENTRY_IID);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        registerAsDataChangeListener();
        LOG.info("Initialized RSP listener");
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        Map<InstanceIdentifier<?>, DataObject> originalDataObject = change.getOriginalData();
        // RSP CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath) {
                RenderedServicePath renderedServicePath = (RenderedServicePath) entry.getValue();
                LOG.debug("\nCreated Rendered Service Path: {}", renderedServicePath.toString());
                lispUpdater.registerPath(renderedServicePath);
            }
        }

        // RSP UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath && !dataCreatedObject.containsKey(entry.getKey())) {
                DataObject dataObject = originalDataObject.get(entry.getKey());
                RenderedServicePath newRsp = (RenderedServicePath) entry.getValue();
                RenderedServicePath oldRsp = (RenderedServicePath) dataObject;
                LOG.debug("\nUpdated Rendered Service Path \nnew: {} \noriginal: {}", newRsp, oldRsp);
                lispUpdater.updatePath(newRsp, oldRsp);
            }
        }

        // RSP DELETE
        Set<InstanceIdentifier<?>> dataRemovedPaths = change.getRemovedPaths();
        for (InstanceIdentifier<?> iid : dataRemovedPaths) {
            DataObject dataObject = originalDataObject.get(iid);
            if (dataObject instanceof RenderedServicePath) {
                RenderedServicePath renderedServicePath = (RenderedServicePath) dataObject;
                LOG.debug("\nRemoved Rendered Service Path: {}", renderedServicePath);
                lispUpdater.deletePath(renderedServicePath);
            }
        }

    }


}
