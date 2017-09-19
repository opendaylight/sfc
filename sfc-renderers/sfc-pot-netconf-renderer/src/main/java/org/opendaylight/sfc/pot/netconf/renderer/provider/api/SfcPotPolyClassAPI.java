/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.provider.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to store PoT configuration for SFC .
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @since 2016-05-01
 */
public class SfcPotPolyClassAPI {
    private long numProfiles;
    private long startIndex;

    private final Map<String, List<SfcPotPolyClass>> polyClassMap;

    public SfcPotPolyClassAPI() {
        polyClassMap = new HashMap<>();
    }

    public List<SfcPotPolyClass> getPolyClassList(String rspName) {
        if (polyClassMap != null) {
            return polyClassMap.get(rspName);
        }

        return null;
    }

    public void putPolyClassList(String rspName, List<SfcPotPolyClass> polyClassList) {
        if (polyClassMap != null) {
            polyClassMap.remove(rspName);

            polyClassMap.put(rspName, polyClassList);
        }
    }

    public long getNumProfiles() {
        return this.numProfiles;
    }

    public long getStartIndex() {
        return this.startIndex;
    }

    public void setNumProfiles(long numProfiles) {
        this.numProfiles = numProfiles;
    }

    public void setStartIndex(long startIndex) {
        this.startIndex = startIndex;
    }

    public void deleteRsp(String rspName) {
        if (polyClassMap != null) {
            polyClassMap.remove(rspName);
        }
    }
}
