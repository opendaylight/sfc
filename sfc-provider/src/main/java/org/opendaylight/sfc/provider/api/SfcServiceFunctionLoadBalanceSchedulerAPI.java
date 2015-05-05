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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.LoadBalance;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * This class implements load balance scheduling mode.
 * <p>
 *
 * @author Shuqiang Zhao (shuqiangx.zhao@intel.com)
 * @version 0.1
 * <p>
 * @since 2015-03-13
 */
public class SfcServiceFunctionLoadBalanceSchedulerAPI extends SfcServiceFunctionSchedulerAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionLoadBalanceSchedulerAPI.class);
    SfcServiceFunctionLoadBalanceSchedulerAPI() {
        super.setSfcServiceFunctionSchedulerType(LoadBalance.class);
    }

    private String getServiceFunctionByType(ServiceFunctionType serviceFunctionType) {
        boolean ret = false;
        List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        String sfName = null;
        String sftServiceFunctionName = null;
        java.lang.Long preCPUUtilization = java.lang.Long.MAX_VALUE;

        for (SftServiceFunctionName curSftServiceFunctionName : sftServiceFunctionNameList){
            sfName = curSftServiceFunctionName.getName();

            /* Check next one if curSftServiceFunctionName doesn't exist */
            ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(sfName);
            if (serviceFunction == null) {
                LOG.error("ServiceFunction {} doesn't exist", sfName);
                continue;
            }

            /* Update ServiceFunctionMonitor information */
            ret = SfcProviderServiceFunctionAPI.putServiceFunctionMonitorExecutor(serviceFunction);
            if (ret == false) {
                LOG.error("Put monitor information to Data Store failed! serviceFunction: {}", sfName);
            }

            /* Read ServiceFunctionMonitor information */
            SfcSfDescMon sfcSfDescMon = SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitorExecutor(sfName);
            if (sfcSfDescMon == null) {
                sftServiceFunctionName = sfName;
                LOG.error("Read monitor information from Data Store failed! serviceFunction: {}", sfName);
                // Use sfName if no sfcSfDescMon is available
                break;
            }

            java.lang.Long curCPUUtilization = sfcSfDescMon.getMonitoringInfo().getResourceUtilization().getCPUUtilization();

            if (preCPUUtilization > curCPUUtilization){
                preCPUUtilization = curCPUUtilization;
                sftServiceFunctionName = sfName;
            }
        }

        if (sftServiceFunctionName == null) {
            LOG.error("Failed to get one available ServiceFunction for {}", serviceFunctionType.getType().getSimpleName());
        }

        return sftServiceFunctionName;
    }

    public List<String> scheduleServiceFunctions(ServiceFunctionChain chain, int serviceIndex) {
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

        return sfNameList;
    }
}
