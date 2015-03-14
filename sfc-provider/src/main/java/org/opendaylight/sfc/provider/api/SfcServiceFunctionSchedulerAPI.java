/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;

import java.util.Comparator;
import java.util.List;

/**
 * This class defines the Generic API for SF scheduling.
 * <p/>
 *
 * @author Johnson Li (johnson.li@intel.com)
 * @version 0.1
 * <p/>
 * @since 2015-03-04
 */
public abstract class SfcServiceFunctionSchedulerAPI {
    static final Comparator<SfcServiceFunction> SF_ORDER =
            new Comparator<SfcServiceFunction>() {
                public int compare(SfcServiceFunction e1, SfcServiceFunction e2) {
                    return e2.getOrder().compareTo(e1.getOrder());
                }
            };

    static final Comparator<SfcServiceFunction> SF_ORDER_REV =
            new Comparator<SfcServiceFunction>() {
                public int compare(SfcServiceFunction e1, SfcServiceFunction e2) {
                    return e1.getOrder().compareTo(e2.getOrder());
                }
            };

    public enum SfcServiceFunctionSchedulerType {
        ROUND_ROBIN, RANDOM
    }
    private SfcServiceFunctionSchedulerType sfcServiceFunctionSchedulerType;

    public SfcServiceFunctionSchedulerType getSfcServiceFunctionSchedulerType() {
        return this.sfcServiceFunctionSchedulerType;
    }

    public void setSfcServiceFunctionSchedulerType(SfcServiceFunctionSchedulerType schedulerType) {
        this.sfcServiceFunctionSchedulerType = schedulerType;
    }

    abstract public List<String> scheduleServiceFuntions(ServiceFunctionChain chain, int serviceIndex);
}
