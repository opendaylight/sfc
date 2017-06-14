/**
 * Copyright (c) 2017 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.statistics.utils;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcStatisticsWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SfcStatisticsWriter.class);

    public static void writeRspStatistics(InstanceIdentifier<RenderedServicePath> rspStatsIid,
            RenderedServicePath rsp) {

        LOG.debug("SfcStatisticsWriter::writeRspStatistics rsp: [{}, {}]", rsp.getName().getValue(), rsp.getPathId());
        if (!SfcDataStoreAPI.writeMergeTransactionAPI(rspStatsIid, rsp, LogicalDatastoreType.OPERATIONAL)) {
            LOG.error("{}: Failed to write Rendered Service Path stats", Thread.currentThread().getStackTrace()[1]);
        }
    }

    public static void writeSfStatistics(InstanceIdentifier<ServiceFunction> sfStatsIid, ServiceFunction sf) {
        // Not implemented for now
    }

    public static void writeSffStatistics(InstanceIdentifier<ServiceFunctionForwarder> sffStatsIid,
                                         ServiceFunctionForwarder sff) {
        // Not implemented for now
    }
}
