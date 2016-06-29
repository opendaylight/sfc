/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.RoundRobin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements a round robin SF scheduling mode.
 * <p>
 *
 * @author Johnson Li (johnson.li@intel.com)
 * @version 0.1
 *          <p>
 * @since 2015-03-04
 */
public class SfcServiceFunctionRoundRobinSchedulerAPI extends SfcServiceFunctionSchedulerAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionRoundRobinSchedulerAPI.class);

    private static Map<SftTypeName, Integer> mapCountRoundRobin = new HashMap<>();

    SfcServiceFunctionRoundRobinSchedulerAPI() {
        super.setSfcServiceFunctionSchedulerType(RoundRobin.class);
    }

    private SfName getServiceFunctionByType(ServiceFunctionType serviceFunctionType) {
        List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        int countRoundRobin = 0;

        if (mapCountRoundRobin.size() != 0) {
            for (SftTypeName sfType : mapCountRoundRobin.keySet()) {
                if (sfType.equals(serviceFunctionType.getType())) {
                    countRoundRobin = mapCountRoundRobin.get(sfType);
                    LOG.debug("countRoundRobin: {}", countRoundRobin);
                    break;
                }
            }
        }

        SftServiceFunctionName sftServiceFunctionName = sftServiceFunctionNameList.get(countRoundRobin);
        countRoundRobin = (countRoundRobin + 1) % sftServiceFunctionNameList.size();
        mapCountRoundRobin.put(serviceFunctionType.getType(), countRoundRobin);
        return new SfName(sftServiceFunctionName.getName());
    }

    @Override
    public List<SfName> scheduleServiceFunctions(ServiceFunctionChain chain, int serviceIndex,
            ServiceFunctionPath sfp) {
        List<SfName> sfNameList = new ArrayList<>();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(chain.getSfcServiceFunction());
        short index = 0;
        Map<Short, SfName> sfpMapping = getSFPHopSfMapping(sfp);

        /*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         */
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.info("ServiceFunction name: {}", sfcServiceFunction.getName());
            SfName hopSf = sfpMapping.get(index++);
            if (hopSf != null) {
                sfNameList.add(hopSf);
                continue;
            }
            /*
             * We iterate thorough the list of service function types and for each one we try to get
             * get a suitable Service Function. WE need to perform lots of checking to make sure
             * we do not hit NULL Pointer exceptions
             */
            ServiceFunctionType serviceFunctionType;
            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(sfcServiceFunction.getType());
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList =
                        serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    SfName sfName = getServiceFunctionByType(serviceFunctionType);
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
