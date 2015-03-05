/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
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
    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionSchedulerAPI.class);
    public enum SfcServiceFunctionSchedulerType {
        ROUND_ROBIN, RANDOM;
    }
    private SfcServiceFunctionSchedulerType sfcServiceFunctionSchedulerType;

    public SfcServiceFunctionSchedulerType getSfcServiceFunctionSchedulerType() {
        return this.sfcServiceFunctionSchedulerType;
    }

    public void setSfcServiceFunctionSchedulerType(SfcServiceFunctionSchedulerType schedulerType) {
        this.sfcServiceFunctionSchedulerType = schedulerType;
    }

    protected List<RenderedServicePathHop> createRenderedServicePathHopList(List<String> serviceFunctionNameList, int serviceIndex) {
        List<RenderedServicePathHop> renderedServicePathHopArrayList = new ArrayList<>();
        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();

        short posIndex = 0;

        if (serviceFunctionNameList != null) {
            for (String serviceFunctionName : serviceFunctionNameList) {
                ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(serviceFunctionName);
                if (serviceFunction != null) {
                    renderedServicePathHopBuilder.setHopNumber(posIndex)
                            .setServiceFunctionName(serviceFunctionName)
                            .setServiceIndex((short) serviceIndex)
                            .setServiceFunctionForwarder(serviceFunction.getSfDataPlaneLocator()
                            .get(0)
                            .getServiceFunctionForwarder());
                    renderedServicePathHopArrayList.add(posIndex, renderedServicePathHopBuilder.build());
                    serviceIndex--;
                    posIndex++;
                } else {
                    LOG.error("Could not find suitable SF in data store by name: {}", serviceFunctionName);
                    return null;
                }
            }
            return renderedServicePathHopArrayList;
        } else {
            LOG.error("Could not create the hop list caused by empty name list");
            return null;
        }
    }

    abstract public List<RenderedServicePathHop> scheduleServiceFuntions(ServiceFunctionChain chain, int serviceIndex);
}
