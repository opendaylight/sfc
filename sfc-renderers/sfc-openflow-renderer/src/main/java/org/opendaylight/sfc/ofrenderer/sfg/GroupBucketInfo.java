/*
 * Copyright (c) 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.sfg;

public class GroupBucketInfo {

    private String sfMac;
    private String sfIp;
    private String outPort;
    private int index;

    public String getSfMac() {
        return sfMac;
    }

    public void setSfMac(String sfMac) {
        this.sfMac = sfMac;
    }

    public String getSfIp() {
        return sfIp;
    }

    public void setSfIp(String sfIp) {
        this.sfIp = sfIp;
    }

    public String getOutPort() {
        return outPort;
    }

    public void setOutPort(String outPort) {
        this.outPort = outPort;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "GroupBucketInfo [sfMac=" + sfMac + ", sfIp=" + sfIp + ", outPort=" + outPort + ", index=" + index + "]";
    }
}
