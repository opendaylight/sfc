/*
 * Copyright (c) 2014, 2017 Contextream, Inc. and others.  All rights reserved.
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
import org.opendaylight.sfc.sfclisp.provider.api.SfcProviderServiceLispAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class gets called whenever there is a change to a Service Function list
 * entry, i.e., added.
 *
 * <p>
 *
 * @author David Goldberg (david.goldberg@contextream.com)
 * @author Florin Coras (fcoras@cisco.com)
 * @version 0.1
 * @since 2014-06-30
 */
public class SfcLispSfEntryDataListener extends SfcLispAbstractDataListener<ServiceFunction> {
    private static final Logger LOG = LoggerFactory.getLogger(SfcLispSfEntryDataListener.class);
    private final LispUpdater lispUpdater;

    public SfcLispSfEntryDataListener(LispUpdater lispUpdater) {
        setInstanceIdentifier(SfcInstanceIdentifiers.SF_ENTRY_IID);
        setDataStoreType(LogicalDatastoreType.CONFIGURATION);
        this.lispUpdater = lispUpdater;
    }

    public void setDataProvider(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        registerAsDataChangeListener();
        LOG.info("Initialized SF listener");
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ServiceFunction>> changes) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        }

        for (DataTreeModification<ServiceFunction> change: changes) {
            DataObjectModification<ServiceFunction> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    // SF CREATION
                    if (rootNode.getDataBefore() == null) {
                        ServiceFunction createdServiceFunction = rootNode.getDataAfter();

                        if (lispUpdater.containsLispAddress(createdServiceFunction)) {
                            SfcProviderServiceLispAPI.lispUpdateServiceFunction(createdServiceFunction);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        }
    }
}
