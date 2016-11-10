/*
 * Copyright (c) 2016 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;


import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderScheduleTypeAPI;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This class listens to changes (addition, update, removal) in Service Function
 * Scheduler Types taking the appropriate actions.
 *
 * @author Ursicio Martin (ursicio.javier.martin@ericsson.com)
 *
 */
public class ServiceFunctionSchedulerTypeListener
        extends AbstractDataTreeChangeListener<ServiceFunctionSchedulerType> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionSchedulerTypeListener.class);

    private final DataBroker dataBroker;
    private ListenerRegistration<ServiceFunctionSchedulerTypeListener> listenerRegistration;

    public ServiceFunctionSchedulerTypeListener(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        LOG.debug("Initializing...");
        registerListeners();
    }

    private void registerListeners() {
        final DataTreeIdentifier<ServiceFunctionSchedulerType> treeId = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(ServiceFunctionSchedulerTypes.class).child(ServiceFunctionSchedulerType.class));
        listenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);
    }

    // SF Scheduler Type ADDITION
    @Override
    protected void add(ServiceFunctionSchedulerType serviceFunctionSchedulerType) {
        if (serviceFunctionSchedulerType != null) {
        LOG.debug("\n########## createdServiceFunctionSchedulerType {} {}",
                serviceFunctionSchedulerType.getType(), serviceFunctionSchedulerType.getName());
        if ( serviceFunctionSchedulerType.isEnabled() ) {
            ServiceFunctionSchedulerTypes serviceFunctionSchedulerTypes = SfcProviderScheduleTypeAPI
                    .readAllServiceFunctionScheduleTypes();
            if(serviceFunctionSchedulerTypes != null) {
                List<ServiceFunctionSchedulerType> sfScheduleTypeList =
                        serviceFunctionSchedulerTypes.getServiceFunctionSchedulerType();
                for (ServiceFunctionSchedulerType sfst : sfScheduleTypeList) {
                    if ( sfst.isEnabled() ) {
                        if (!sfst.getType().equals(serviceFunctionSchedulerType.getType())) {
                            ServiceFunctionSchedulerType sfstUpdate = new ServiceFunctionSchedulerTypeBuilder()
                                    .setName(sfst.getName())
                                    .setType(sfst.getType())
                                    .setEnabled(false).build();

                            SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(sfstUpdate);
                            break;
                        }
                    }
                }
            }
        }
        }
    }

    // SF Scheduler Type DELETION
    @Override
    protected void remove(ServiceFunctionSchedulerType serviceFunctionSchedulerType){
            if (serviceFunctionSchedulerType != null) {
            LOG.debug("\n########## deletedServiceFunctionSchedulerType {} {}",
                    serviceFunctionSchedulerType.getType(), serviceFunctionSchedulerType.getName());
        }
    }

    // SF Scheduler Type UPDATE
    @Override
    protected void update(ServiceFunctionSchedulerType originalServiceFunctionSchedulerType,
                          ServiceFunctionSchedulerType updatedServiceFunctionSchedulerType) {

        if (originalServiceFunctionSchedulerType.getType() != null && updatedServiceFunctionSchedulerType.getType() != null
                && originalServiceFunctionSchedulerType.getType().equals( updatedServiceFunctionSchedulerType.getType())) {
            LOG.debug("\n########## updatedServiceFunctionSchedulerType {} {}",
                    updatedServiceFunctionSchedulerType.getType(), updatedServiceFunctionSchedulerType.getName());

                if ( updatedServiceFunctionSchedulerType.isEnabled() ) {
                    ServiceFunctionSchedulerTypes serviceFunctionSchedulerTypes = SfcProviderScheduleTypeAPI
                            .readAllServiceFunctionScheduleTypes();
                    if(serviceFunctionSchedulerTypes != null) {
                        List<ServiceFunctionSchedulerType> sfScheduleTypeList =
                                serviceFunctionSchedulerTypes.getServiceFunctionSchedulerType();
                        for (ServiceFunctionSchedulerType sfst : sfScheduleTypeList) {
                            if ( sfst.isEnabled() ) {
                                if (!sfst.getType().equals(updatedServiceFunctionSchedulerType.getType())) {
                                    ServiceFunctionSchedulerType sfstUpdate = new ServiceFunctionSchedulerTypeBuilder()
                                            .setName(sfst.getName())
                                            .setType(sfst.getType())
                                            .setEnabled(false).build();

                                    SfcProviderScheduleTypeAPI.putServiceFunctionScheduleType(sfstUpdate);
                                    break;

                                }
                            }
                        }
                    }
                }
        }
    }


    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }
}
