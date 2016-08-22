/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider;

import io.netty.util.Timeout;

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
import java.util.concurrent.TimeUnit;

/**
 * This class is used to handle RPC for SFC Proof of Transit enabling, disable and to
 * handle RSP deletes.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @since 2016-06-01
 */
public class SfcPotRspProcessor {

    public static final long DEF_CFG_REFRESH_INTERVAL_MS = 5000;

    /* NOTE: The timerwheel by default triggers only after expiry and
     * and also runs at 100ms and so, advance the expiry time by 250ms.
     * To apply this, there is a min. timeout value of 500ms check.
     * This should be removed in future and appropriate fuzz added.
     */
    public static final long MIN_CFG_REFRESH_INTERVAL_MS  =  500;
    public static final long CFG_REFRESH_INTERVAL_FUZZ_MS =  250;

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotRspProcessor.class);

    public SfcPotRspProcessor(DataBroker dataBroker) {
    }

    /* utility function to process common aspects of enable and refresh */
    private static int processSfcPot(RenderedServicePath rsp,
                                     final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                                     Long refreshValue,
                                     BitMaskOptions ioamPotProfileBitMask,
                                     int currActiveIndex,
                                     boolean flagRefresh) {

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
        short posIndex = 0;
        int newActiveIndex = -1;
        Long refreshPeriodValue = DEF_CFG_REFRESH_INTERVAL_MS;
        RspName rspName = rsp.getName();

        SfcPotTimerData potTimerData = SfcPotTimerData.getInstance();

        if (flagRefresh && (potTimerData.isRspDataPresent(rspName))) {
            LOG.error("enableSfcPot:{}:PoT information updates not yet supported...", rspName);
            return -1;
        }

        if (flagRefresh && (refreshValue < MIN_CFG_REFRESH_INTERVAL_MS)) {
            LOG.error("enableSfcPot:{}:Unsupported refreshValue: minimum:{}...",
                      rspName, MIN_CFG_REFRESH_INTERVAL_MS);
            return -1;
        }

        /* NOTE: Adjust by fuzz for timerwheel expiry after-the-fact issue. First time only */
        if (flagRefresh) {
            refreshPeriodValue = refreshValue - CFG_REFRESH_INTERVAL_FUZZ_MS;
        }

        renderedServicePathBuilder = new RenderedServicePathBuilder(rsp);
        sfp = SfcProviderServicePathAPI.readServiceFunctionPath(rsp.getParentServiceFunctionPath());

        serviceFunctionChainNameObj = sfp.getServiceChainName();
        serviceFunctionChain = serviceFunctionChainNameObj != null ?
                SfcProviderServiceChainAPI.readServiceFunctionChain(serviceFunctionChainNameObj)
                : null;
        if (serviceFunctionChain == null) {
            LOG.error("ServiceFunctionChain name for Path {} is null.", sfp.getName());
            return newActiveIndex;
        }

        SfcPotAPI potAPI = SfcPotAPI.getAPI(null);
        if (potAPI == null) {
            LOG.error("Proof of transit algorithm not supported.");
            return newActiveIndex;
        }

        rspIoamPotAugmentationBuilder.setIoamPotAlgorithm(SfcPotAPI.getDefaultIoamPotAlgorithm());

        // Descending order
        serviceIndex = MAX_STARTING_INDEX;

        //Count the number of service functions in the SFC.  That is all we currently need.
        sfcSize = serviceFunctionChain.getSfcServiceFunction().size();

        List<String> sfgNameList = SfcProviderServiceFunctionGroupAPI.getSfgNameList(serviceFunctionChain);
        if((sfcSize == 0) && (sfgNameList == null)) {
            LOG.error("Service Function chain returned empty list");
            return -1;
        }

        if (flagRefresh) {
           /* First-time config generation */
           if (!potAPI.init(rspName.getValue(), sfcSize, sfgNameList, serviceIndex,
                         refreshPeriodTimeUnits, refreshPeriodValue,
                         ioamPotProfileBitMask)) {
                LOG.error("enableSfcPot: Could not initialize SfcPotAPI");
                return -1;
            }
        } else {
            /* Replaces cofiguration at the next active index after currActiveIndex and sends back the value. */
            newActiveIndex = potAPI.initRenew(rspName.getValue(), sfcSize, sfgNameList, serviceIndex, currActiveIndex);
            if (newActiveIndex == -1) {
                LOG.error("refreshSfcPot: Could not initialize SfcPotAPI");
                return newActiveIndex;
            }
        }

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

        /* Set the new Active Index to allow SB to set it properly */
        potAPI.setIoamPotParameters(rspIoamPotAugmentationBuilder, newActiveIndex);

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
            LOG.debug("Updated RSP: {}", sfp.getName());
        } else {
            LOG.error("{}: Failed to update Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], sfp.getName());
            return -1;
        }

        if (flagRefresh) {
            SfcPotTimerTask potTimerTask = new SfcPotTimerTask(rspName);
            SfcPotTimerWheel potTimerWheel = SfcPotTimerWheel.getInstance();

            /* NOTE:TODO: support other time units. Currently only ms */
            Timeout potTimeout = potTimerWheel.setTimerContext(potTimerTask,
                                                           refreshPeriodValue.longValue(),
                                                           TimeUnit.MILLISECONDS);

            /* Start off with active cfg index 0 */
            potTimerData.addRspData(rspName, refreshPeriodValue, refreshPeriodTimeUnits,
                                    0,
                                    potTimerTask, potTimeout);

            return 0;
        } else {
            return newActiveIndex;
        }
    }

    public static boolean enableSfcPot(RspName rspName,
                                       final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                                       Long refreshPeriodValue,
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
                                       Long refreshPeriodValue,
                                       BitMaskOptions ioamPotProfileBitMask) {
        int retval = 0;

        retval = processSfcPot(rsp, refreshPeriodTimeUnits, refreshPeriodValue, ioamPotProfileBitMask, 0, false);
        if (retval >= 0) {
            return true;
        }

        return false;
    }

    /*
     * This method refreshes the SFC PoT configuration based on a timer.  The timer event looks at
     * timeouts for each RSP and refreshes the PoT configuration.
     * @see SfcPotTimerThread
     */
    public static int refreshSfcPot(RspName rspName,
                                    int currActiveIndex,
                                    Long refreshPeriodValue) {

        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);

        return (processSfcPot(rsp, null, refreshPeriodValue, null, currActiveIndex, true));
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
        SfcPotTimerData potTimerData = SfcPotTimerData.getInstance();

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
            return false;
        }

        /* cleanup timer related state */
        SfcPotTimerWheel timer = SfcPotTimerWheel.getInstance();
        timer.clearTimerContext(potTimerData.getRspDataTimeout(rspName));

        potTimerData.delRspData(rspName);

        return true;
    }


    /* On RSP delete, clean up local data store and stop timers etc., */
    public void deleteRspEvent(RenderedServicePath rsp) {
        RspName rspName;
        SfcPotTimerWheel timer = SfcPotTimerWheel.getInstance();
        SfcPotTimerData  potTimerData = SfcPotTimerData.getInstance();

        if (rsp == null) return;

        rspName = rsp.getName();
        timer.clearTimerContext(potTimerData.getRspDataTimeout(rspName));

        potTimerData.delRspData(rspName);
    }
}
