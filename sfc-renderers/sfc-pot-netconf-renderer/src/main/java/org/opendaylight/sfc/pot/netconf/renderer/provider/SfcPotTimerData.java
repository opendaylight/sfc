/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.provider;

import io.netty.util.Timeout;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.TimeResolution;

/**
 * This class is used to store iOAM PoT and RSP meta data in the timer context.
 *
 * <p>
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @since 2016-12-01
 */
public final class SfcPotTimerData {
    private static final SfcPotTimerData SFC_POT_TIMER_DATA_INSTANCE = new SfcPotTimerData();

    private static class SfcPotRspInfo {
        Long configSendTimerValue;
        int currActiveIndex;
        SfcPotTimerTask potTimerTask;
        Timeout potTimeout;
        int sfcSize;

        SfcPotRspInfo(Long configVal, int currActiveIndex, int sfcSize, SfcPotTimerTask potTimerTask,
                Timeout potTimeout) {
            this.configSendTimerValue = configVal;
            this.currActiveIndex = currActiveIndex;
            this.sfcSize = sfcSize;
            this.potTimerTask = potTimerTask;
            this.potTimeout = potTimeout;
        }
    }

    private final Map<String, SfcPotRspInfo> potRspInfoStore;

    private SfcPotTimerData() {
        potRspInfoStore = new HashMap<>();
    }

    public static SfcPotTimerData getInstance() {
        return SFC_POT_TIMER_DATA_INSTANCE;
    }

    public boolean isRspDataPresent(RspName rspName) {
        return potRspInfoStore != null && potRspInfoStore.get(rspName.getValue()) != null;
    }

    public boolean addRspData(RspName rspName, Long refreshPeriodValue,
            final Class<? extends TimeResolution> refreshPeriodTimeUnits, int currActiveIndex, int sfcSize,
            SfcPotTimerTask potTimerTask, Timeout potTimeout) {
        SfcPotRspInfo potRspInfo;

        if (potRspInfoStore == null) {
            return false;
        }

        potRspInfo = new SfcPotRspInfo(refreshPeriodValue, currActiveIndex, sfcSize, potTimerTask, potTimeout);

        potRspInfoStore.put(rspName.getValue(), potRspInfo);

        return true;
    }

    public void delRspData(RspName rspName) {
        if (potRspInfoStore == null) {
            return;
        }

        potRspInfoStore.remove(rspName.getValue());
    }

    public Long getRspDataConfigRefreshValue(RspName rspName) {
        Long ret = 0L;
        SfcPotRspInfo potRspInfo;

        if (potRspInfoStore == null) {
            return ret;
        }

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            return potRspInfo.configSendTimerValue;
        }

        return ret;
    }

    public int getRspDataConfigActiveIndex(RspName rspName) {
        SfcPotRspInfo potRspInfo;

        if (potRspInfoStore == null) {
            return 0;
        }

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            return potRspInfo.currActiveIndex;
        }

        return 0;
    }

    public int getRspDataSfcSize(RspName rspName) {
        SfcPotRspInfo potRspInfo;

        if (potRspInfoStore == null) {
            return 0;
        }

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            return potRspInfo.sfcSize;
        }

        return 0;
    }

    public boolean setRspDataConfigActiveIndex(RspName rspName, int newActiveIndex) {
        SfcPotRspInfo potRspInfo;

        if (potRspInfoStore == null) {
            return false;
        }

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            potRspInfo.currActiveIndex = newActiveIndex;
            potRspInfoStore.put(rspName.getValue(), potRspInfo);
            return true;
        }

        return false;
    }

    public SfcPotTimerTask getRspDataTimerTask(RspName rspName) {
        SfcPotRspInfo potRspInfo;

        if (potRspInfoStore == null) {
            return null;
        }

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            return potRspInfo.potTimerTask;
        }

        return null;
    }

    public Timeout getRspDataTimeout(RspName rspName) {
        SfcPotRspInfo potRspInfo;

        if (potRspInfoStore == null) {
            return null;
        }

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            return potRspInfo.potTimeout;
        }

        return null;
    }

    public void setRspDataTimeout(RspName rspName, Timeout potTimeout) {
        SfcPotRspInfo potRspInfo;

        if (potRspInfoStore == null) {
            return;
        }

        potRspInfo = potRspInfoStore.get(rspName.getValue());
        if (potRspInfo != null) {
            potRspInfo.potTimeout = potTimeout;
            potRspInfoStore.put(rspName.getValue(), potRspInfo);
        }
    }
}
