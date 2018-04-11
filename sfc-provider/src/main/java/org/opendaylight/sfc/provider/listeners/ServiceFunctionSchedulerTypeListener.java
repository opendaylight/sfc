/*
 * Copyright (c) 2016, 2017 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcProviderScheduleTypeAPI;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service Function
 * Scheduler Types taking the appropriate actions.
 *
 * @author Ursicio Martin (ursicio.javier.martin@ericsson.com)
 */
@Singleton
public class ServiceFunctionSchedulerTypeListener extends
        AbstractSyncDataTreeChangeListener<ServiceFunctionSchedulerType> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionSchedulerTypeListener.class);

    @Inject
    public ServiceFunctionSchedulerTypeListener(DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ServiceFunctionSchedulerTypes.class).child(ServiceFunctionSchedulerType.class));
    }

    @Override
    public void add(@Nonnull ServiceFunctionSchedulerType serviceFunctionSchedulerType) {
        LOG.debug("Adding Service Function Scheduler Type {} {}", serviceFunctionSchedulerType.getType(),
                  serviceFunctionSchedulerType.getName());
        if (serviceFunctionSchedulerType.isEnabled()) {
            ServiceFunctionSchedulerTypes serviceFunctionSchedulerTypes = SfcProviderScheduleTypeAPI
                    .readAllServiceFunctionScheduleTypes();
            if (serviceFunctionSchedulerTypes != null) {
                List<ServiceFunctionSchedulerType> sfScheduleTypeList = serviceFunctionSchedulerTypes
                        .getServiceFunctionSchedulerType();
                for (ServiceFunctionSchedulerType sfst : sfScheduleTypeList) {
                    if (sfst.isEnabled() && !sfst.getType().equals(serviceFunctionSchedulerType.getType())) {
                        ServiceFunctionSchedulerType sfstUpdate = new ServiceFunctionSchedulerTypeBuilder()
                                .setName(sfst.getName()).setType(sfst.getType()).setEnabled(false).build();

                        SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(sfstUpdate);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void remove(@Nonnull ServiceFunctionSchedulerType serviceFunctionSchedulerType) {
        LOG.debug("Deleting Service FunctionScheduler Type {} {}", serviceFunctionSchedulerType.getType(),
                  serviceFunctionSchedulerType.getName());
    }

    // SF Scheduler Type UPDATE
    @Override
    public void update(@Nonnull ServiceFunctionSchedulerType originalServiceFunctionSchedulerType,
                       @Nonnull ServiceFunctionSchedulerType updatedServiceFunctionSchedulerType) {
        if (originalServiceFunctionSchedulerType.getType() != null
                && updatedServiceFunctionSchedulerType.getType() != null && originalServiceFunctionSchedulerType
                .getType().equals(updatedServiceFunctionSchedulerType.getType())) {
            LOG.debug("Updating ServiceFunction Scheduler Type {} {}", updatedServiceFunctionSchedulerType.getType(),
                      updatedServiceFunctionSchedulerType.getName());

            if (updatedServiceFunctionSchedulerType.isEnabled()) {
                ServiceFunctionSchedulerTypes serviceFunctionSchedulerTypes = SfcProviderScheduleTypeAPI
                        .readAllServiceFunctionScheduleTypes();
                if (serviceFunctionSchedulerTypes != null) {
                    List<ServiceFunctionSchedulerType> sfScheduleTypeList = serviceFunctionSchedulerTypes
                            .getServiceFunctionSchedulerType();
                    for (ServiceFunctionSchedulerType sfst : sfScheduleTypeList) {
                        if (sfst.isEnabled() && !sfst.getType().equals(updatedServiceFunctionSchedulerType.getType())) {
                            ServiceFunctionSchedulerType sfstUpdate = new ServiceFunctionSchedulerTypeBuilder()
                                    .setName(sfst.getName()).setType(sfst.getType()).setEnabled(false).build();

                            SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(sfstUpdate);
                            break;
                        }
                    }
                }
            }
        }
    }
}
