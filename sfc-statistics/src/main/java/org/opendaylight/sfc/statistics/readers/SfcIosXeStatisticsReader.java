/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.readers;

import java.util.Optional;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.statistic.fields.ServiceStatistic;

public class SfcIosXeStatisticsReader extends SfcStatisticsReaderBase {

    public SfcIosXeStatisticsReader(ServiceFunctionForwarder sff) {
    }

    @Override
    public Optional<ServiceStatistic>
        getNextHopStatistics(boolean inputStats, ServiceFunctionForwarder sff, long nsp, short nsi) {
        // Not implemented yet
        return Optional.empty();
    }

    @Override
    public Optional<ServiceStatistic> getTransportIngressStatistics(ServiceFunctionForwarder sff) {
        // Not implemented yet
        return Optional.empty();
    }

    @Override
    public Optional<ServiceStatistic> getTransportEgressStatistics(ServiceFunctionForwarder sff) {
        // Not implemented yet
        return Optional.empty();
    }

}
