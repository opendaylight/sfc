/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.*;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class is used to handle south-bound configuration generation for SFC verification.
 *
 * @author Xiao Liang, Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationAPI
 * @since 2015-08-26
 */
public class SfcVerificationAPI implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcVerificationAPI.class);
    private static final Map<Class<? extends SfcvAlgorithmIdentity>, Class<? extends SfcVerificationAPI>>
            algorithmClassMap =  new HashMap<>();

    static {
        algorithmClassMap.put(null, SfcVerificationAPI.class);
        registerAlgorithm(PolyAlg.class, SfcVerificationPolyAPI.class);
    }

    protected static void registerAlgorithm(Class<? extends SfcvAlgorithmIdentity> algorithm, Class<? extends SfcVerificationAPI> api) {
        if (algorithm != null && api != null) {
            algorithmClassMap.put(algorithm, api);
            LOG.info("Registered algorithm {} API {}", algorithm.getSimpleName(), api.getSimpleName());
        } else {
            LOG.error("Cannot register algorithm null");
        }
    }

    public static SfcVerificationAPI getAPI(Class<? extends SfcvAlgorithmIdentity> algorithm) {

        if (algorithm == null) {
            //Assume default PolyAlg.class
            algorithm = PolyAlg.class;
        }

        Class<? extends SfcVerificationAPI> algorithmApiClass = algorithmClassMap.get(algorithm);
        if (algorithmApiClass != null) {
            try {
                return algorithmApiClass.newInstance();
            } catch (Throwable e) {
                LOG.error("Error creating algorithm API");
            }
        }
        return null;
    }

    public static boolean checkLastIsVerifier(ServiceFunctionChain chain) {
        List<SfcServiceFunction> functions = chain.getSfcServiceFunction();
        //return functions.get(functions.size() - 1).getType().equals(Verification.class);
        return (functions.get(functions.size() - 1).getType().getValue().equals("verification"));
    }

    public static boolean checkServiceFunctionAlgorithm(ServiceFunction serviceFunction, Class<? extends SfcvAlgorithmIdentity> algorithm) {
        if (algorithm == null) {
            return true;
        }
        SfEntrySfcvAugmentation augmentation = serviceFunction.getAugmentation(SfEntrySfcvAugmentation.class);
        if (augmentation == null) {
            return false;
        }
        for (Class<? extends SfcvAlgorithmIdentity> alg : augmentation.getSfcvAlgorithms()) {
            if (alg.equals(algorithm)) {
                return true;
            }
        }
        return false;
    }

    public static List<ServiceFunction> filterServiceFunctionByAlgorithm(List<ServiceFunction> sfList, Class<? extends SfcvAlgorithmIdentity> algorithm) {
        List<ServiceFunction> filteredList = new ArrayList<>();
        for (ServiceFunction sf : sfList) {
            if (checkServiceFunctionAlgorithm(sf, algorithm)) {
                filteredList.add(sf);
            }
        }
        return filteredList;
    }

    public boolean initRefresh(String rspName, int sfSize, List<String> sfgNameList, int serviceIndex,
                               long sfcvStartIndex, long sfcvNumProfiles) {
        return true;
    }

    public boolean initRefresh(String rspName, List<SfName> sfNameList, List<String> sfgNameList, int serviceIndex,
                               long sfcvStartIndex, long sfcvNumProfiles) {
        return true;
    }

    public boolean initRenew(String rspName, int sfSize,  List<String> sfgNameList, int serviceIndex, long sfcvStartIndex, long sfcvNumProfiles) {
        return true;
    }
    public boolean initRenew(String rspName, List<SfName> sfNameList, List<String> sfgNameList, int serviceIndex, long sfcvStartIndex, long sfcvNumProfiles) {
        return true;
    }

    public boolean init(String rspName, int sfSize, List<String> sfgNameList, int serviceIndex, long sfcvNumProfiles, long sfcvRenewTtl) {
        return true;
    }
    public boolean init(String rspName, List<SfName> sfNameList, List<String> sfgNameList, int serviceIndex, long sfcvNumProfiles, long sfcvRenewTtl) {
        return true;
    }

    @Override
    public void close() { }

    public boolean setBuilderHopSecret(RenderedServicePathHopBuilder renderedServicePathHopBuilder, int serviceIndex,
                               short posIndex, SfName serviceFunctionName, ServiceFunction serviceFunction) {
        return false;
    }

    public void setSfcvParameters(RspSfcvAugmentationBuilder builder, NodeId refreshNode) {

    }

}
