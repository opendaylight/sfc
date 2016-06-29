/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements a random SF scheduling mode.
 * <p>
 *
 * @author Johnson Li (johnson.li@intel.com)
 * @version 0.1
 *          <p>
 * @since 2015-03-04
 */
public class SfcServiceFunctionRandomSchedulerAPI extends SfcServiceFunctionSchedulerAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionRandomSchedulerAPI.class);

    SfcServiceFunctionRandomSchedulerAPI() {
        super.setSfcServiceFunctionSchedulerType(
                org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Random.class);
    }

    // TODO See similar TODO in LoadBalancer about method name.
    private SfName getServiceFunctionByType(ServiceFunctionType serviceFunctionType) {
        List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        int maxTries = sftServiceFunctionNameList.size();
        Random rad = new Random();
        ServiceFunction serviceFunction = null;
        SfName serviceFunctionName = null;
        int start = rad.nextInt(sftServiceFunctionNameList.size());

        while (maxTries > 0) {
            serviceFunctionName = new SfName(sftServiceFunctionNameList.get(start).getName());
            serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionName);
            if (serviceFunction != null) {
                break;
            } else {
                LOG.debug("ServiceFunction {} doesn't exist", serviceFunctionName);
                maxTries--;
                serviceFunctionName = null;
                start = (start + 1) % sftServiceFunctionNameList.size();
            }
        }
        if (serviceFunctionName == null) {
            LOG.error("Could not find an existing ServiceFunction for {}", serviceFunctionType.getType());
        }
        return serviceFunctionName;
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
                    LOG.info("sfName {} for serviceFunctionType {}", sfName, serviceFunctionType.getType());
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
