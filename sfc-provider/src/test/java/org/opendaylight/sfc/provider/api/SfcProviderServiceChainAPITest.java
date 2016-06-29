/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.service.function.chain.state.SfcServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.service.function.chain.state.SfcServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.service.function.chain.state.SfcServicePathKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SfcProviderServiceChainAPITest extends AbstractDataStoreManager {

    private List<ServiceFunction> sfList = new ArrayList<>();

    @Before
    public void before() {
        setOdlSfc();
    }

    @Test
    public void testCreateReadServiceFunctionChain() {

        SfcName name = new SfcName("unittest-chain-1");
        ServiceFunctionChainKey key = new ServiceFunctionChainKey(name);

        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        for (ServiceFunction serviceFunction : sfList) {

            SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
            // As per YANG model for service-chain, this name is NOT Service Function name hence
            // still String
            SfcServiceFunction sfcServiceFunction =
                    sfcServiceFunctionBuilder.setName(serviceFunction.getName().getValue())
                        .setKey(new SfcServiceFunctionKey(serviceFunction.getName().getValue()))
                        .setType(serviceFunction.getType())
                        .build();
            sfcServiceFunctionList.add(sfcServiceFunction);
        }
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(name).setKey(key).setSfcServiceFunction(sfcServiceFunctionList).setSymmetric(false);

        SfcProviderServiceChainAPI.putServiceFunctionChain(sfcBuilder.build());
        ServiceFunctionChain sfc2 = SfcProviderServiceChainAPI.readServiceFunctionChain(name);

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2.getSfcServiceFunction(), sfcServiceFunctionList);
    }

    @Test
    public void testCreateReadServiceFunctionChains() {

        List<SfcServiceFunction> sfcAllServiceFunctionList = new ArrayList<>();

        for (ServiceFunction serviceFunction : sfList) {

            SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
            SfcServiceFunction sfcServiceFunction =
                    sfcServiceFunctionBuilder.setName(serviceFunction.getName().getValue())
                        .setKey(new SfcServiceFunctionKey(serviceFunction.getName().getValue()))
                        .setType(serviceFunction.getType())
                        .build();
            sfcAllServiceFunctionList.add(sfcServiceFunction);
        }

        List<SfcName> sfcName = new ArrayList<>();
        sfcName.add(new SfcName("unittest-sfc-1"));
        sfcName.add(new SfcName("unittest-sfc-2"));
        sfcName.add(new SfcName("unittest-sfc-3"));

        ServiceFunctionChain[] sfcArray = new ServiceFunctionChain[3];

        List<ServiceFunctionChain> sfcList = new ArrayList<>();
        int i = 0;
        for (SfcName name : sfcName) {
            List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

            int j = 0;
            for (SfcServiceFunction sf : sfcAllServiceFunctionList) {
                if (i != j) {
                    sfcServiceFunctionList.add(sf);
                }
                j++;
            }
            ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
            sfcArray[i] = sfcBuilder.setName(name)
                .setKey(new ServiceFunctionChainKey(name))
                .setSfcServiceFunction(sfcServiceFunctionList)
                .setSymmetric(false)
                .build();
            sfcList.add(sfcArray[i]);
            i++;
        }

        ServiceFunctionChainsBuilder sfcsBuilder = new ServiceFunctionChainsBuilder();
        sfcsBuilder.setServiceFunctionChain(sfcList);

        InstanceIdentifier<ServiceFunctionChains> sfcsIID =
                InstanceIdentifier.builder(ServiceFunctionChains.class).build();
        SfcDataStoreAPI.writePutTransactionAPI(sfcsIID, sfcsBuilder.build(), LogicalDatastoreType.CONFIGURATION);

        final int INDEX_TO_READ = 1;
        ServiceFunctionChain sfc2 = SfcProviderServiceChainAPI.readServiceFunctionChain(sfcName.get(INDEX_TO_READ));

        assertNotNull("Must be not null", sfc2);
        assertEquals("Must be equal", sfc2, sfcArray[INDEX_TO_READ]);
    }

    /*
     * create service function chains object with one chain, that chain contains one service
     * function
     * all these data are written into data store and then get back
     */
    @Test
    public void testGetServiceFunctionChainsRef() {
        SfcName sfcName = new SfcName("SFC1");
        SfName sfName = new SfName("SF1");
        SfcServiceFunctionBuilder serviceFunctionBuilder = new SfcServiceFunctionBuilder();
        List<SfcServiceFunction> serviceFunctionList = new ArrayList<>();

        // build service function and add to list
        serviceFunctionBuilder.setName(sfName.getValue()).setKey(new SfcServiceFunctionKey(sfName.getValue()));

        SfcServiceFunction sfcServiceFunction = serviceFunctionBuilder.build();
        serviceFunctionList.add(sfcServiceFunction);

        // build service function chain
        ServiceFunctionChainBuilder serviceFunctionChainBuilder = new ServiceFunctionChainBuilder();
        serviceFunctionChainBuilder.setName(sfcName)
            .setKey(new ServiceFunctionChainKey(sfcName))
            .setSfcServiceFunction(serviceFunctionList);

        // add chain to service function chains
        ServiceFunctionChainsBuilder serviceFunctionChainsBuilder = new ServiceFunctionChainsBuilder();
        List<ServiceFunctionChain> serviceFunctionChainList = new ArrayList<>();
        serviceFunctionChainList.add(serviceFunctionChainBuilder.build());
        serviceFunctionChainsBuilder.setServiceFunctionChain(serviceFunctionChainList);

        // create instance identifier
        InstanceIdentifier<ServiceFunctionChains> sfcIID =
                InstanceIdentifier.builder(ServiceFunctionChains.class).build();

        // write chain to data store
        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(sfcIID,
                serviceFunctionChainsBuilder.build(), LogicalDatastoreType.CONFIGURATION);
        assertTrue("Must be true", transactionSuccessful);

        // get service function chains
        ServiceFunctionChains serviceFunctionChains = SfcDataStoreAPI.readTransactionAPI(InstanceIdentifier.builder(ServiceFunctionChains.class).build(), LogicalDatastoreType.CONFIGURATION);
        assertNotNull("Must not be null", serviceFunctionChains);
        assertEquals("Must be equal", serviceFunctionChains.getServiceFunctionChain().get(0).getName(), sfcName);
        assertEquals("Must be equal",
                serviceFunctionChains.getServiceFunctionChain().get(0).getSfcServiceFunction().get(0).getName(),
                sfName.getValue());

        // remove data
        transactionSuccessful = SfcDataStoreAPI.deleteTransactionAPI(sfcIID, LogicalDatastoreType.CONFIGURATION);
        assertTrue("Must be true", transactionSuccessful);
    }

    /*
     * create service function chains state object with one service function chain state containing
     * service function path
     * this object is put into data store, then get back and removed
     */
    @Test
    public void testGetServiceFunctionChainsStateRef() {
        SfcName sfcName = new SfcName("SFC1");
        SfpName sfpName = new SfpName("SP1");
        ServiceFunctionChainsStateBuilder serviceFunctionChainsStateBuilder = new ServiceFunctionChainsStateBuilder();

        // build service function path and add to list
        SfcServicePathBuilder sfcServicePathBuilder = new SfcServicePathBuilder();
        sfcServicePathBuilder.setName(sfpName).setKey(new SfcServicePathKey(sfpName));

        List<SfcServicePath> sfcServicePathList = new ArrayList<>();
        sfcServicePathList.add(sfcServicePathBuilder.build());

        // create service function chain state with sfc path and add to list
        ServiceFunctionChainStateBuilder serviceFunctionChainStateBuilder = new ServiceFunctionChainStateBuilder();
        List<ServiceFunctionChainState> serviceFunctionChainStateList = new ArrayList<>();
        serviceFunctionChainStateBuilder.setName(sfcName)
            .setKey(new ServiceFunctionChainStateKey(sfcName))
            .setSfcServicePath(sfcServicePathList);
        serviceFunctionChainStateList.add(serviceFunctionChainStateBuilder.build());

        // build service function chains state
        serviceFunctionChainsStateBuilder.setServiceFunctionChainState(serviceFunctionChainStateList);

        // create instance identifier
        InstanceIdentifier<ServiceFunctionChainsState> sfcsIID =
                InstanceIdentifier.builder(ServiceFunctionChainsState.class).build();

        // write transaction
        boolean transactionSuccessful = SfcDataStoreAPI.writePutTransactionAPI(sfcsIID,
                serviceFunctionChainsStateBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);

        // get data
        ServiceFunctionChainsState serviceFunctionChainsState =
                SfcDataStoreAPI.readTransactionAPI(InstanceIdentifier.builder(ServiceFunctionChainsState.class).build(), LogicalDatastoreType.OPERATIONAL);
        assertNotNull("Must not be null", serviceFunctionChainsState);
        assertEquals("Must be equal", serviceFunctionChainsState.getServiceFunctionChainState().get(0).getName(),
                sfcName);
        assertEquals("Must be equal",
                serviceFunctionChainsState.getServiceFunctionChainState().get(0).getSfcServicePath().get(0).getName(),
                sfpName);

        // remove data
        transactionSuccessful = SfcDataStoreAPI.deleteTransactionAPI(sfcsIID, LogicalDatastoreType.OPERATIONAL);
        assertTrue("Must be true", transactionSuccessful);
    }

}
