/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.BitMaskOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.RspIoamPotAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.RspIoamPotAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.TimeResolution;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to handle RPC for SFC Proof of Transit enable and disable.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @since 2016-06-01
 * @link https://github.com/CiscoDevNet/iOAM
 */
public class SfcPotRspProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotRspProcessor.class);

    public SfcPotRspProcessor() {
    }


    /**
     * This utility method adds augmentations to the RSP with a record to enable iOAM
     * Proof of Transit and related parameters.
     * <p>
     *
     * @param rsp Rendered service path (SFC) to enable trace on.
     * @param refreshPeriodTimeUnits iOAM PoT configuration refresh period time units.
     * @param refreshPeriodValue iOAM PoT configuration refresh period.
     * @param ioamPotProfileBitMask iOAM internal configuration parameter.
     * @param ioamPotNumProfiles iOAM number of PoT profiles per node.
     * @return success or failure.
     */
    private static boolean processSfcPot(RenderedServicePath rsp,
                                     final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                                     Long           refreshPeriodValue,
                                     BitMaskOptions ioamPotProfileBitMask,
                                     Long           ioamPotNumProfiles) {
        ServiceFunctionPath sfp;
        SfcName serviceFunctionChainNameObj;
        ServiceFunctionChain serviceFunctionChain;
        RenderedServicePathBuilder renderedServicePathBuilder;

        RspIoamPotAugmentationBuilder rspIoamPotAugmentationBuilder = new RspIoamPotAugmentationBuilder();
        RspName rspName = rsp.getName();

        renderedServicePathBuilder = new RenderedServicePathBuilder(rsp);
        sfp = SfcProviderServicePathAPI.readServiceFunctionPath(rsp.getParentServiceFunctionPath());

        serviceFunctionChainNameObj = sfp.getServiceChainName();
        serviceFunctionChain = serviceFunctionChainNameObj != null ?
                SfcProviderServiceChainAPI.readServiceFunctionChain(serviceFunctionChainNameObj)
                : null;
        if (serviceFunctionChain == null) {
            LOG.error("iOAM:PoT:ServiceFunctionChain name for Path {} is invalid.", sfp.getName());
            return false;
        }

        /* Set the iOAM PoT parameters */
        rspIoamPotAugmentationBuilder.setIoamPotEnable(true);
        rspIoamPotAugmentationBuilder.setRefreshPeriodTimeUnits(refreshPeriodTimeUnits);
        rspIoamPotAugmentationBuilder.setRefreshPeriodValue(refreshPeriodValue);
        rspIoamPotAugmentationBuilder.setIoamPotBitMask(ioamPotProfileBitMask);
        rspIoamPotAugmentationBuilder.setIoamPotNumProfiles(ioamPotNumProfiles);

        /* remove the existing augmentation, if any */
        renderedServicePathBuilder.removeAugmentation(RspIoamPotAugmentation.class);

        /* add augmentation */
        renderedServicePathBuilder.addAugmentation(RspIoamPotAugmentation.class,
                                                   rspIoamPotAugmentationBuilder.build());

        /* Re-create the RSP with the fresh details and store back to the datastore.
         */
        RenderedServicePathKey renderedServicePathKey = new
                RenderedServicePathKey(renderedServicePathBuilder.getName());
        InstanceIdentifier<RenderedServicePath> rspIID;
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey)
                .build();

        RenderedServicePath renderedServicePath =
                renderedServicePathBuilder.build();

        /* Write to datastore */
        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath,
                                                     LogicalDatastoreType.OPERATIONAL)) {
            LOG.debug("iOAM:PoT:Enable:Updated RSP: {}", sfp.getName());
        } else {
            LOG.error("iOAM:PoT:Enable:{}:Failed to update Rendered Service Path:{}",
                    Thread.currentThread().getStackTrace()[1], sfp.getName());
            return false;
        }

        return true;
    }

    public static boolean enableSfcPot(RspName rspName,
                                       final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                                       Long           refreshPeriodValue,
                                       BitMaskOptions ioamPotProfileBitMask,
                                       Long           ioamPotNumProfiles) {

        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (rsp == null) {
            LOG.error("iOAM:PoT:Enable:Rendered service path by name:{} does not exist.", rspName);
            return false;
        }

        return (enableSfcPot(rsp, rspName, refreshPeriodTimeUnits,
                             refreshPeriodValue, ioamPotProfileBitMask,
                             ioamPotNumProfiles));
    }

    public static boolean enableSfcPot(RenderedServicePath rsp,
                                       RspName rspName,
                                       final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                                       Long           refreshPeriodValue,
                                       BitMaskOptions ioamPotProfileBitMask,
                                       Long           ioamPotNumProfiles) {
        return (processSfcPot(rsp, refreshPeriodTimeUnits, refreshPeriodValue,
                              ioamPotProfileBitMask, ioamPotNumProfiles));
    }


    public static boolean disableSfcPot(RspName rspName) {
        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (rsp == null) {
            LOG.error("iOAM:PoT:Disable:Rendered service path by name:{} does not exist.", rspName);
            return false;
        }

        return (disableSfcPot(rsp, rspName));
    }

    /**
     * This method updates iOAM augmentations to the RSP to disable iOAM
     * Proof of Transit feature.
     * <p>
     *
     * @param rsp Rendered service path (SFC) to disable trace on.
     * @return success or failure.
     */
    public static boolean disableSfcPot(RenderedServicePath rsp, RspName rspName) {
        ServiceFunctionPath sfp;
        SfcName serviceFunctionChainNameObj;
        RenderedServicePathBuilder renderedServicePathBuilder;
        RspIoamPotAugmentationBuilder rspIoamPotAugmentationBuilder = new RspIoamPotAugmentationBuilder();
        ServiceFunctionChain serviceFunctionChain;

        renderedServicePathBuilder = new RenderedServicePathBuilder(rsp);
        sfp = SfcProviderServicePathAPI.readServiceFunctionPath(rsp.getParentServiceFunctionPath());

        serviceFunctionChainNameObj = sfp.getServiceChainName();
        serviceFunctionChain = serviceFunctionChainNameObj != null ?
                SfcProviderServiceChainAPI.readServiceFunctionChain(serviceFunctionChainNameObj)
                : null;
        if (serviceFunctionChain == null) {
            LOG.error("iOAM:PoT:Disable:ServiceFunctionChain name for Path {} is null.", sfp.getName());
            return false;
        }

        /* update the iOAM PoT record to disable the PoT.
         * Note: Not deleting the record, as some parameters might be needed SB for handling disable.
         */
        rspIoamPotAugmentationBuilder.setIoamPotEnable(false);

        /* update the augmentation */
        renderedServicePathBuilder.removeAugmentation(RspIoamPotAugmentation.class);
        renderedServicePathBuilder.addAugmentation(RspIoamPotAugmentation.class, rspIoamPotAugmentationBuilder.build());

        /* Re-create the RSP with the fresh details and store back to the datastore.
         */
        RenderedServicePathKey renderedServicePathKey = new
                RenderedServicePathKey(renderedServicePathBuilder.getName());
        InstanceIdentifier<RenderedServicePath> rspIID;
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey)
                .build();

        RenderedServicePath renderedServicePath =
                renderedServicePathBuilder.build();

        /* Write to datastore */
        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath,
                                                     LogicalDatastoreType.OPERATIONAL)) {
            LOG.debug("iOAM:PoT:Disable:Updated RSP: {}", sfp.getName());
        } else {
            LOG.error("iOAM:PoT:Disable:{}:Failed to update Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], sfp.getName());
            return false;
        }

        return true;
    }
}
