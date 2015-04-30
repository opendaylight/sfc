/*
 * Copyright (c) 2015 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * This class is a Callable API to LispFlowMapping
 *
 * @author Florin Coras (fcoras@cisco.com)
 *
 */

package org.opendaylight.sfc.sfc_lisp.provider.api;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.opendaylight.sfc.sfc_lisp.provider.LispUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.LispAddressContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.GetMappingOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.LfmMappingDatabaseService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class SfcLispFlowMappingApi implements Callable<Object> {

    public enum Method {
        GET_MAPPING,
        ADD_MAPPING,
        DELETE_MAPPING
    }

    private static final Logger LOG = LoggerFactory.getLogger(SfcLispFlowMappingApi.class);

    private LfmMappingDatabaseService lfmService;
    private Method methodToCall;
    private Object[] methodParameters;

    public SfcLispFlowMappingApi(LfmMappingDatabaseService lfmService, Method methodToCall, Object[] methodParameters) {
        this.lfmService = lfmService;
        this.methodToCall = methodToCall;
        this.methodParameters = methodParameters;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object call() throws Exception {
        Object result = null;

        switch (methodToCall) {
        case GET_MAPPING:
            try {
                LispAddressContainer eid = (LispAddressContainer) methodParameters[0];
                result = getLispMapping(eid);
            } catch (ClassCastException e) {
                LOG.error("Cannot call getLispMapping, passed argument is not an IpAddress object:{} ",
                        methodParameters[0]);
            }
            break;
        case ADD_MAPPING:
            try {
                LispAddressContainer eid = (LispAddressContainer) methodParameters[0];
                List<LispAddressContainer> locators = (List<LispAddressContainer>) methodParameters[1];
                result = addLispMapping(eid, locators);
            } catch (ClassCastException e) {
                LOG.error("Cannot call addLispMapping, passed argument is not a Mapping object:{} ",
                        methodParameters[0]);
            }
            break;
        case DELETE_MAPPING:
            try {
                LispAddressContainer eid = (LispAddressContainer) methodParameters[0];
                result = removeLispMapping(eid);
            } catch (ClassCastException e) {
                LOG.error("Cannot call deleteLispMapping, passed argument is not a Mapping object:{} ",
                        methodParameters[0]);
            }
            break;
        }


        return result;
    }

    private Object getLispMapping(LispAddressContainer eid) {
        Preconditions.checkNotNull(eid, "Cannot GET Mapping from LispFlowMapping, Mapping is null.");
        try {
            Future<RpcResult<GetMappingOutput>> result = lfmService.getMapping(LispUtil.buildGetMappingInput(eid));
            GetMappingOutput output = result.get().getResult();

            return (Object) output.getEidToLocatorRecord();
        } catch (Exception e) {
            LOG.warn("Failed to GET mapping for EID {}", eid);
        }
        return null;
    }

    private boolean addLispMapping(LispAddressContainer eid, List<LispAddressContainer> locators) {
        Preconditions.checkNotNull(eid, "Cannot ADD new Mapping to LISP configuration store, Eid is null.");
        Preconditions.checkNotNull(locators, "Cannot ADD new Mapping to LISP configuration store, Locators is null.");
        try {
            Future<RpcResult<Void>> result = lfmService.addMapping(LispUtil.buildAddMappingInput(eid, locators));
            result.get().getResult();
            return true;
        } catch (Exception e) {
            LOG.warn("Failed to ADD mapping for EID {}", eid);
        }
        return false;
    }

    private boolean removeLispMapping(LispAddressContainer eid) {
        Preconditions.checkNotNull(eid, "Cannot REMOVE new Mapping to LISP configuration store, Eid is null.");
        try {
            Future<RpcResult<Void>> result = lfmService.removeMapping(LispUtil.buildRemoveMappingInput(eid));
            result.get().getResult();
            return true;
        } catch (Exception e) {
            LOG.warn("Failed to REMOVE mapping for EID {}", eid);
        }
        return false;
    }
}
