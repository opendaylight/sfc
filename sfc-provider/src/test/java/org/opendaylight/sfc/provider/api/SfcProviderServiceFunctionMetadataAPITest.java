/*
 * Copyright (c) 2015 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.ContextMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.ContextMetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.ContextMetadataKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.VariableMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.VariableMetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.VariableMetadataKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.variable.metadata.TlvMetadata;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.md.rev140701.service.function.metadata.variable.metadata.TlvMetadataBuilder;


/**
 * This class defines the APIs to operate on the ServiceFunctionScheduleTypes
 * datastore.
 *
 * @author Ruijing Guo(ruijing.guo@intel.com)
 * @version 0.1
 * @since 2015-10-12
 */

public class SfcProviderServiceFunctionMetadataAPITest extends AbstractDataStoreManager {

    @Before
    public void before() {
        setOdlSfc();
    }

    @Test
    public void testContextMetadata() {
        //build service function metadata
        ContextMetadataBuilder contextMetadataBuilder = new ContextMetadataBuilder();

        contextMetadataBuilder.setName("CMD")
                .setKey(new ContextMetadataKey("CMD"))
                .setContextHeader1(1L)
                .setContextHeader2(2L)
                .setContextHeader3(3L)
                .setContextHeader4(4L);

        //write service function metadata
        boolean transactionSuccessful = SfcProviderServiceFunctionMetadataAPI.putContextMetadata(contextMetadataBuilder.build());
        assertTrue("Must be true", transactionSuccessful);

        //read service function metadata
        ContextMetadata contextMetadata =  SfcProviderServiceFunctionMetadataAPI.readContextMetadata("CMD");
        assertNotNull("Must not be null", contextMetadata);
        assertEquals("Must be equal", contextMetadata.getName(), "CMD");
        assertEquals("Must be equal", contextMetadata.getContextHeader1(), (Object)1L);
        assertEquals("Must be equal", contextMetadata.getContextHeader2(), (Object)2L);
        assertEquals("Must be equal", contextMetadata.getContextHeader3(), (Object)3L);
        assertEquals("Must be equal", contextMetadata.getContextHeader4(), (Object)4L);

        //remove service function metadata
        transactionSuccessful = SfcProviderServiceFunctionMetadataAPI.deleteContextMetadata("CMD");
        assertTrue("Must be true", transactionSuccessful);
    }

    @Test
    public void testVariableMetadata() {
        //build service function metadata
        VariableMetadataBuilder variableMetadataBuilder = new VariableMetadataBuilder();
        List<TlvMetadata> tlvList =  new ArrayList<>();

        TlvMetadataBuilder tlvMetadataBuilder = new TlvMetadataBuilder();
        tlvMetadataBuilder.setTlvClass(1)
                .setTlvType(Short.valueOf("2"))
                .setLength(Short.valueOf("6"))
                .setTlvData("123456");

        tlvList.add(tlvMetadataBuilder.build());

        variableMetadataBuilder.setName("VMD")
                .setKey(new VariableMetadataKey("VMD"))
                .setTlvMetadata(tlvList);

        //write service function metadata
        boolean transactionSuccessful = SfcProviderServiceFunctionMetadataAPI.putVariableMetadata(variableMetadataBuilder.build());
        assertTrue("Must be true", transactionSuccessful);

        //read service function metadata
        VariableMetadata variableMetadata =  SfcProviderServiceFunctionMetadataAPI.readVariableMetadata("VMD");
        assertNotNull("Must not be null", variableMetadata);
        assertNotNull("Must not be null", variableMetadata.getTlvMetadata());
        assertEquals("Must be equal", variableMetadata.getTlvMetadata().size(), 1);
        assertNotNull("Must not be null", variableMetadata.getTlvMetadata().get(0));
        assertEquals("Must be equal", variableMetadata.getTlvMetadata().get(0).getTlvClass(), (Object)1);
        assertEquals("Must be equal", variableMetadata.getTlvMetadata().get(0).getTlvType(), Short.valueOf("2"));
        assertEquals("Must be equal", variableMetadata.getTlvMetadata().get(0).getLength(), Short.valueOf("6"));
        assertEquals("Must be equal", variableMetadata.getTlvMetadata().get(0).getTlvData(), "123456");

        //remove service function metadata
        transactionSuccessful = SfcProviderServiceFunctionMetadataAPI.deleteVariableMetadata("VMD");
        assertTrue("Must be true", transactionSuccessful);
    }
}
