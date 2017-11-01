/*
 * Copyright (c) 2017 Inocybe Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Rendered
 * Service Paths (RSPs) in the Config data store, taking the appropriate
 * actions. As of the Oxygen release, RSPs are written to the config data
 * store when an SFP is created in the config data store.
 */
@Singleton
public class RenderedServicePathListener extends AbstractSyncDataTreeChangeListener<RenderedServicePath> {

    private static final Logger LOG = LoggerFactory.getLogger(RenderedServicePathListener.class);

    @Inject
    public RenderedServicePathListener(final DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(RenderedServicePaths.class).child(RenderedServicePath.class));
    }

    @Override
    public void add(@Nonnull RenderedServicePath renderedServicePath) {

        // As of the Oxygen release, the RSP creation workflow is as follows:
        // - An SFP creation in the Config Data Store triggers an RSP and
        //   optionally the symmetric RSP creation in the Config Data Store.
        // - This listener listens for RSPs created in the Config Data Store
        //   and will write the RSP in the Operational Data Store

        LOG.info("add: RSP: {}", renderedServicePath.getName().getValue());

        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI
                .readServiceFunctionPath(renderedServicePath.getParentServiceFunctionPath());

        if (serviceFunctionPath == null) {
            LOG.error("Service Function Path does not exist, cant create Rendered Service Path {}",
                    renderedServicePath.getName().getValue());
            return;
        }

        RenderedServicePath newRenderedServicePath = SfcProviderRenderedPathAPI
                .createRenderedServicePathAndState(serviceFunctionPath, renderedServicePath);
        if (newRenderedServicePath == null) {
            LOG.error("Failed to create Rendered Service Path {}", renderedServicePath.getName().getValue());
        }

        // As mentioned above, no need to do anything here for Symmetric
        // RSPs, since that will be taken care of when an SFP is created.
    }

    @Override
    public void remove(@Nonnull RenderedServicePath renderedServicePath) {
        LOG.info("remove: Deleting RSP: {}", renderedServicePath.getName().getValue());

        boolean ret = true;
        RspName rspName = renderedServicePath.getName();
        RspName revRspName = SfcProviderRenderedPathAPI.getReversedRspName(rspName);
        if (revRspName != null) {
            // The RSP has a symmetric ("Reverse") Path
            ret = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(revRspName);
            if (ret) {
                ret = SfcProviderServiceFunctionAPI.deleteRspFromServiceFunctionState(revRspName);
            }
            if (ret) {
                ret = SfcProviderRenderedPathAPI.deleteRenderedServicePath(revRspName);
            }

            if (!ret) {
                LOG.error("Failed to delete Symmetric RSP [{}] for RSP [{}]",
                        revRspName.getValue(), rspName.getValue());
            }
        }

        if (ret) {
            ret = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspName);
        }
        if (ret) {
            ret = SfcProviderServiceFunctionAPI.deleteRspFromServiceFunctionState(rspName);
        }
        if (ret) {
            ret = SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName);
        }

        if (!ret) {
            LOG.error("Failed to delete RSP [{}]", rspName.getValue());
        }
    }

    @Override
    public void update(@Nonnull RenderedServicePath originalRenderedServicePath,
                       @Nonnull RenderedServicePath updatedRenderedServicePath) {
        LOG.info("Update: RSP {}", updatedRenderedServicePath.getName().getValue());

        // Only support updating the SymmetricPathId
        if (updatedRenderedServicePath.getSymmetricPathId() == null) {
            LOG.info("Updating the RSP in config is not supported: {}",
                    updatedRenderedServicePath.getName().getValue());
            return;
        }

        if (originalRenderedServicePath.getSymmetricPathId() == null
            || !originalRenderedServicePath.getSymmetricPathId().equals(
                    updatedRenderedServicePath.getSymmetricPathId())) {
            RenderedServicePath operRenderedServicePath =
                    SfcProviderRenderedPathAPI.readRenderedServicePath(updatedRenderedServicePath.getName());
            // TODO what if the operational RSP hasnt been created??
            if (operRenderedServicePath != null) {
                LOG.info("Updated the symmetricPathId for RSP: {}", updatedRenderedServicePath.getName().getValue());
                // Update the SymmetricPathId on the operational RSP
                SfcProviderRenderedPathAPI.setSymmetricPathId(updatedRenderedServicePath,
                        updatedRenderedServicePath.getSymmetricPathId(), LogicalDatastoreType.OPERATIONAL);
            }
        }
    }
}
