/*
 * Copyright (c) 2016 Eircsson, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.opendaylight.sfc.provider.api.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 *
 * @author eguitom
 */
public class SfcProviderIIDs {
        public static final InstanceIdentifier<ServiceFunctions> SF_IID =
            InstanceIdentifier.builder(ServiceFunctions.class).build();
        public static final InstanceIdentifier<ServiceFunctionForwarders> SFF_IID =
            InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();
        public static final InstanceIdentifier<ServiceFunctionPaths> SFP_IID =
            InstanceIdentifier.builder(ServiceFunctionPaths.class).build();
        public static final InstanceIdentifier<ServiceFunctionChains> SFC_IID =
            InstanceIdentifier.builder(ServiceFunctionChains.class).build();



}
