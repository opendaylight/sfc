/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;


/**
 * This class defines the Generic API for SF scheduling.
 * <p>
 *
 * @author Johnson Li (johnson.li@intel.com)
 * @version 0.1
 *          <p>
 * @since 2015-03-04
 */
public abstract class SfcServiceFunctionSchedulerAPI {

    private java.lang.Class<? extends org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity> sfcServiceFunctionSchedulerType;

    public java.lang.Class<? extends org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity> getSfcServiceFunctionSchedulerType() {
        return this.sfcServiceFunctionSchedulerType;
    }

    public void setSfcServiceFunctionSchedulerType(
            java.lang.Class<? extends org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypeIdentity> schedulerType) {
        this.sfcServiceFunctionSchedulerType = schedulerType;
    }

    abstract public List<SfName> scheduleServiceFunctions(ServiceFunctionChain chain, int serviceIndex,
            ServiceFunctionPath sfp);

    protected Map<Short, SfName> getSFPHopSfMapping(ServiceFunctionPath sfp) {
        Map<Short, SfName> ret = new HashMap<>();
        List<ServicePathHop> hops = sfp.getServicePathHop();
        if (hops != null) {
            for (ServicePathHop hop : hops) {
                ret.put(hop.getHopNumber(), hop.getServiceFunctionName());
            }
        }
        return ret;
    }
}
