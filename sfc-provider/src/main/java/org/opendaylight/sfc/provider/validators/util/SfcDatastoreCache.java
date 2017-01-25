/*
 * Copyright (c) 2017 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.validators.util;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * SFC caching layer for datastore access. Used for allowing creation-time validation
 * of SF paths (during which the write transaction is kept open) to be performed
 * without datastore accesses in most cases
 * @author Diego Granados (diego.jesus.granados.lopez@ericsson.com)
 */
public class SfcDatastoreCache {

    /**
     * This cache stores the relationship between SFs and SF types
     */
    private static LoadingCache<SfName, String> sfToSfTypeCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .build(new CacheLoader<SfName, String>() {
                @Override
                public String load(SfName key) {
                    ServiceFunction sf = SfcProviderServiceFunctionAPI.readServiceFunction(key);
                    if (sf == null) {
                        return null;
                    }
                    return sf.getType().getValue().toString();
                }
            });

    /**
     * This cache holds the relation between SF chains and the list of SF types for the chain
     */
    private static LoadingCache<SfcName, List<String>> sfChainToSfTypeList = CacheBuilder.newBuilder()
            .maximumSize(500)
            .build(new CacheLoader<SfcName, List<String>>() {
                @Override
                public List<String> load(SfcName key) {
                    ServiceFunctionChain serviceFunctionChain = SfcProviderServiceChainAPI.readServiceFunctionChain(key);
                    if (serviceFunctionChain == null) {
                        return null;
                    }
                    List<String> serviceFunctionTypesForChain = new ArrayList<String>();
                    //SfcServiceFunction sfcSf = null;
                    for (SfcServiceFunction sfcSf: serviceFunctionChain.getSfcServiceFunction()) {
                        serviceFunctionTypesForChain.add(sfcSf.getType().getValue());
                    }
                    return serviceFunctionTypesForChain;
                }
            });

    /**
     * @return the cache holding the association between SF names and SF types
     */
    public static LoadingCache<SfName, String> getSfToSfTypeCache() {
        return sfToSfTypeCache;
    }

    /**
     * @return the cache holding the association between service function chains
     * and SF types in that chain
     */
    public static LoadingCache<SfcName, List<String>> getSfChainToSfTypeList() {
        return sfChainToSfTypeList;
    }
}
