/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc.vnfm.spi;

import java.math.BigInteger;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.ServiceStatistics;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.ServiceStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.ZeroBasedCounter64;

public class SfcVnfmDummyImpl implements SfcVnfmSpi {

    @Override
    public boolean createSf(ServiceFunctionType sfType) {
        if (sfType.isSymmetry() != null)
            return sfType.isSymmetry();
        return false;
    }

    @Override
    public boolean deleteSf(ServiceFunction sf) {
        return true;
    }

    @Override
    public ServiceStatistics getSfStatistics(ServiceFunction sf) {
        ServiceStatistics ss =
                new ServiceStatisticsBuilder().setPacketsIn(new ZeroBasedCounter64(new BigInteger("100"))).build();
        return ss;
    }

}
