/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider;


import io.netty.util.Timeout;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.TimeResolution;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;

public class SfcPotTimerData {
    private static final SfcPotTimerData sfcPotTimerDataInstance = new SfcPotTimerData();

    private static class SfcPotRspInfo {
        Long      configSendTimerValue;
        int       currActiveIndex;
        SfcPotTimerTask potTimerTask;
        Timeout         potTimeout;
        SfcPotRspInfo (String rspName, Long configVal, int Idx,
                       SfcPotTimerTask potTimerTask, Timeout potTimeout) {
            this.configSendTimerValue = configVal;
            this.currActiveIndex = Idx;
            this.potTimerTask = potTimerTask;
            this.potTimeout = potTimeout;
        }
    }

    private Map<String, SfcPotRspInfo> potRspInfoStore;

    private SfcPotTimerData () {
        potRspInfoStore = new HashMap<>();
    }

    public static SfcPotTimerData getInstance() {
        return sfcPotTimerDataInstance;
    }

    public boolean isRspDataPresent (RspName rspName) {
        if (potRspInfoStore == null) return false;

        if (potRspInfoStore.get(rspName.getValue()) != null) {
             return true;
        }

        return false;
    }

    public boolean addRspData (RspName rspName, Long refreshPeriodValue,
                               final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                               int Idx,
                               SfcPotTimerTask potTimerTask, Timeout potTimeout) {
        SfcPotRspInfo potRspInfo;

        if (potRspInfoStore == null) return false;

        potRspInfo = new SfcPotRspInfo(rspName.getValue(),
                                       refreshPeriodValue,
                                       Idx,
                                       potTimerTask, potTimeout);

        potRspInfoStore.put(rspName.getValue(), potRspInfo);

        return true;
    }

    public void delRspData (RspName rspName) {
        if (potRspInfoStore == null) return;

        potRspInfoStore.remove(rspName.getValue());
    }

    public Long getRspDataConfigRefreshValue (RspName rspName) {
        Long ret = Long.valueOf(0);
        SfcPotRspInfo potRspInfo = null;

        if (potRspInfoStore == null) return ret;

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            return potRspInfo.configSendTimerValue;
        }

        return ret;
    }

    public int getRspDataConfigActiveIndex (RspName rspName) {
        int ret = 0;
        SfcPotRspInfo potRspInfo = null;

        if (potRspInfoStore == null) return ret;

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            return potRspInfo.currActiveIndex;
        }

        return ret;
    }

    public boolean setRspDataConfigActiveIndex (RspName rspName, int newActiveIndex) {
        boolean ret = false;
        SfcPotRspInfo potRspInfo = null;

        if (potRspInfoStore == null) return ret;

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            potRspInfo.currActiveIndex = newActiveIndex;
            potRspInfoStore.put(rspName.getValue(), potRspInfo);
        }

        return ret;
    }

    public SfcPotTimerTask getRspDataTimerTask (RspName rspName) {
        SfcPotTimerTask ret = null;
        SfcPotRspInfo potRspInfo = null;

        if (potRspInfoStore == null) return ret;

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            return potRspInfo.potTimerTask;
        }

        return ret;
    }

    public Timeout getRspDataTimeout (RspName rspName) {
        Timeout ret = null;
        SfcPotRspInfo potRspInfo = null;

        if (potRspInfoStore == null) return ret;

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            return potRspInfo.potTimeout;
        }

        return ret;
    }

    public void setRspDataTimeout (RspName rspName, Timeout potTimeout) {
        SfcPotRspInfo potRspInfo = null;

        if (potRspInfoStore == null) return;

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            potRspInfo.potTimeout = potTimeout;
            potRspInfoStore.put(rspName.getValue(), potRspInfo);
        }

        return;
    }
}
