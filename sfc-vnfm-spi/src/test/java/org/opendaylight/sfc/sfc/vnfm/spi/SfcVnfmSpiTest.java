/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc.vnfm.spi;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;

public class SfcVnfmSpiTest {

    @Before
    public void setup() {

        ServiceFunctionType sft2 = new ServiceFunctionTypeBuilder().setBidirectionality(true)
            .setNshAware(true)
            .setSymmetry(true)
            .setType(new SftType("dummy2"))
            .build();
    }

    @Test
    public void createSfTest() {
        SfcVnfmDummyImpl dummyVnfm = new SfcVnfmDummyImpl();
        ServiceFunctionType sft1 = new ServiceFunctionTypeBuilder().setBidirectionality(true)
            .setNshAware(true)
            .setSymmetry(true)
            .setType(new SftType("dummy1"))
            .build();
        assertTrue("Correct", dummyVnfm.createSf(sft1));

    }
}
