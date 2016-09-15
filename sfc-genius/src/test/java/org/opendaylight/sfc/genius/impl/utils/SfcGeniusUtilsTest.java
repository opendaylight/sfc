/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.binding.DataObject;


@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusUtilsTest {

    @Mock
    Executor executor;

    @Mock
    DataObject dataObject;

    @Captor
    ArgumentCaptor<Runnable> runnableCaptor;

    @Test(expected = IllegalArgumentException.class)
    public void getDpnIdFromLowerLayerIfListTooManyItems() throws Exception {
        SfcGeniusUtils.getDpnIdFromLowerLayerIfList(Arrays.asList("Item1", "Item2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDpnIdFromLowerLayerIfListBadItem() throws Exception {
        SfcGeniusUtils.getDpnIdFromLowerLayerIfList(Collections.singletonList(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDpnIdFromNullLowerLayerIfList() throws Exception {
        SfcGeniusUtils.getDpnIdFromLowerLayerIfList(null);
    }

}
