/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.BridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;

import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ClassifierHandlerTest {
    @Mock
    private SclServiceFunctionForwarder sffClassifier;

    @Mock
    private ServiceFunctionClassifier scf;

    @Mock
    private ServiceFunctionForwarder sff;

    private final ClassifierHandler handler;

    private final String INTERFACE_TO_CLASSIFY = "750135c0-67a9-4fc1-aac0-1359ae7944d4";

    public ClassifierHandlerTest() {
        initMocks(this);
        handler = new ClassifierHandler();
    }

    @Before
    public void setUp() {
        when(sffClassifier.getName()).thenReturn("sffName");
        when(sffClassifier.getAttachmentPointType())
            .thenReturn(new InterfaceBuilder()
                        .setInterface(INTERFACE_TO_CLASSIFY)
                        .build());
        when(scf.getSclServiceFunctionForwarder())
            .thenReturn(new ArrayList<SclServiceFunctionForwarder>(){{ add(sffClassifier); }});
    }

    @Test
    public void buildFlowKeyNameTest() {
        Assert.assertEquals("abcballoons", handler.buildFlowKeyName("a", "b", "c", "balloons"));
    }

    @Test
    public void getInterfaceNameFromClassifierOK() {
        Optional<String> res = handler.getInterfaceNameFromClassifier(sffClassifier);
        Assert.assertTrue(res.isPresent());
        Assert.assertEquals(INTERFACE_TO_CLASSIFY, res.get());
    }

    @Test
    public void getInterfaceNameFromClassifierWrongAttachmentPoint() {
        when(sffClassifier.getAttachmentPointType()).thenReturn(new BridgeBuilder().build());
        Optional<String> res = handler.getInterfaceNameFromClassifier(sffClassifier);
        Assert.assertFalse(res.isPresent());
    }

    @Test
    public void getInterfaceNameFromClassifierNullClassifier() {
        Optional<String> res = handler.getInterfaceNameFromClassifier(null);
        Assert.assertFalse(res.isPresent());
    }

    @Test
    public void usesLogicalInterfacesLogicalSff() {
        when(sff.getSffDataPlaneLocator()).thenReturn(null);
        Assert.assertTrue(handler.usesLogicalInterfaces(sff));
    }

    @Test
    public void usesLogicalInterfacesLegacySff() {
        when(sff.getSffDataPlaneLocator()).thenReturn(new ArrayList<>());
        Assert.assertFalse(handler.usesLogicalInterfaces(sff));
    }
}
