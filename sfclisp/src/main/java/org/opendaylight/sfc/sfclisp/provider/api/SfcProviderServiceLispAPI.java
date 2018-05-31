/*
 * Copyright (c) 2014, 2017 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sfclisp.provider.api;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfclisp.provider.LispUpdater;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcProviderServiceLispAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceLispAPI.class);

    private final DataBroker dataProvider;
    private final LispUpdater lispUpdater;

    public SfcProviderServiceLispAPI(DataBroker dataProvider, LispUpdater lispUpdater) {
        this.dataProvider = dataProvider;
        this.lispUpdater = lispUpdater;
    }

    public void lispUpdateServiceFunction(ServiceFunction sf) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        }

        ServiceFunction updatedSf = lispUpdater.updateLispData(sf);

        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, updatedSf.key()).build();

        WriteTransaction writeTx = dataProvider.newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, sfEntryIID, updatedSf, true);
        writeTx.submit();

        if (LOG.isDebugEnabled()) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        }
    }

    public void lispUpdateServiceFunctionForwarder(ServiceFunctionForwarder sff) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        }

        ServiceFunctionForwarder updatedSff = lispUpdater.updateLispData(sff);

        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier
                .builder(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class, updatedSff.key())
                .build();

        WriteTransaction writeTx = dataProvider.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, sffEntryIID, updatedSff, true);
        writeTx.submit();

        if (LOG.isDebugEnabled()) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        }
    }
}
