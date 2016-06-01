/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.provider.OpendaylightSfc;

import org.opendaylight.sfc.sfc_verify.listener.RenderedPathListener;
import org.opendaylight.sfc.sfc_verify.listener.SfcVerifyNodeListener;
import org.opendaylight.sfc.sfc_verify.listener.SfcVerifyRSPDataListener;
import org.opendaylight.sfc.sfc_verify.provider.SfcVerifyNodeManager;
import org.opendaylight.sfc.sfc_verify.provider.SfcVerifyRspProcessor;
import org.opendaylight.sfc.sfc_verify.utils.SfcVerifyNetconfDataProvider;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initialize all necessary SFC verification components
 */
public class SfcVerify {
    final OpendaylightSfc opendaylightSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static final Logger LOG = LoggerFactory.getLogger(SfcVerify.class);

    private final SfcVerifyNodeManager nodeManager;
    private final SfcVerifyRSPDataListener sfcVerifyRSPDataListener;
    private final SfcVerifyRspProcessor sfcVerifyRspProcessor;

    private final SfcVerifyNodeListener nodeListener;
    private final RenderedPathListener  rspListener;

    public static final InstanceIdentifier<ServiceFunctionTypes> SFT_IID =
            InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

    public SfcVerify(DataBroker dataBroker,
                     BindingAwareBroker bindingAwareBroker) {

        //Register as provider for node handling and netconf node notifications/subscriptions.
        nodeManager = new SfcVerifyNodeManager(dataBroker, bindingAwareBroker);
        nodeListener = new SfcVerifyNodeListener(dataBroker, nodeManager);

        //Register as consumer for sending down configurations to SB nodes.
        final SfcVerifyNetconfDataProvider sfcVerifyNetconfDataProvider = SfcVerifyNetconfDataProvider.getNetconfDataProvider();
        bindingAwareBroker.registerConsumer(sfcVerifyNetconfDataProvider);

        //For now, listen to RSP creates to add updates for augmentations.  In future,
        //this will be used to create a parallel run-time data to effect SFCV
        //configurations.
        sfcVerifyRspProcessor = new SfcVerifyRspProcessor(dataBroker, nodeManager);
        rspListener = new RenderedPathListener(dataBroker, sfcVerifyRspProcessor);

        //Register for RSP update/delete for creating and sending out SB configuration.
        sfcVerifyRSPDataListener = new SfcVerifyRSPDataListener(opendaylightSfc);
    }

    public void unregisterListeners() {
        nodeListener.getRegistrationObject().close();
        rspListener.getRegistrationObject().close();
        sfcVerifyRSPDataListener.getDataChangeListenerRegistration().close();
    }

    public void close() {
    }
}
