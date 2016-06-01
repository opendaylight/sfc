/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider.api;



import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionGroupAPI;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.IoamScvListener;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.ScvProfileRefresh;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.ScvProfileRenew;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.RspSfcvAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.RspSfcvAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.RspSfcvHopAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.SfcSfcvAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.SfcvAlgorithmIdentity;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.util.ArrayList;
import java.util.List;


/*
 * Listener class that implements handlers for iOAM SCV notifications: renew and refresh.
 *
 * On renew, the new configurations are generated only for the requested index and number of profiles from
 * that index and the RSP is updated.
 *
 * On refresh, the configurations are NOT regenerated but the profiles at the requested index and number
 * of profiles from that index are resent and the RSP is updated.

 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationListener
 * @since 2016-05-01
 */
public class SfcVerificationListener implements IoamScvListener {
    public static final String IOAM_SCV = "ioam-scv";
    private static final Logger LOG = LoggerFactory.getLogger(SfcVerificationListener.class);
    //private SfcServiceFunctionSchedulerAPI scheduler;
    private NodeId srcNodeId;

    public SfcVerificationListener(NodeId srcNodeId) {
        this.srcNodeId = srcNodeId;
    }

    private void updateProfile(String rspName, Short sfcvStartIndex, Short sfcvNumProfiles, boolean flagRefresh) {
        ServiceFunctionPath sfp;
        RenderedServicePath rsp;
        List<RenderedServicePathHop> renderedServicePathHopList;
        SfcName serviceFunctionChainNameObj;
        ServiceFunctionChain serviceFunctionChain;
        SfcSfcvAugmentation sfcSfcvAugmentation;
        Class<? extends SfcvAlgorithmIdentity> sfcvAlgorithm = null;
        RenderedServicePathBuilder renderedServicePathBuilder;
        RspSfcvAugmentationBuilder rspSfcvAugmentationBuilder = new RspSfcvAugmentationBuilder();
        int serviceIndex;
        int MAX_STARTING_INDEX = 255;
        short posIndex = 0;
        RspName rspNameObj;

        rspNameObj = new RspName(rspName);
        rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspNameObj);
        if (rsp == null) {
            LOG.error("updateProfile:RSP null in handle notification.");
            return;
        }

        renderedServicePathBuilder = new RenderedServicePathBuilder(rsp);
        sfp = SfcProviderServicePathAPI.readServiceFunctionPath(rsp.getParentServiceFunctionPath());

        serviceFunctionChainNameObj = sfp.getServiceChainName();
        serviceFunctionChain = serviceFunctionChainNameObj != null ?
                SfcProviderServiceChainAPI.readServiceFunctionChain(serviceFunctionChainNameObj)
                : null;
        if (serviceFunctionChain == null) {
            LOG.error("updateProfile:ServiceFunctionChain name for Path {} is null.", sfp.getName());
            return;
        }

        sfcSfcvAugmentation = serviceFunctionChain.getAugmentation(SfcSfcvAugmentation.class);
        if (sfcSfcvAugmentation != null) {
            sfcvAlgorithm = sfcSfcvAugmentation.getSfcvAlgorithm();
        }

        SfcVerificationAPI verificationAPI = SfcVerificationAPI.getAPI(sfcvAlgorithm);
        if (verificationAPI == null) {
            LOG.error("updateProfile:Verfication algorithm {} not supported.", sfcvAlgorithm.getSimpleName());
            return;
        }
        if (sfcvAlgorithm != null) {
            rspSfcvAugmentationBuilder.setSfcvAlgorithm(sfcvAlgorithm);
        }

        // Descending order
        serviceIndex = MAX_STARTING_INDEX;

        //Count the number of service functions in the SFC.  That is all we currently need.
        int sfcSize = serviceFunctionChain.getSfcServiceFunction().size();


        List<String> sfgNameList = SfcProviderServiceFunctionGroupAPI.getSfgNameList(serviceFunctionChain);
        if((sfcSize == 0) && (sfgNameList == null)) {
            LOG.error("updateProfile: ServiceFunctionchain returned empty list");
            return;
        }

        if (flagRefresh == true) {
            if (!verificationAPI.initRefresh(rspName, sfcSize, sfgNameList, serviceIndex, sfcvStartIndex, sfcvNumProfiles)) {
                LOG.error("updateProfile:Refresh:Could not init SfcVerificationAPI {}",
                          sfcvAlgorithm == null? "None": sfcvAlgorithm.getSimpleName());
                return;
            }
        } else {
            if (!verificationAPI.initRenew(rspName, sfcSize, sfgNameList, serviceIndex, sfcvStartIndex, sfcvNumProfiles)) {
                LOG.error("updateProfile:Renew:Could not init SfcVerificationAPI {}",
                          sfcvAlgorithm == null? "None": sfcvAlgorithm.getSimpleName());
                return;
            }
        }

        renderedServicePathHopList = rsp.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder(renderedServicePathHop);

            /* first remove the existing augmentation */
            renderedServicePathHopBuilder.removeAugmentation(RspSfcvHopAugmentation.class);

            /* add a fresh sfcv hop augmentation with asked for profiles only */
            verificationAPI.setBuilderHopSecret(renderedServicePathHopBuilder, serviceIndex, posIndex, null, null);

            serviceIndex--;
            posIndex++;
        }

        /* Now that, we have modified the array list augmentations...set it back */
        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopList);

        /* remove the existing augmentation */
        renderedServicePathBuilder.removeAugmentation(RspSfcvAugmentation.class);

        /* redo the basic augmentation as a fresh sfcv augmentation */
        if (flagRefresh == true) {
            verificationAPI.setSfcvParameters(rspSfcvAugmentationBuilder, srcNodeId);
        } else {
            verificationAPI.setSfcvParameters(rspSfcvAugmentationBuilder, null);
        }
        renderedServicePathBuilder.addAugmentation(RspSfcvAugmentation.class, rspSfcvAugmentationBuilder.build());

        verificationAPI.close();

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

        /* Currently using merge instead of put, but can be latter as well */
        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath, LogicalDatastoreType.OPERATIONAL)) {
            LOG.debug("updateProfile: Updated RSP: {}", sfp.getName());
        } else {
            LOG.error("{}: Failed to update Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], sfp.getName());
        }
    }

    @Override
    public void onScvProfileRefresh(ScvProfileRefresh notification) {
        String rspName = notification.getRenderedServicePathName().getValue();
        Short  sfcvStartIndex = notification.getScvProfileRefreshStartIndex();
        Short  sfcvNumProfiles = notification.getScvProfileRefreshNumProfiles();

        LOG.debug("Got onScvProfileRefresh notification: RSP:{}, idx:{}, num:{}", rspName, sfcvStartIndex, sfcvNumProfiles);

        //Set flagRefresh to true to indicate refresh.
        updateProfile(rspName, sfcvStartIndex, sfcvNumProfiles, true);
    }

    @Override
    public void onScvProfileRenew(ScvProfileRenew notification) {
        String rspName = notification.getRenderedServicePathName().getValue();
        Short  sfcvStartIndex = notification.getScvProfileRenewStartIndex();
        Short  sfcvNumProfiles = notification.getScvProfileRenewNumProfiles();

        LOG.debug("Got onScvProfileRenew notification: RSP:{}, idx:{}, num:{}", rspName, sfcvStartIndex, sfcvNumProfiles);

        //Set flagRefresh to false to indicate renew.
        updateProfile(rspName, sfcvStartIndex, sfcvNumProfiles, false);
    }
}
