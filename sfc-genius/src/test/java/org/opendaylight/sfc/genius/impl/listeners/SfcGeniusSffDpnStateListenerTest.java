/*
 * Copyright (c) 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.sfc.genius.impl.SfcGeniusServiceManager;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.Dpn;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.RspsForDpnid;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.dpnid.rsps.dpn.rsps.dpn.rsps._for.dpnid.Rsps;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusSffDpnStateListenerTest {

    @Mock
    SfcGeniusServiceManager sfcGeniusServiceManager;

    @Mock
    Executor executor;

    @Captor
    ArgumentCaptor<Runnable> runnableCaptor;

    @Mock
    Dpn dpn;

    @Mock
    RspsForDpnid rspsForDpnid;

    @Mock
    List<Rsps> rspsList;

    SfcGeniusSffDpnStateListener sfcGeniusSffDpnStateListener;

    @Before
    public void setup() {
        when(dpn.getDpnId()).thenReturn(new DpnIdType(new BigInteger("1")));
        when(dpn.getRspsForDpnid()).thenReturn(rspsForDpnid);
        when(rspsForDpnid.getRsps()).thenReturn(rspsList);
        sfcGeniusSffDpnStateListener = new SfcGeniusSffDpnStateListener(sfcGeniusServiceManager, executor);
    }

    @Test
    public void remove() throws Exception {
        when(rspsList.isEmpty()).thenReturn(false);
        sfcGeniusSffDpnStateListener.remove(null, dpn);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(sfcGeniusServiceManager).unbindNode(new BigInteger("1"));
    }

    @Test
    public void removeNoPaths() throws Exception {
        when(rspsList.isEmpty()).thenReturn(true);
        sfcGeniusSffDpnStateListener.remove(null, dpn);
        verifyZeroInteractions(executor);
        verifyZeroInteractions(sfcGeniusServiceManager);
    }

    @Test
    public void updateAddPaths() throws Exception {
        when(rspsList.isEmpty()).thenReturn(false).thenReturn(true);
        sfcGeniusSffDpnStateListener.update(null, dpn, dpn);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(sfcGeniusServiceManager).bindNode(new BigInteger("1"));
    }

    @Test
    public void updateAddRemoveSomePaths() throws Exception {
        when(rspsList.isEmpty()).thenReturn(false).thenReturn(false);
        sfcGeniusSffDpnStateListener.update(null, dpn, dpn);
        verifyZeroInteractions(executor);
        verifyZeroInteractions(sfcGeniusServiceManager);
    }

    @Test
    public void updateRemovePaths() throws Exception {
        when(rspsList.isEmpty()).thenReturn(true).thenReturn(false);
        sfcGeniusSffDpnStateListener.update(null, dpn, dpn);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(sfcGeniusServiceManager).unbindNode(new BigInteger("1"));
    }

    @Test
    public void add() throws Exception {
        when(rspsList.isEmpty()).thenReturn(false);
        sfcGeniusSffDpnStateListener.add(null, dpn);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(sfcGeniusServiceManager).bindNode(new BigInteger("1"));
    }

    @Test
    public void addNoPaths() throws Exception {
        when(rspsList.isEmpty()).thenReturn(true);
        sfcGeniusSffDpnStateListener.add(null, dpn);
        verifyZeroInteractions(executor);
        verifyZeroInteractions(sfcGeniusServiceManager);
    }

}
