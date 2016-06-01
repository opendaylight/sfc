/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionGroupAPI;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationAPI;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
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
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.SfcvAlgorithmIdentity;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * This class is used to handle RPC for SFC verification enabling, disable and to
 * handle RSP deletes.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_verify.provider.SfcVerifyRspProcessor
 * @since 2016-06-01
 */
public class SfcVerifyRspProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcVerifyRspProcessor.class);

    private final SfcVerifyNodeManager nodeManager;

    public SfcVerifyRspProcessor(DataBroker dataBroker, SfcVerifyNodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }


    //
    // NOTE: This method currently updates the RSP and writes it back.  Due to the
    // nature of the SFC verification requiring configured and generated parameters
    // to be added to the RSP and to each hops of the component SFC, the RSP update
    // method is chosen.  In future versions, the RSP augmentations and SFCV data
    // will be moved out and stored separately to avoid updating the RSP itself.
    //
    public static boolean enableSfcVerification(RspName rspName,
                                                NodeId verifyNodeId,
                                                Long sfcVerifyNumProfiles,
                                                Long sfcVerifyProfilesValidator) {

        ServiceFunctionPath sfp;
        List<RenderedServicePathHop> renderedServicePathHopList;
        SfcName serviceFunctionChainNameObj;
        ServiceFunctionChain serviceFunctionChain;
        int sfcSize;
        SfcSfcvAugmentation sfcSfcvAugmentation;
        RenderedServicePathBuilder renderedServicePathBuilder;
        RspSfcvAugmentationBuilder rspSfcvAugmentationBuilder = new RspSfcvAugmentationBuilder();
        int serviceIndex;
        int MAX_STARTING_INDEX = 255;
        int DEF_NUM_PROFILES = 3;
        int DEF_VALIDATOR_VAL = 1000;
        short posIndex = 0;
        long sfcvNumProfiles = DEF_NUM_PROFILES;
        long sfcvProfilesValidator = DEF_VALIDATOR_VAL;


        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (rsp == null) {
            LOG.error("enableSfcVerification:Rendered service path by name:{} does not exist.", rspName);
            return false;
        }

        renderedServicePathBuilder = new RenderedServicePathBuilder(rsp);
        sfp = SfcProviderServicePathAPI.readServiceFunctionPath(rsp.getParentServiceFunctionPath());

        serviceFunctionChainNameObj = sfp.getServiceChainName();
        serviceFunctionChain = serviceFunctionChainNameObj != null ?
                SfcProviderServiceChainAPI.readServiceFunctionChain(serviceFunctionChainNameObj)
                : null;
        if (serviceFunctionChain == null) {
            LOG.error("enableSfcVerification:ServiceFunctionChain name for Path {} is null.", sfp.getName());
            return false;
        }


        SfcVerificationAPI verificationAPI = SfcVerificationAPI.getAPI(null);
        if (verificationAPI == null) {
            LOG.error("enableSfcVerification:Verfication algorithm not supported.");
            return false;
        }

        rspSfcvAugmentationBuilder.setSfcvAlgorithm(SfcVerificationAPI.getDefaultSfcvAlgorithm());

        // Descending order
        serviceIndex = MAX_STARTING_INDEX;

        //Count the number of service functions in the SFC.  That is all we currently need.
        sfcSize = serviceFunctionChain.getSfcServiceFunction().size();

        List<String> sfgNameList = SfcProviderServiceFunctionGroupAPI.getSfgNameList(serviceFunctionChain);
        if((sfcSize == 0) && (sfgNameList == null)) {
            LOG.error("enableSfcVerification:Service Function chain returned empty list");
            return false;
        }

        if (sfcVerifyNumProfiles > 0) {
            sfcvNumProfiles = sfcVerifyNumProfiles;
        }

        if (sfcVerifyProfilesValidator > 0) {
            sfcvProfilesValidator = sfcVerifyProfilesValidator;
        }

        //NOTE:Currently assuming last node in the service chain to be 'verifier'/decap node
        if (!verificationAPI.init(rspName.getValue(), sfcSize, sfgNameList, serviceIndex, sfcvNumProfiles, sfcvProfilesValidator)) {
            LOG.error("enableSfcVerification: Could not initialize SfcVerificationAPI");
            return false;
        }

        LOG.debug("enableSfcVerification: Initialized with num:{}, valid:{}", sfcvNumProfiles, sfcvProfilesValidator);

        renderedServicePathHopList = rsp.getRenderedServicePathHop();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder(renderedServicePathHop);

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

        verificationAPI.setSfcvParameters(rspSfcvAugmentationBuilder, null);

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

        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath, LogicalDatastoreType.OPERATIONAL)) {
            LOG.debug("enableSfcVerification: Updated RSP: {}", sfp.getName());
        } else {
            LOG.error("{}: enableSfcVerification: Failed to update Rendered Service Path: {}",
                    Thread.currentThread().getStackTrace()[1], sfp.getName());
        }

        return true;
    }

    public static boolean disableSfcVerification(RspName rspName) {
        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (rsp == null) {
            LOG.error("Rendered service path by name:{} does not exist.", rspName);
            return false;
        }

        //TODO:Remove SFCV related augmentations and update RSP.

        return true;
    }

    public void deleteRsp(RenderedServicePath renderedServicePath) {
        //TODO: handle RSP deletes, as needed.
    }
}
