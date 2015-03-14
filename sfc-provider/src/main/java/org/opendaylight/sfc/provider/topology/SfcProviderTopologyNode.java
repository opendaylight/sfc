/*
* Copyright (c) 2014 Intel Corporation. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.sfc.provider.topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcProviderTopologyNode
{
    public String name;
    public String load;
    public SfcProviderTopologyNode parent;
    public int color;
    public int dist;

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderGraph.class);

    public void SfcProviderTopologyNode()
    {
        this.name = "";
        this.load = "";
        this.parent = null;
        this.color = 0;
        this.dist = 0;
    }

    public void show()
    {
        LOG.info("SfcProviderTopologyNode : {}", this.name);
    }
}
