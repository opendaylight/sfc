/**
 * Copyright (c) 2017 Inocybe Technologies Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.listeners;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.statistics.SfcStatisticsManagerInterface;
import org.opendaylight.yang.gen.v1.urn.inocybe.params.xml.ns.yang.sfc.statistics.configuration.rev171130.SfcStatisticsConfiguration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcStatisticsConfigurationListener extends AbstractSyncDataTreeChangeListener<SfcStatisticsConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcStatisticsConfigurationListener.class);
    private final SfcStatisticsManagerInterface statsManager;

    public SfcStatisticsConfigurationListener(final DataBroker dataBroker,
                                              SfcStatisticsManagerInterface sfcStatisticsManager) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(SfcStatisticsConfiguration.class));

        this.statsManager = sfcStatisticsManager;
    }

    @Override
    public void add(@Nonnull SfcStatisticsConfiguration newDataObject) {
        LOG.info("SfcStatisticsConfigurationListener::add updating StatisticsManager");
        this.statsManager.updateSfcStatisticsConfiguration();
    }

    @Override
    public void remove(@Nonnull SfcStatisticsConfiguration removedDataObject) {
        // Nothing to do here
        LOG.info("SfcStatisticsConfigurationListener::delete nothing to be done");
    }

    @Override
    public void update(@Nonnull SfcStatisticsConfiguration originalDataObject,
                       @Nonnull SfcStatisticsConfiguration updatedDataObject) {
        LOG.info("SfcStatisticsConfigurationListener::update updating StatisticsManager");
        this.statsManager.updateSfcStatisticsConfiguration();
    }
}
