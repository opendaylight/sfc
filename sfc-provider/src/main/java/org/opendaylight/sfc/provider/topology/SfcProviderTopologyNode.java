/*
 * Copyright (c) 2014, 2015 Intel Corporation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.topology;

/**
 * This class represents a node/vertex in topology graph
 * that composes of SFs (Service Functions) and SFFs (Service Function Forwarders)
 * , a node may be SF or SFF, please refer to class SfcProviderGraph for
 * topology graph
 *
 * @author Shuqiang Zhao &lt;shuqiangx.zhao@intel.com&gt;
 * @author Yi Yang &lt;yi.y.yang@intel.com&gt;
 */
public class SfcProviderTopologyNode implements Comparable<SfcProviderTopologyNode> {
    private String name;
    private int load;
    private SfcProviderTopologyNode parent;
    private int color;
    private int dist;

    public SfcProviderTopologyNode(String name)
    {
        this.name = name;
        this.load = 0;
        this.parent = null;
        this.color = 0;
        this.dist = 0;
    }

    public String getName() {
        return this.name;
    }

    public SfcProviderTopologyNode getParent() {
        return this.parent;
    }

    public void setParent(SfcProviderTopologyNode parent) {
        this.parent = parent;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getDist() {
        return this.dist;
    }

    public void setDist(int dist) {
        this.dist = dist;
    }

    public int compareTo(SfcProviderTopologyNode node) {
        return this.name.compareTo(node.getName());
    }

    public boolean equals(java.lang.Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof SfcProviderTopologyNode)) {
            return false;
        }

        return this.name == ((SfcProviderTopologyNode)obj).getName();
    }

    public int hashCode() {
        if (this.name == null) {
            return 0;
        }

        return this.name.hashCode();
    }
}
