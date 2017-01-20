/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkOverutilized;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkUtilizationNormal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkDiscoveryListener implements FlowTopologyDiscoveryListener {
	private static final Logger LOG = LoggerFactory.getLogger(LinkDiscoveryListener.class);

	public LinkDiscoveryListener() {
		LOG.info("LinkDiscoveryListener initialized");
	}

    @Override
    public void onLinkDiscovered(final LinkDiscovered notification) {
    	// NOOP
        LOG.trace("LinkDiscoveryListener onLinkDiscovered");
    }

    @Override
    public void onLinkOverutilized(final LinkOverutilized notification) {
        // NOOP
        LOG.trace("LinkDiscoveryListener onLinkOverutilized");
    }

    @Override
    public void onLinkRemoved(final LinkRemoved notification) {
    	// NOOP
    	LOG.trace("LinkDiscoveryListener onLinkRemoved");
    }

    @Override
    public void onLinkUtilizationNormal(final LinkUtilizationNormal notification) {
        // NOOP
        LOG.trace("LinkDiscoveryListener onLinkUtilizationNormal");
    }

}
