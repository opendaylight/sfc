/*
 * Copyright (c) 2014, 2017 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfclisp.provider.listener;

import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sfclisp.provider.LispUpdater;
import org.opendaylight.sfc.sfclisp.provider.api.SfcProviderServiceLispAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the DataListener for SFF changes.
 *
 * <p>
 *
 * @author David Goldberg (david.goldberg@contextream.com)
 * @author Florin Coras (fcoras@cisco.com)
 * @version 0.1
 * @since 2014-06-30
 */
public class SfcLispSffDataListener extends SfcLispAbstractDataListener<ServiceFunctionForwarders> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcLispSffDataListener.class);
    private final LispUpdater lispUpdater;

    public SfcLispSffDataListener(LispUpdater lispUpdater) {
        setInstanceIdentifier(SfcInstanceIdentifiers.SFF_IID);
        setDataStoreType(LogicalDatastoreType.CONFIGURATION);
        this.lispUpdater = lispUpdater;
    }

    public void setDataProvider(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        registerAsDataChangeListener();
        LOG.info("Initialized SFF listener");
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ServiceFunctionForwarders>> changes) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        }

        for (DataTreeModification<ServiceFunctionForwarders> change: changes) {
            DataObjectModification<ServiceFunctionForwarders> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    // SF CREATION
                    if (rootNode.getDataBefore() == null) {
                        ServiceFunctionForwarders updatedServiceFunctionForwarders = rootNode.getDataAfter();
                        List<ServiceFunctionForwarder> serviceFunctionForwarderList = updatedServiceFunctionForwarders
                                .getServiceFunctionForwarder();
                        for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList) {
                            if (lispUpdater.containsLispAddress(serviceFunctionForwarder)) {
                                SfcProviderServiceLispAPI.lispUpdateServiceFunctionForwarder(serviceFunctionForwarder);
                            }
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
