/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.logicalclassifier;


import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import java.util.Optional;

public class LogicalClassifierDataGetter {

    SfcGeniusRpcClient theGeniusRpcClient;

    private LogicalClassifierDataGetter() {}

    public LogicalClassifierDataGetter(SfcGeniusRpcClient theRpcClient) {
        theGeniusRpcClient = theRpcClient;
    }

    /**
     * Get the openflow port associated to a logical interface
     * @param theInterfaceName  the name of the interface
     * @return                  the openflow port number
     */
    public static Optional<Long> getOpenflowPort(String theInterfaceName) {
        return SfcGeniusDataUtils.getInterfaceLowerLayerIf(theInterfaceName)
                .stream()
                .map(str -> str.split(":"))
                .filter(strArray -> strArray.length == 3)
                .map(strArray -> strArray[2])
                .map(Long::parseLong)
                .findFirst();
    }
}
