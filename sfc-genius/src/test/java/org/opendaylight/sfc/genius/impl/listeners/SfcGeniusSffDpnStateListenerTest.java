/*
 * Copyright (c) 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.genius.impl.SfcGeniusServiceManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.state.ServiceFunctionForwarderState;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.SffLogicalSffAugmentation;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.DpnRsps;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.Dpn;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.RspsForDpnid;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.rsps._for.dpnid.Rsps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusSffDpnStateListenerTest {

    @Mock
    private DataBroker dataBroker;

    @Mock
    private SfcGeniusServiceManager sfcGeniusServiceManager;

    @Mock
    private ExecutorService executorService;

    @Mock
    private Dpn dpn;

    @Mock
    private RspsForDpnid rspsForDpnid;

    @Mock
    private List<Rsps> rspsList;

    private SfcGeniusSffDpnStateListener sfcGeniusSffDpnStateListener;

    @Before
    public void setup() {
        when(dpn.getDpnId()).thenReturn(new DpnIdType(new BigInteger("1")));
        when(dpn.getRspsForDpnid()).thenReturn(rspsForDpnid);
        when(rspsForDpnid.getRsps()).thenReturn(rspsList);
        when(dataBroker.registerDataChangeListener(
                eq(LogicalDatastoreType.OPERATIONAL),
                eq(InstanceIdentifier.create(ServiceFunctionForwardersState.class)
                        .child(ServiceFunctionForwarderState.class)
                        .augmentation(SffLogicalSffAugmentation.class)
                        .child(DpnRsps.class)
                        .child(Dpn.class)),
                any(),
                any()))
                .thenAnswer(Answers.RETURNS_DEEP_STUBS.get());
        sfcGeniusSffDpnStateListener = new SfcGeniusSffDpnStateListener(
                dataBroker,
                sfcGeniusServiceManager,
                executorService);
    }

    @Test
    public void remove() throws Exception {
        when(rspsList.isEmpty()).thenReturn(false);
        sfcGeniusSffDpnStateListener.remove(dpn);
        verify(sfcGeniusServiceManager).unbindNode(new BigInteger("1"));
    }

    @Test
    public void removeNoPaths() throws Exception {
        when(rspsList.isEmpty()).thenReturn(true);
        sfcGeniusSffDpnStateListener.remove(dpn);
        verifyZeroInteractions(sfcGeniusServiceManager);
    }

    @Test
    public void updateAddPaths() throws Exception {
        when(rspsList.isEmpty()).thenReturn(false).thenReturn(true);
        sfcGeniusSffDpnStateListener.update(dpn, dpn);
        verify(sfcGeniusServiceManager).bindNode(new BigInteger("1"));
    }

    @Test
    public void updateAddRemoveSomePaths() throws Exception {
        when(rspsList.isEmpty()).thenReturn(false).thenReturn(false);
        sfcGeniusSffDpnStateListener.update(dpn, dpn);
        verifyZeroInteractions(sfcGeniusServiceManager);
    }

    @Test
    public void updateRemovePaths() throws Exception {
        when(rspsList.isEmpty()).thenReturn(true).thenReturn(false);
        sfcGeniusSffDpnStateListener.update(dpn, dpn);
        verify(sfcGeniusServiceManager).unbindNode(new BigInteger("1"));
    }

    @Test
    public void add() throws Exception {
        when(rspsList.isEmpty()).thenReturn(false);
        sfcGeniusSffDpnStateListener.add(dpn);
        verify(sfcGeniusServiceManager).bindNode(new BigInteger("1"));
    }

    @Test
    public void addNoPaths() throws Exception {
        when(rspsList.isEmpty()).thenReturn(true);
        sfcGeniusSffDpnStateListener.add(dpn);
        verifyZeroInteractions(sfcGeniusServiceManager);
    }

}
