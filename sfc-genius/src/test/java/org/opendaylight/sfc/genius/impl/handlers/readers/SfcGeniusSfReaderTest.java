/*
 * Copyright (c) 2016, 2018 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.OtherBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterfaceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusSfReaderTest {

    @Mock
    ReadTransaction readTransaction;

    @Mock
    Executor executor;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    SfcGeniusSfReader reader;

    ServiceFunctions serviceFunctions;

    private static final Map<String, Set<LocatorType>> SF_LOCATORS = new HashMap<>();

    static {
        SF_LOCATORS.put("SF1", new HashSet<>());
        SF_LOCATORS.get("SF1").add(new LogicalInterfaceBuilder().setInterfaceName("IFA").build());
        SF_LOCATORS.put("SF2", new HashSet<>());
        SF_LOCATORS.get("SF2").add(new OtherBuilder().setOtherName("IFA").build());
        SF_LOCATORS.put("SF3", new HashSet<>());
        SF_LOCATORS.get("SF3").add(new LogicalInterfaceBuilder().setInterfaceName("IFA").build());
        SF_LOCATORS.get("SF3").add(new LogicalInterfaceBuilder().setInterfaceName("IFA").build());
        SF_LOCATORS.put("SF4", new HashSet<>());
        SF_LOCATORS.get("SF4").add(new LogicalInterfaceBuilder().setInterfaceName("IFB").build());
        SF_LOCATORS.put("SF5", new HashSet<>());
        SF_LOCATORS.get("SF5").add(new LogicalInterfaceBuilder().setInterfaceName("IFA").build());
    }

    @Before
    public void setup() {
        serviceFunctions = new ServiceFunctionsBuilder().setServiceFunction(SF_LOCATORS.entrySet().stream()
                .map(entry -> new ServiceFunctionBuilder().setName(new SfName(entry.getKey()))
                        .setSfDataPlaneLocator(entry.getValue().stream()
                                .map(locatorType -> new SfDataPlaneLocatorBuilder().setLocatorType(locatorType).build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList())).build();
    }

    @Test
    public void readSfOnInterface() throws Exception {
        InstanceIdentifier<ServiceFunctions> iid = InstanceIdentifier.builder(ServiceFunctions.class).build();
        doReturn(CompletableFuture.completedFuture(Optional.of(serviceFunctions))).when(reader)
                .doReadOptional(LogicalDatastoreType.CONFIGURATION, iid);

        CompletableFuture<List<SfName>> sfNames = reader.readSfOnInterface("IFA");

        assertThat(sfNames.get(), containsInAnyOrder(new SfName("SF1"), new SfName("SF3"), new SfName("SF5")));
        assertEquals(3, sfNames.get().size());
    }

    @Test
    public void readInterfacesOfSf() throws Exception {
        SfName sfName = new SfName("SF5");
        InstanceIdentifier<ServiceFunction> iid = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, new ServiceFunctionKey(sfName)).build();
        doReturn(CompletableFuture.completedFuture(serviceFunctions.getServiceFunction().stream()
                .filter(serviceFunction -> serviceFunction.getName().equals(sfName)).findFirst())).when(reader)
                        .doReadOptional(LogicalDatastoreType.CONFIGURATION, iid);

        List<String> ifNames = reader.readInterfacesOfSf(sfName).get();

        assertThat(ifNames.size(), is(1));
        assertThat(ifNames.get(0), is("IFA"));
    }
}
