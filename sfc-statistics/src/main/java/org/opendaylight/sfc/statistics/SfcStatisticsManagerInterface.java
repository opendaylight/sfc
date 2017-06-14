/**
 * Copyright (c) 2017 Inocybe Technologies Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfTableOffsets;

public interface SfcStatisticsManagerInterface {

    void scheduleRspStatistics(RenderedServicePath rsp, SfcOfTableOffsets sfcOfTableOffsets);

    void scheduleSfStatistics(ServiceFunction sf, SfcOfTableOffsets sfcOfTableOffsets);

    void scheduleSffStatistics(ServiceFunctionForwarder sff, SfcOfTableOffsets sfcOfTableOffsets);

    // Call to signal that the statistics configuration has changed
    void updateSfcStatisticsConfiguration();
}
