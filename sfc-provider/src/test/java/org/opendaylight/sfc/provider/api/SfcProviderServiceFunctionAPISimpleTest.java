/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1Builder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMonBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFDescriptionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFMonitoringInfoOutputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcProviderSfDescriptionMonitorAPI.class)
public class SfcProviderServiceFunctionAPISimpleTest extends AbstractDataStoreManager {

    private static final String[] IP_MGMT_ADDRESS =
            {"196.168.55.101",
                    "196.168.55.102",
                    "196.168.55.103"};
    private static final String SF_NAME = "dummySF";
    private static final String SF_STATE_NAME = "dummySFS";
    private static final String SF_SERVICE_PATH = "dummySFSP";
    private static final String RSP_NAME = "dummyRSP";
    private static final int PORT = 555;

    @Before
    public void before() {
        setOdlSfc();
    }

    /*
     * test, whether is possible to put service function monitor & description into service function
     */
    @Test
    public void testPutServiceFunctionDescriptionAndMonitor() throws Exception {

        //create description
        org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.description.output.DescriptionInfoBuilder descriptionInfoBuilder
                = new org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.description.output.DescriptionInfoBuilder();
        GetSFDescriptionOutputBuilder getSFDescriptionOutputBuilder = new GetSFDescriptionOutputBuilder();

        descriptionInfoBuilder.setNumberOfDataports(1L);
        getSFDescriptionOutputBuilder.setDescriptionInfo(descriptionInfoBuilder.build());

        //create monitor
        org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.monitoring.info.output.MonitoringInfoBuilder monitoringInfoBuilder
                = new org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.monitoring.info.output.MonitoringInfoBuilder();
        GetSFMonitoringInfoOutputBuilder getSFMonitoringInfoOutputBuilder = new GetSFMonitoringInfoOutputBuilder();

        getSFMonitoringInfoOutputBuilder.setMonitoringInfo(monitoringInfoBuilder.build());

        //push service function state with augmentation into data store
        boolean transactionSuccessful = writeServiceFunctionStateAugmentation();

        assertTrue("Must be true", transactionSuccessful);

        //build service function
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SF_NAME)
                .setKey(new ServiceFunctionKey(SF_NAME))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS[1])));

        PowerMockito.stub(PowerMockito.method(SfcProviderSfDescriptionMonitorAPI.class, "getSFDescriptionInfoFromNetconf")).toReturn(getSFDescriptionOutputBuilder.build());
        PowerMockito.stub(PowerMockito.method(SfcProviderSfDescriptionMonitorAPI.class, "getSFMonitorInfoFromNetconf")).toReturn(getSFMonitoringInfoOutputBuilder.build());

        boolean result = SfcProviderServiceFunctionAPI.putServiceFunctionDescriptionExecutor(serviceFunctionBuilder.build());

        assertTrue("Must be true", result);

        result = SfcProviderServiceFunctionAPI.putServiceFunctionMonitorExecutor(serviceFunctionBuilder.build());

        assertTrue("Must be true", result);
    }

    //write or remove service function state with augmentation
    private boolean writeServiceFunctionStateAugmentation() {

        MonitoringInfoBuilder monitoringInfoBuilder1 = new MonitoringInfoBuilder();
        DescriptionInfoBuilder descriptionInfoBuilder1 = new DescriptionInfoBuilder();
        SfcSfDescMonBuilder sfcSfDescMonBuilder = new SfcSfDescMonBuilder();
        sfcSfDescMonBuilder.setMonitoringInfo(monitoringInfoBuilder1.build())
                .setDescriptionInfo(descriptionInfoBuilder1.build());

        ServiceFunctionState1Builder serviceFunctionState1Builder = new ServiceFunctionState1Builder();
        serviceFunctionState1Builder.setSfcSfDescMon(sfcSfDescMonBuilder.build());

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        serviceFunctionStateBuilder.setName(SF_STATE_NAME)
                .setKey(new ServiceFunctionStateKey(SF_STATE_NAME))
                .addAugmentation(ServiceFunctionState1.class, serviceFunctionState1Builder.build());

        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier
                .builder(ServiceFunctionsState.class)
                .child(ServiceFunctionState.class, new ServiceFunctionStateKey(SF_STATE_NAME))
                .build();


        return SfcDataStoreAPI.writePutTransactionAPI(sfStateIID, serviceFunctionStateBuilder.build(), LogicalDatastoreType.OPERATIONAL);
    }
}