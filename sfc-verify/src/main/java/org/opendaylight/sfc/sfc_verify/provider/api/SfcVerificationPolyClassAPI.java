/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

/**
 * This class is used to store south-bound configuration for SFC verification.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationPolyClassAPI
 * @since 2016-05-01
 */
public class SfcVerificationPolyClassAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcVerificationPolyAPI.class);

    private long _numprofiles;
    private long _startindex;
    private long _profilesvalidator;

    private long _sbnumprofiles;
    private long _sbstartindex;

    private static SfcVerificationPolyClassAPI api;

    public Map<String, List<SfcVerificationPolyClass>> polyClassMap;

    private SfcVerificationPolyClassAPI() {
        polyClassMap = new HashMap<>();
    }

    static {
        //Singleton instance via static blocks.
        api = new SfcVerificationPolyClassAPI();
    }

    public static SfcVerificationPolyClassAPI getSfcVerificationPolyClassAPI() {
        return api;
    }

    //
    // Get PolyClassList given the rspName
    //
    public List<SfcVerificationPolyClass> getPolyClassList(String rspName) {
        if (polyClassMap != null) {
            return (polyClassMap.get(rspName));
        }
        return null;
    }

    public void putPolyClassList(String rspName, List<SfcVerificationPolyClass> polyClassList) {
        if (polyClassMap != null) {
            polyClassMap.remove(rspName);

            polyClassMap.put(rspName, polyClassList);
        }
    }

    public long getNumProfiles() {
        return this._numprofiles;
    }

    public long getStartIndex() {
        return this._startindex;
    }

    public long getNumSBProfiles() {
        return this._sbnumprofiles;
    }

    public long getStartSBIndex() {
        return this._sbstartindex;
    }

    public long getProfilesValidator() {
        return this._profilesvalidator;
    }

    public void setNumProfiles(long numProfiles) {
        this._numprofiles = numProfiles;
    }

    public void setStartIndex(long startIndex) {
        this._startindex = startIndex;
    }

    public void setNumSBProfiles(long numProfiles) {
        this._sbnumprofiles = numProfiles;
    }

    public void setStartSBIndex(long startIndex) {
        this._sbstartindex = startIndex;
    }

    public void setProfilesValidator(long renewTtl) {
        this._profilesvalidator= renewTtl;
    }

    public void deleteRsp(String rspName) {
        if (polyClassMap != null) {
            polyClassMap.remove(rspName);
        }
    }

}
