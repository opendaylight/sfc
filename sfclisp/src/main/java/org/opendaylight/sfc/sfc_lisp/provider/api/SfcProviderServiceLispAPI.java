/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sfc_lisp.provider.api;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_lisp.provider.LispUpdater;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcProviderServiceLispAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceLispAPI.class);

    public static void lispUpdateServiceFunction(ServiceFunction sf) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (OpendaylightSfc.getOpendaylightSfcObj().getDataProvider() != null) {
            sf = LispUpdater.getLispUpdaterObj().updateLispData(sf);

            InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
                    .child(ServiceFunction.class, sf.getKey()).build();

            WriteTransaction writeTx = OpendaylightSfc.getOpendaylightSfcObj().getDataProvider().newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.CONFIGURATION, sfEntryIID, sf, true);
            writeTx.submit();

        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    public static void lispUpdateServiceFunctionForwarder(ServiceFunctionForwarder sff) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (OpendaylightSfc.getOpendaylightSfcObj().getDataProvider() != null) {

            sff = LispUpdater.getLispUpdaterObj().updateLispData(sff);

            InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class, sff.getKey()).build();

            WriteTransaction writeTx = OpendaylightSfc.getOpendaylightSfcObj().getDataProvider().newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, sffEntryIID, sff, true);
            writeTx.submit();

        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

}
