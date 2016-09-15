/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.utils;

import java.math.BigInteger;
import java.util.List;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;


public class SfcGeniusUtils {

    /**
     * Extracts the data plane node Id from a lowerlayer interface list by
     * means of {#MDSALUtil::getDpnIdFromLowerLayerIfList} performing extra
     * checks on the input argument.
     *
     * @param lowerLayerIfList to extract the data plane node Id from.
     * @return the data plane node Id.
     * @throws IllegalArgumentException if the input list does not contain
     * one item only, or if the format of the item is invalid.
     */
    public static BigInteger getDpnIdFromLowerLayerIfList(List<String> lowerLayerIfList)
            throws IllegalArgumentException {
        if (lowerLayerIfList == null || lowerLayerIfList.size() != 1) {
            throw new IllegalArgumentException("Expected 1 and only 1 item in lower layer interface list");
        }
        long nodeId = MDSALUtil.getDpnIdFromPortName(new NodeConnectorId(lowerLayerIfList.get(0)));
        if (nodeId < 0L) {
            throw new IllegalArgumentException("Unexpected format of lower layer interface list");
        }
        return BigInteger.valueOf(nodeId);
    }
}
