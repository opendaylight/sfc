/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_lisp.provider.api;

import java.util.Arrays;
import java.util.concurrent.Callable;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.sfc_lisp.provider.SfcLispUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.db.instance.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class SfcLispDataStoreApi implements Callable<Object> {

    public enum Method {
        ADD_MAPPING
    }

    private static final Logger LOG = LoggerFactory.getLogger(SfcLispDataStoreApi.class);

    private Method methodToCall;
    private Object[] methodParameters;

    public SfcLispDataStoreApi(Method methodToCall, Object[] newMethodParameters) {
        this.methodToCall = methodToCall;
        if (newMethodParameters == null) {
            this.methodParameters = null;
        } else {
            this.methodParameters = Arrays.copyOf(newMethodParameters, newMethodParameters.length);
        }
    }

    @Override
    public Object call() throws Exception {
        Object result = null;

        switch (methodToCall) {
        case ADD_MAPPING:
            try {
                Mapping mapping = (Mapping) methodParameters[0];
                result = addLispMapping(mapping);
            } catch (ClassCastException e) {
                LOG.error("Cannot call addLispMapping, passed argument is not a Mapping object:{} ",
                        methodParameters[0]);
            }
            break;
        }

        return result;
    }

    private boolean addLispMapping(Mapping mapping) {
        Preconditions.checkNotNull(mapping, "Cannot ADD new Mapping to LISP configuration store, Mapping is null.");

        return SfcDataStoreAPI.writePutTransactionAPI(
                SfcLispUtil.buildMappingIid(mapping), mapping, LogicalDatastoreType.CONFIGURATION);
    }

}
