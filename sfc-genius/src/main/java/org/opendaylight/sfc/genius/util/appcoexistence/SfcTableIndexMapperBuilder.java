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

    public SfcTableIndexMapperBuilder setClassifierTable(short classifierTable) {
        this.externalTransportClassifierTable = classifierTable;
        return this;
    }

    public SfcTableIndexMapperBuilder setTransportIngressTable(short ingressTable) {
        this.externalTransportIngressTable = ingressTable;
        return this;
    }

    public SfcTableIndexMapperBuilder setPathMapperTable(short mapperTable) {
        this.externalPathMapperTable = mapperTable;
        return this;
    }

    public SfcTableIndexMapperBuilder setPathMapperAclTable(short mapperAclTable) {
        this.externalPathMapperAclTable = mapperAclTable;
        return this;
    }

    public SfcTableIndexMapperBuilder setNextHopTable(short nextHopTable) {
        this.externalNextHopTable = nextHopTable;
        return this;
    }

    public SfcTableIndexMapperBuilder setTransportEgressTable(short transportEgressTable) {
        this.externalTransportEgressTable = transportEgressTable;
        return this;
    }

    public SfcTableIndexMapper build() {
        return new SfcTableIndexMapper(externalTransportClassifierTable, externalTransportIngressTable,
                externalPathMapperTable, externalPathMapperAclTable,
                externalNextHopTable, externalTransportEgressTable);
    }
}
