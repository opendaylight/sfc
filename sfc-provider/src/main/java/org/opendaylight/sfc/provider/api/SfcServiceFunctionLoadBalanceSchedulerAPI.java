/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * This class implements load balance scheduling mode.
 * <p/>
 *
 * @author Shuqiang Zhao (shuqiangx.zhao@intel.com)
 * @version 0.1
 * <p/>
 * @since 2015-03-13
 */
public class SfcServiceFunctionLoadBalanceSchedulerAPI extends SfcServiceFunctionSchedulerAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionLoadBalanceSchedulerAPI.class);
    SfcServiceFunctionLoadBalanceSchedulerAPI() {
        super.setSfcServiceFunctionSchedulerType(SfcServiceFunctionSchedulerType.LOAD_BALANCE);
    }

    private String getServiceFunctionByType(ServiceFunctionType serviceFunctionType) {
        List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        String sftServiceFunctionName = sftServiceFunctionNameList.get(0).getName();

        if (SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitorExecutor(sftServiceFunctionName) != null){
            java.lang.Long preCPUUtilization = SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitorExecutor(sftServiceFunctionName)
                                                   .getMonitoringInfo()
                                                   .getResourceUtilization()
                                                   .getCPUUtilization();

            for (SftServiceFunctionName curSftServiceFunctionName : sftServiceFunctionNameList){
                java.lang.Long curCPUUtilization = SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitorExecutor(curSftServiceFunctionName.getName())
                                            .getMonitoringInfo()
                                            .getResourceUtilization()
                                            .getCPUUtilization();

                if (preCPUUtilization > curCPUUtilization){
                    preCPUUtilization = curCPUUtilization;
                    sftServiceFunctionName = curSftServiceFunctionName.getName();
                }
            }
        }
        return sftServiceFunctionName;
    }

    public List<RenderedServicePathHop> scheduleServiceFuntions(ServiceFunctionChain chain, int serviceIndex) {
        List<String> sfNameList = new ArrayList<>();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(chain.getSfcServiceFunction());

        /*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         */
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.debug("SfcServiceFunctionLoadBalanceSchedulerAPI ServiceFunction name: {}", sfcServiceFunction.getName());

            /*
             * We iterate thorough the list of service function types and for each one we try to get
             * get a suitable Service Function. WE need to perform lots of checking to make sure
             * we do not hit NULL Pointer exceptions
             */

            ServiceFunctionType serviceFunctionType;
            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(sfcServiceFunction.getType());
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    String sfName = getServiceFunctionByType(serviceFunctionType);
                    sfNameList.add(sfName);
                } else {
                    LOG.error("Could not create path because there are no configured SFs of type: {}",
                            sfcServiceFunction.getType());
                    return null;
                }
            } else {
                LOG.error("Could not create path because there are no configured SFs of type: {}",
                        sfcServiceFunction.getType());
                return null;
            }
        }

        return super.createRenderedServicePathHopList(sfNameList, serviceIndex);
    }
}
