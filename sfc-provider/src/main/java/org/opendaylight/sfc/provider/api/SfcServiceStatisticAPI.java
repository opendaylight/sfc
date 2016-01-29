/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcServiceStatisticAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceStatisticAPI.class);

    public class ServiceStatistic {

        private List<Rsp> rsp;
        private long timestamp;

        public List<Rsp> getRsp() {
            return this.rsp;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public class Rsp {

        private List<Sff> sff;
        private List<Sf> sf;
        private SfcName sfcName;
        private long bytesIn;
        private long bytesOut;
        private long packetsIn;
        private long packetsOut;

        public List<Sff> getSff() {
            return sff;
        }

        public void setSff(List<Sff> sff) {
            this.sff = sff;
        }

        public List<Sf> getSf() {
            return sf;
        }

        public void setSf(List<Sf> sf) {
            this.sf = sf;
        }

        public SfcName getSfcName() {
            return sfcName;
        }

        public void setSfcName(SfcName sfcName) {
            this.sfcName = sfcName;
        }

        public long getBytesIn() {
            return bytesIn;
        }

        public void setBytesIn(long bytesIn) {
            this.bytesIn = bytesIn;
        }

        public long getBytesOut() {
            return bytesOut;
        }

        public void setBytesOut(long bytesOut) {
            this.bytesOut = bytesOut;
        }

        public long getPacketsIn() {
            return packetsIn;
        }

        public void setPacketsIn(long packetsIn) {
            this.packetsIn = packetsIn;
        }

        public long getPacketsOut() {
            return packetsOut;
        }

        public void setPacketsOut(long packetsOut) {
            this.packetsOut = packetsOut;
        }


    }

    public class Sff {

        private SffName sffName;
        private long bytesIn;
        private long bytesOut;
        private long packetsIn;
        private long packetsOut;

        public SffName getSfcName() {
            return sffName;
        }

        public void setSfcName(SffName sffName) {
            this.sffName = sffName;
        }

        public long getBytesIn() {
            return bytesIn;
        }

        public void setBytesIn(long bytesIn) {
            this.bytesIn = bytesIn;
        }

        public long getBytesOut() {
            return bytesOut;
        }

        public void setBytesOut(long bytesOut) {
            this.bytesOut = bytesOut;
        }

        public long getPacketsIn() {
            return packetsIn;
        }

        public void setPacketsIn(long packetsIn) {
            this.packetsIn = packetsIn;
        }

        public long getPacketsOut() {
            return packetsOut;
        }

        public void setPacketsOut(long packetsOut) {
            this.packetsOut = packetsOut;
        }

    }

    public class Sf {

        private SfName sfName;
        private long bytesIn;
        private long bytesOut;
        private long packetsIn;
        private long packetsOut;

        public SfName getSfName() {
            return sfName;
        }

        public void setSfName(SfName sfName) {
            this.sfName = sfName;
        }

        public long getBytesIn() {
            return bytesIn;
        }

        public void setBytesIn(long bytesIn) {
            this.bytesIn = bytesIn;
        }

        public long getBytesOut() {
            return bytesOut;
        }

        public void setBytesOut(long bytesOut) {
            this.bytesOut = bytesOut;
        }

        public long getPacketsIn() {
            return packetsIn;
        }

        public void setPacketsIn(long packetsIn) {
            this.packetsIn = packetsIn;
        }

        public long getPacketsOut() {
            return packetsOut;
        }

        public void setPacketsOut(long packetsOut) {
            this.packetsOut = packetsOut;
        }

    }

    public static void writeStats(ServiceStatistic statistic, DataBroker broker) {
        if (statistic == null) {
            LOG.info("Statistic empy. Skipping processing");
            return;
        }
        LOG.debug("Processing statistic {}", statistic);

    }
}
