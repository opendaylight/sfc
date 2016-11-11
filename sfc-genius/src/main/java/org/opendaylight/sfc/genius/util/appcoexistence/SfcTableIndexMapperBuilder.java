/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.genius.util.appcoexistence;

/**
 * Builder class. Eases the creation of SFC table index mappers
 */
public class SfcTableIndexMapperBuilder {

    public static final short EXTERNAL_TABLE_NOT_SET = -1;
    private short externalTransportClassifierTable = EXTERNAL_TABLE_NOT_SET;
    private short externalTransportIngressTable = EXTERNAL_TABLE_NOT_SET;
    private short externalPathMapperTable = EXTERNAL_TABLE_NOT_SET;
    private short externalPathMapperAclTable = EXTERNAL_TABLE_NOT_SET;
    private short externalNextHopTable = EXTERNAL_TABLE_NOT_SET;
    private short externalTransportEgressTable = EXTERNAL_TABLE_NOT_SET;

    public SfcTableIndexMapperBuilder setClassifierTable(
            short externalTransportClassifierTable) {
        this.externalTransportClassifierTable = externalTransportClassifierTable;
        return this;
    }

    public SfcTableIndexMapperBuilder setTransportIngressTable(
            short externalTransportIngressTable) {
        this.externalTransportIngressTable = externalTransportIngressTable;
        return this;
    }

    public SfcTableIndexMapperBuilder setPathMapperTable(
            short externalPathMapperTable) {
        this.externalPathMapperTable = externalPathMapperTable;
        return this;
    }

    public SfcTableIndexMapperBuilder setPathMapperAclTable(
            short externalPathMapperAclTable) {
        this.externalPathMapperAclTable = externalPathMapperAclTable;
        return this;
    }

    public SfcTableIndexMapperBuilder setNextHopTable(
            short externalNextHopTable) {
        this.externalNextHopTable = externalNextHopTable;
        return this;
    }

    public SfcTableIndexMapperBuilder setTransportEgressTable(
            short externalTransportEgressTable) {
        this.externalTransportEgressTable = externalTransportEgressTable;
        return this;
    }

    public SfcTableIndexMapper build() {
        return new SfcTableIndexMapper(externalTransportClassifierTable, externalTransportIngressTable,
                externalPathMapperTable, externalPathMapperAclTable,
                externalNextHopTable, externalTransportEgressTable);
    }
}
