/*
 * Copyright (c) 2017 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.openflow;

import org.opendaylight.sfc.genius.util.appcoexistence.SfcTableIndexMapper;

public class SfcOpenFlowConfig {
    // Used for app-coexistence
    private short tableBase;
    private short tableEgress;
    private final short maxTableOffset;
    private SfcTableIndexMapper tableIndexMapper;

    // TODO these definitions are duplicated
    private static final short TABLE_INDEX_TRANSPORT_INGRESS = 1;
    private static final short APP_COEXISTENCE_NOT_SET = -1;

    public SfcOpenFlowConfig(short maxTableOffset) {
        this.tableBase = 0;
        this.tableEgress = 0;
        this.maxTableOffset = maxTableOffset;
        this.tableIndexMapper = null;
    }

    public short getTableBase() {
        return tableBase;
    }

    // TODO need to synchronize these methods
    public void setTableBase(short tableBase) {
        this.tableBase = tableBase;
    }

    public short getTableEgress() {
        return tableEgress;
    }

    public void setTableEgress(short tableEgress) {
        this.tableEgress = tableEgress;
    }

    public short getMaxTableOffset() {
        return this.maxTableOffset;
    }

    public void setTableIndexMapper(SfcTableIndexMapper tableIndexMapper) {
        this.tableIndexMapper = tableIndexMapper;
    }

    public SfcTableIndexMapper getTableIndexMapper() {
        return this.tableIndexMapper;
    }

    /**
     * getTableId Having a TableBase allows us to "offset" the SFF tables by
     * this.tableBase tables. This is used for App Coexistence. When a
     * {@link SfcTableIndexMapper} has been provided, it is used (this is
     * another way of performing App coexistence)
     *
     * @param tableIndex
     *            - the table to offset
     * @return the resulting table id
     */
    protected short getTableId(short tableIndex) {

        // A transport processor can provide a table index mapper in order
        // to retrieve table positions
        if (tableIndexMapper != null && tableIndexMapper.getTableIndex(tableIndex).isPresent()) {
            return tableIndexMapper.getTableIndex(tableIndex).get();
        }

        if (getTableBase() > APP_COEXISTENCE_NOT_SET) {
            // App Coexistence
            if (tableIndex == TABLE_INDEX_TRANSPORT_INGRESS) {
                // With AppCoexistence the TransportIngress table is now table 0
                return 0;
            } else {
                // Need to subtract 2 to compensate for:
                // - TABLE_INDEX_CLASSIFIER=0 - which is not used for
                // AppCoexistence
                // - TABLE_INDEX_TRANSPORT_INGRESS=1 - which is table 0 for
                // AppCoexistence
                // Example: tableBase=20, TABLE_INDEX_PATH_MAPPER=2, should
                // return 20
                return (short) (getTableBase() + tableIndex - 2);
            }
        } else {
            return tableIndex;
        }
    }

}
