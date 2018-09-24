/*
 * Copyright (c) 2016, 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.sfc.provider.validators.util.SfcDatastoreCache;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service
 * Functions taking the appropriate actions.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
@Singleton
public class ServiceFunctionListener extends AbstractSyncDataTreeChangeListener<ServiceFunction> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionListener.class);

    @Inject
    public ServiceFunctionListener(DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ServiceFunctions.class).child(ServiceFunction.class));
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<ServiceFunction> instanceIdentifier,
                    @Nonnull ServiceFunction serviceFunction) {
        LOG.debug("add: storing name [{}] type [{}]", serviceFunction.getName().getValue(),
                  serviceFunction.getType().getValue());
        SfcDatastoreCache.getSfToSfTypeCache().put(serviceFunction.getName(), serviceFunction.getType().getValue());
        if (!SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(serviceFunction)) {
            LOG.error("add:Failed to create Service Function: {}", serviceFunction.getName());
        }
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<ServiceFunction> instanceIdentifier,
                       @Nonnull ServiceFunction serviceFunction) {
        LOG.debug("remove: Deleting Service Function: {}", serviceFunction.getName());

        // delete cache
        SfcDatastoreCache.getSfToSfTypeCache().invalidate(serviceFunction.getName());

        deleteSfSfps(serviceFunction);
        if (!SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(serviceFunction)) {
            LOG.error("remove: Failed to delete Service Function: {}", serviceFunction.getName());
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<ServiceFunction> instanceIdentifier,
                       @Nonnull ServiceFunction originalServiceFunction,
                       @Nonnull ServiceFunction updatedServiceFunction) {
        LOG.debug("update:Updating Service Function: {}", originalServiceFunction.getName());

        if (!compareSfs(originalServiceFunction, updatedServiceFunction)) {
            // We only update SF type entry if type has changed
            if (!updatedServiceFunction.getType().equals(originalServiceFunction.getType())) {
                // We remove the original SF from SF type list
                SfcDatastoreCache.getSfToSfTypeCache().invalidate(originalServiceFunction.getName());
                SfcProviderServiceTypeAPI.deleteServiceFunctionTypeEntry(originalServiceFunction);
                SfcDatastoreCache.getSfToSfTypeCache()
                        .put(updatedServiceFunction.getName(), updatedServiceFunction.getType().getValue());
                // We create a independent entry
                SfcProviderServiceTypeAPI.createServiceFunctionTypeEntry(updatedServiceFunction);
            }

            deleteSfSfps(originalServiceFunction);
        }
    }

    private boolean compareSfs(@Nonnull ServiceFunction originalServiceFunction,
                               @Nonnull ServiceFunction serviceFunction) {
        // Compare SFF IP Mgmt Addresses
        if (serviceFunction.getIpMgmtAddress() != null && originalServiceFunction.getIpMgmtAddress() != null) {
            if (!serviceFunction.getIpMgmtAddress().toString()
                    .equals(originalServiceFunction.getIpMgmtAddress().toString())) {
                LOG.info("compareSFs: IP mgmt addresses changed orig [{}] new [{}]",
                         originalServiceFunction.getIpMgmtAddress().toString(),
                         serviceFunction.getIpMgmtAddress().toString());
                return false;
            }
        } else if (originalServiceFunction.getIpMgmtAddress() != null && serviceFunction.getIpMgmtAddress() == null) {
            LOG.info("compareSFFs: the IP mgmt address has been removed");
            return false;
        }

        // Compare SF Types
        if (!serviceFunction.getType().getValue().equals(originalServiceFunction.getType().getValue())) {
            LOG.info("compareSFs: SF type changed orig [{}] new [{}]", originalServiceFunction.getType().getValue(),
                     serviceFunction.getType().getValue());
            return false;
        }

        // Compare SF DPLs
        List<SfDataPlaneLocator> origSfDplList = originalServiceFunction.getSfDataPlaneLocator();
        List<SfDataPlaneLocator> sfDplList = serviceFunction.getSfDataPlaneLocator();

        if (origSfDplList.size() > sfDplList.size()) {
            LOG.info("compareSfDpls An SF DPL has been removed");
            // TODO should we check if the removed SF DPL is used??
            return false;
        }

        Collection<SfDataPlaneLocator> differentSfDpls = new ArrayList<>(sfDplList);
        // This will remove everything in common, thus leaving only the
        // different values
        differentSfDpls.removeAll(origSfDplList);

        // If the different SfDpl entries are all contained in the sfDplList,
        // then this was a simple case of adding a new SfDpl entry, else one
        // of the entries was modified, and the RSPs should be deleted
        if (!sfDplList.containsAll(differentSfDpls)) {
            LOG.info("compareSfDpls An SF DPL has been modified");
            return false;
        }

        return true;
    }

    /**
     * Removes all the SFPs in which the Service Function is referenced.
     * Removing the SFP will in-turn remove the RSP from config and oper
     */
    private void deleteSfSfps(ServiceFunction serviceFunction) {
        /*
         * Before removing RSPs used by this Service Function, we need to remove
         * all references in the SFF/SF operational trees
         */
        SfName sfName = serviceFunction.getName();
        List<SfServicePath> sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(sfName);
        if (sfServicePathList != null && !sfServicePathList.isEmpty()) {
            if (!SfcProviderServiceFunctionAPI.deleteServiceFunctionState(sfName)) {
                LOG.error("{}: Failed to delete SF {} operational state", Thread.currentThread().getStackTrace()[1],
                          sfName);
            }
            for (SfServicePath sfServicePath : sfServicePathList) {
                LOG.info("Deleting SFP [{}] on SF [{}]", sfServicePath.getName().getValue(), sfName);
                SfcProviderServicePathAPI.deleteServiceFunctionPath(sfServicePath.getName());
            }
        }

        /*
         * We do not update the SFF dictionary. Since the user configured it in
         * the first place, (s)he is also responsible for updating it.
         */
    }
}
