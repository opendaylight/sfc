/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.listener;

import java.util.Collection;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotNetconfIoam;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles RSP changes and triggers config send to the SB nodes.
 *
 * <p>
 *
 * @version 0.1
 */
public class SfcPotNetconfRSPListener extends SfcPotNetconfAbstractDataListener<RenderedServicePath> {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotNetconfRSPListener.class);

    private final SfcPotNetconfIoam sfcPotNetconfIoam;

    public SfcPotNetconfRSPListener(DataBroker dataBroker, SfcPotNetconfIoam sfcPotNetconfIoam) {
        setDataBroker(dataBroker);
        setInstanceIdentifier(SfcInstanceIdentifiers.RSP_ENTRY_IID);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        this.sfcPotNetconfIoam = sfcPotNetconfIoam;
        registerAsDataChangeListener();
    }

    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        /* RSP creation */
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath) {
                RenderedServicePath rsp = (RenderedServicePath) entry.getValue();
                LOG.debug("iOAM:PoT:SB:Created RSP: {}.Not handling iOAM configuration.", rsp.getName());
                /*
                 * As of now, it is not expected that PoT configurations will be
                 * configured as part of the RSP creation itself.
                 */
            }
        }

        /* RSP update */
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath && !dataCreatedObject.containsKey(entry.getKey())) {
                RenderedServicePath rsp = (RenderedServicePath) entry.getValue();
                LOG.debug("iOAM:PoT:SB:Updated RSP: {}", rsp.getName());
                sfcPotNetconfIoam.processRspUpdate(rsp);
            }
        }

        /* RSP deletion */
        for (InstanceIdentifier iid : change.getRemovedPaths()) {
            if (dataOriginalDataObject.get(iid) instanceof RenderedServicePath) {
                RenderedServicePath rsp = (RenderedServicePath) dataOriginalDataObject.get(iid);
                LOG.debug("iOAM:PoT:SB:Deleted RSP: {}", rsp.getName());
                sfcPotNetconfIoam.deleteRsp(rsp);
            }
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<RenderedServicePath>> changes) {
        for (DataTreeModification<RenderedServicePath> change: changes) {
            DataObjectModification<RenderedServicePath> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    // As of now, it is not expected that PoT configurations will be
                    // configured as part of the RSP creation itself.
                    RenderedServicePath updatedRsp = rootNode.getDataAfter();
                    if (rootNode.getDataBefore() != null) {
                        LOG.debug("iOAM:PoT:SB:Updated RSP: {}", updatedRsp.getName());
                        sfcPotNetconfIoam.processRspUpdate(updatedRsp);
                    } else {
                        LOG.debug("iOAM:PoT:SB:Created RSP: {}.Not handling iOAM configuration.", updatedRsp.getName());
                    }
                    break;
                case DELETE:
                    RenderedServicePath deletedRsp = rootNode.getDataBefore();
                    LOG.debug("iOAM:PoT:SB:Deleted RSP: {}", deletedRsp.getName());
                    sfcPotNetconfIoam.deleteRsp(deletedRsp);
                    break;
                default:
                    break;
            }
        }
    }
}
