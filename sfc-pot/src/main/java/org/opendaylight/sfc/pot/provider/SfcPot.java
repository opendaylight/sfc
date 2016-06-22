/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.provider.OpendaylightSfc;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initialize all necessary SFC Proof of Transit components
 */
public class SfcPot {

    final OpendaylightSfc opendaylightSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static final Logger LOG = LoggerFactory.getLogger(SfcPot.class);
    private static final String SFCV_STR = "ioam-pot";

    public static final InstanceIdentifier<ServiceFunctionTypes> SFT_IID =
            InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

    public SfcPot(DataBroker dataBroker,
                  BindingAwareBroker bindingAwareBroker) {

    }

    public void unregisterListeners() {
    }

    public void close() {
    }
}
