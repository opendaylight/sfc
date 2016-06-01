/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.sfc_verify.provider.SfcVerifyNodeManager;
import org.opendaylight.sfc.sfc_verify.provider.SfcVerifyNetconfDataProvider;
import org.opendaylight.sfc.sfc_verify.provider.SfcVerifyRspProcessor;
import org.opendaylight.sfc.sfc_verify.listener.SfcVerifyRSPDataListener;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initialize all necessary SFC verification components
 */
public class SfcVerify {
    final OpendaylightSfc opendaylightSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static final Logger LOG = LoggerFactory.getLogger(SfcVerify.class);
    private final String verTypeName = "verification";

    private final SfcVerifyNodeManager nodeManager;
    private final SfcVerifyRSPDataListener sfcVerifyRSPDataListener;
    private final SfcVerifyRspProcessor sfcVerifyRspProcessor;

    public static final InstanceIdentifier<ServiceFunctionTypes> SFT_IID =
            InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

    public SfcVerify(DataBroker dataBroker,
                     BindingAwareBroker bindingAwareBroker) {

        //Register as provider for node handling and netconf node notifications/subscriptions.
        nodeManager = new SfcVerifyNodeManager(dataBroker, bindingAwareBroker);

        //Register as consumer for sending down configurations to SB nodes.
        final SfcVerifyNetconfDataProvider sfcVerifyNetconfDataProvider = SfcVerifyNetconfDataProvider.GetNetconfDataProvider();
        bindingAwareBroker.registerConsumer(sfcVerifyNetconfDataProvider);

        //For now, listen to RSP creates to add updates for augmentations.  In future,
        //this will be used to create a parallel run-time data to effect SFCV
        //configurations.
        sfcVerifyRspProcessor = new SfcVerifyRspProcessor(dataBroker, nodeManager);

        //Register for RSP update/delete for creating and sending out SB configuration.
        sfcVerifyRSPDataListener = new SfcVerifyRSPDataListener(opendaylightSfc);

        //Create a new ServiceFunctionType called 'verification' to allow SFC Verification
        //decap node role.
        List<ServiceFunctionType> sftList = new ArrayList<>();
        SftType sftType = new SftType(verTypeName);
        ServiceFunctionTypeBuilder sftBuilder = new ServiceFunctionTypeBuilder()
            .setKey(new ServiceFunctionTypeKey(sftType))
            .setNshAware(false)
            .setSymmetry(false)
            .setBidirectionality(false)
            .setRequestReclassification(false)
            .setType(sftType);
        sftList.add(sftBuilder.build());
        ServiceFunctionTypesBuilder sftTypesBuilder = new ServiceFunctionTypesBuilder().setServiceFunctionType(sftList);

        //Call merge to avoid overwriting existing types.
        if (SfcDataStoreAPI.writeMergeTransactionAPI(SFT_IID, sftTypesBuilder.build(),
                LogicalDatastoreType.CONFIGURATION)) {
            LOG.debug("Added verification Service Function Types.");
        } else {
            LOG.error("Could not add verification Service Function Type.");
        }
    }

    public void unregisterListeners() {
        nodeManager.unregisterNodeListener();
        sfcVerifyRspProcessor.unregisterRspListener();
        sfcVerifyRSPDataListener.getDataChangeListenerRegistration().close();
    }

    public void close() {
    }
}
