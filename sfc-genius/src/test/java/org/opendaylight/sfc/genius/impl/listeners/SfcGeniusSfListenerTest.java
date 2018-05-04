/*
 * Copyright (c) 2017 SUSE LINUX GmbH.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.genius.impl.SfcGeniusServiceManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterface;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterfaceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusSfListenerTest {

    private ServiceFunction serviceFunction;

    @Mock
    private DataBroker dataBroker;

    @Mock
    private SfcGeniusServiceManager sfcGeniusServiceManager;

    @Mock
    private ExecutorService executorService;

    private SfcGeniusSfListener sfcGeniusSfListener;

    @Before
    public void setup() {
        LogicalInterface logicalInterface = new LogicalInterfaceBuilder().setInterfaceName("IFA").build();
        SfDataPlaneLocator sfDataPlaneLocator;
        sfDataPlaneLocator = new SfDataPlaneLocatorBuilder().setLocatorType(logicalInterface).build();
        List<SfDataPlaneLocator> sfDataPlaneLocatorList = Collections.singletonList(sfDataPlaneLocator);
        serviceFunction = new ServiceFunctionBuilder().setSfDataPlaneLocator(sfDataPlaneLocatorList).build();
        sfcGeniusSfListener = new SfcGeniusSfListener(dataBroker, sfcGeniusServiceManager, executorService);
    }

    @Test
    public void remove() throws Exception {
        sfcGeniusSfListener.remove(InstanceIdentifier.create(ServiceFunction.class), serviceFunction);
        verify(sfcGeniusServiceManager).unbindInterfaces(Collections.singletonList("IFA"));
    }
}
