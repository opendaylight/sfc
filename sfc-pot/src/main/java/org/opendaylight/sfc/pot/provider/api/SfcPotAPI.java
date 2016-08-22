/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.BitMaskOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.PolyAlg;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.IoamPotAlgorithmIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.RspIoamPotAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.TimeResolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class is used to handle PoT configuration for SFC verification.
 *
 * @author Xiao Liang, Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.pot.provider.api.SfcPotAPI
 * @since 2015-08-26
 */
public class SfcPotAPI implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotAPI.class);
    private static final Map<Class<? extends IoamPotAlgorithmIdentity>, Class<? extends SfcPotAPI>>
            algorithmClassMap =  new HashMap<>();

    static {
        algorithmClassMap.put(null, SfcPotAPI.class);
        registerAlgorithm(PolyAlg.class, SfcPotPolyAPI.class);
    }

    protected static void registerAlgorithm(Class<? extends IoamPotAlgorithmIdentity> algorithm,
                                            Class<? extends SfcPotAPI> api) {
        if (algorithm != null && api != null) {
            algorithmClassMap.put(algorithm, api);
            LOG.info("Registered algorithm {} API {}", algorithm.getSimpleName(), api.getSimpleName());
        } else {
            LOG.error("Cannot register algorithm null");
        }
    }

    public static SfcPotAPI getAPI(Class<? extends IoamPotAlgorithmIdentity> algorithm) {

        if (algorithm == null) {
            //Assume default PolyAlg.class
            algorithm = PolyAlg.class;
        }

        Class<? extends SfcPotAPI> algorithmApiClass = algorithmClassMap.get(algorithm);
        if (algorithmApiClass != null) {
            try {
                return algorithmApiClass.newInstance();
            } catch (Throwable e) {
                LOG.error("Error creating algorithm API for {}", algorithm, e);
            }
        }
        return null;
    }

    public static Class<? extends IoamPotAlgorithmIdentity> getDefaultIoamPotAlgorithm () {
        return PolyAlg.class;
    }

    public boolean init(String rspName, int sfSize, List<String> sfgNameList, int serviceIndex,
                        final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                        Long           refreshPeriodValue,
                        BitMaskOptions ioamPotProfileBitMask) {
        return true;
    }
    public boolean init(String rspName, List<SfName> sfNameList, List<String> sfgNameList, int serviceIndex,
                        final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                        Long           refreshPeriodValue,
                        BitMaskOptions ioamPotProfileBitMask) {
        return true;
    }

    public int initRenew(String rspName, int sfSize, List<String> sfgNameList,
                             int serviceIndex, int currActiveIndex) {
        return 0;

    }

    @Override
    public void close() { }

    public boolean setBuilderHopSecret(RenderedServicePathHopBuilder renderedServicePathHopBuilder, int serviceIndex,
                               short posIndex, SfName serviceFunctionName, ServiceFunction serviceFunction) {
        return false;
    }

    public void setIoamPotParameters(RspIoamPotAugmentationBuilder builder, int currActiveIndex) {

    }

}
