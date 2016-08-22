/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionGroupAPI;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.pot.provider.api.SfcPotAPI;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.BitMaskOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.TimeResolution;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.RspIoamPotAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.RspIoamPotAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.RspIoamPotHopAugmentation;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * This class is used to handle RPC for SFC Proof of Transit enabling, disable and to
 * handle RSP deletes.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.pot.provider.SfcPotRspProcessor
 * @since 2016-06-01
 */
public class SfcPotRspProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotRspProcessor.class);

    public SfcPotRspProcessor(DataBroker dataBroker) {
    }


    public static boolean enableSfcPot(RspName rspName,
                                       final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                                       Long           refreshPeriodValue,
                                       BitMaskOptions ioamPotProfileBitMask) {
        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        return (enableSfcPot(rsp, rspName, refreshPeriodTimeUnits, refreshPeriodValue, ioamPotProfileBitMask));
    }

    /*
     * NOTE: This method currently updates the RSP and writes it back.  Due to the
     * nature of the SFC PoT requiring configured and generated parameters
     * to be added to the RSP and to each hops of the component SFC, the RSP update
     * method is chosen.  In future versions, the RSP augmentations and SFCV data
     * can be moved out and stored separately if needed to avoid updating
     * the RSP itself.
     */
    public static boolean enableSfcPot(RenderedServicePath rsp,
                                       RspName rspName,
                                       final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                                       Long           refreshPeriodValue,
                                       BitMaskOptions ioamPotProfileBitMask) {

        ServiceFunctionPath sfp;
        List<RenderedServicePathHop> renderedServicePathHopList;
        List<RenderedServicePathHop> renderedServicePathHopListCopy = new ArrayList<>();
        SfcName serviceFunctionChainNameObj;
        ServiceFunctionChain serviceFunctionChain;
        int sfcSize;
        RenderedServicePathBuilder renderedServicePathBuilder;
        RspIoamPotAugmentationBuilder rspIoamPotAugmentationBuilder = new RspIoamPotAugmentationBuilder();
        int serviceIndex;
        int MAX_STARTING_INDEX = 255;
        int DEF_NUM_PROFILES = 2;
        int DEF_VALIDATOR_VAL = 1000;
        short posIndex = 0;
        long ioamPotNumProfiles = DEF_NUM_PROFILES;
        long ioamPotProfilesValidator = DEF_VALIDATOR_VAL;


        if (rsp == null) {
            LOG.error("enableSfcPot:Rendered service path by name:{} does not exist.", rspName);
            return false;
        }

        renderedServicePathBuilder = new RenderedServicePathBuilder(rsp);
        sfp = SfcProviderServicePathAPI.readServiceFunctionPath(rsp.getParentServiceFunctionPath());

        serviceFunctionChainNameObj = sfp.getServiceChainName();
        serviceFunctionChain = serviceFunctionChainNameObj != null ?
                SfcProviderServiceChainAPI.readServiceFunctionChain(serviceFunctionChainNameObj)
                : null;
        if (serviceFunctionChain == null) {
            LOG.error("enableSfcPot:ServiceFunctionChain name for Path {} is null.", sfp.getName());
            return false;
        }


        SfcPotAPI potAPI = SfcPotAPI.getAPI(null);
        if (potAPI == null) {
            LOG.error("enableSfcPot:Proof of transit algorithm not supported.");
            return false;
        }

        rspIoamPotAugmentationBuilder.setIoamPotAlgorithm(SfcPotAPI.getDefaultIoamPotAlgorithm());

        // Descending order
        serviceIndex = MAX_STARTING_INDEX;

        //Count the number of service functions in the SFC.  That is all we currently need.
        sfcSize = serviceFunctionChain.getSfcServiceFunction().size();

        List<String> sfgNameList = SfcProviderServiceFunctionGroupAPI.getSfgNameList(serviceFunctionChain);
        if((sfcSize == 0) && (sfgNameList == null)) {
            LOG.error("enableSfcPot:Service Function chain returned empty list");
            return false;
        }

        //NOTE:Currently assuming last node in the service chain to be 'verifier'/decap node
        if (!potAPI.init(rspName.getValue(), sfcSize, sfgNameList, serviceIndex,
                         refreshPeriodTimeUnits, refreshPeriodValue,
                         ioamPotProfileBitMask)) {
            LOG.error("enableSfcPot: Could not initialize SfcPotAPI");
            return false;
        }

        LOG.debug("enableSfcPot: Initialized with num:{}, valid:{}", ioamPotNumProfiles, ioamPotProfilesValidator);

        renderedServicePathHopList = rsp.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            RenderedServicePathHopBuilder renderedServicePathHopBuilder =
                                          new RenderedServicePathHopBuilder(renderedServicePathHop);

            renderedServicePathHopBuilder.removeAugmentation(RspIoamPotHopAugmentation.class);

            /* add a fresh sfcv hop augmentation with asked for profiles only */
            potAPI.setBuilderHopSecret(renderedServicePathHopBuilder, serviceIndex, posIndex, null, null);

            renderedServicePathHopListCopy.add(posIndex, renderedServicePathHopBuilder.build());

            serviceIndex--;
            posIndex++;
        }

        /* Now that, we have modified the array list augmentations...set it back */
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopListCopy);

        /* remove the existing augmentation */
        renderedServicePathBuilder.removeAugmentation(RspIoamPotAugmentation.class);

        potAPI.setIoamPotParameters(rspIoamPotAugmentationBuilder);

        renderedServicePathBuilder.addAugmentation(RspIoamPotAugmentation.class, rspIoamPotAugmentationBuilder.build());

        potAPI.close();

        /* Re-create the RSP with the fresh details and store back to the datastore.  Since the same key is used
         * it should be an update to the RSP.
         */
        RenderedServicePathKey renderedServicePathKey = new
                RenderedServicePathKey(renderedServicePathBuilder.getName());
        InstanceIdentifier<RenderedServicePath> rspIID;
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey)
                .build();

        RenderedServicePath renderedServicePath =
                renderedServicePathBuilder.build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath,
                                                     LogicalDatastoreType.OPERATIONAL)) {
            LOG.debug("enableSfcPot: Updated RSP: {}", sfp.getName());
        } else {
            LOG.error("{}: enableSfcPot: Failed to update Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], sfp.getName());
        }

        return true;
    }

    public static boolean disableSfcPot(RspName rspName) {
        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        return (disableSfcPot(rsp, rspName));
    }

    /*
     * This method currently updates the RSP by removing all the SFC verification
     * related augmentations and writes it back.
     */
    public static boolean disableSfcPot(RenderedServicePath rsp, RspName rspName) {
        ServiceFunctionPath sfp;
        List<RenderedServicePathHop> renderedServicePathHopList;
        List<RenderedServicePathHop> renderedServicePathHopListCopy = new ArrayList<>();
        SfcName serviceFunctionChainNameObj;
        RenderedServicePathBuilder renderedServicePathBuilder;
        ServiceFunctionChain serviceFunctionChain;
        short posIndex = 0;

        if (rsp == null) {
            LOG.error("Rendered service path by name:{} does not exist.", rspName);
            return false;
        }

        renderedServicePathBuilder = new RenderedServicePathBuilder(rsp);
        sfp = SfcProviderServicePathAPI.readServiceFunctionPath(rsp.getParentServiceFunctionPath());

        serviceFunctionChainNameObj = sfp.getServiceChainName();
        serviceFunctionChain = serviceFunctionChainNameObj != null ?
                SfcProviderServiceChainAPI.readServiceFunctionChain(serviceFunctionChainNameObj)
                : null;
        if (serviceFunctionChain == null) {
            LOG.error("disableSfcPot:ServiceFunctionChain name for Path {} is null.", sfp.getName());
            return false;
        }

        renderedServicePathHopList = rsp.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            RenderedServicePathHopBuilder renderedServicePathHopBuilder =
                                          new RenderedServicePathHopBuilder(renderedServicePathHop);

            renderedServicePathHopBuilder.removeAugmentation(RspIoamPotHopAugmentation.class);

            renderedServicePathHopListCopy.add(posIndex, renderedServicePathHopBuilder.build());

            posIndex++;
        }

        /* Removed the array list augmentations...set it back */
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopListCopy);

        /* remove the existing augmentation */
        renderedServicePathBuilder.removeAugmentation(RspIoamPotAugmentation.class);

        /* Re-create the RSP with the fresh details and store back to the datastore.  Since the same key is used
         * it should be an update to the RSP.
         */
        RenderedServicePathKey renderedServicePathKey = new
                RenderedServicePathKey(renderedServicePathBuilder.getName());
        InstanceIdentifier<RenderedServicePath> rspIID;
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePathKey)
                .build();

        RenderedServicePath renderedServicePath =
                renderedServicePathBuilder.build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath,
                                                     LogicalDatastoreType.OPERATIONAL)) {
            LOG.debug("disableSfcPot: Updated RSP: {}", sfp.getName());
        } else {
            LOG.error("{}: disableSfcPot: Failed to update Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], sfp.getName());
        }

        return true;
    }
}
