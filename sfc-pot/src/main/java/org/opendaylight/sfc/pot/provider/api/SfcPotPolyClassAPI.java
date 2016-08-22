/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

/**
 * This class is used to store PoT configuration for SFC .
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.pot.provider.api.SfcPotPolyClassAPI
 * @since 2016-05-01
 */
public class SfcPotPolyClassAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotPolyAPI.class);

    private long numprofiles;
    private long startindex;

    public Map<String, List<SfcPotPolyClass>> polyClassMap;

    public SfcPotPolyClassAPI() {
        polyClassMap = new HashMap<>();
    }

    //
    // Get PolyClassList given the rspName
    //
    public List<SfcPotPolyClass> getPolyClassList(String rspName) {
        if (polyClassMap != null) {
            return (polyClassMap.get(rspName));
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
        return this.numprofiles;
    }

    public long getStartIndex() {
        return this.startindex;
    }

    public void setNumProfiles(long numProfiles) {
        this.numprofiles = numProfiles;
    }

    public void setStartIndex(long startIndex) {
        this.startindex = startIndex;
    }

    public void deleteRsp(String rspName) {
        if (polyClassMap != null) {
            polyClassMap.remove(rspName);
        }
    }

}
