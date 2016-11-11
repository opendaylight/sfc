/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.genius.util.appcoexistence;

import java.util.HashMap;
import java.util.Optional;

import org.opendaylight.genius.mdsalutil.NwConstants;

/**
 * This class performs translation between a set of external SFC table indexes
 * and the table indexes which are reserved by Genius for those tables. For
 * doing so, the client module (sfc renderers using Genius) first builds the
 * SfcTableIndexMapper (using the companion {@link SfcTableIndexMapperBuilder}
 * class) by providing the table indexes it uses for each SFC table (this
 * mechanism exists in order not to have a circular dependency between
 * sfc-genius and the sfc renderer). Later, at flow writing time, the flow
 * programmer will use this class in order to retrieve the correct table indexes
 * for each SFC table
 *
 * In short, this class allows to perform Genius-based application coexistence
 * at table level, while keeping the previous application coexistence when
 * genius is not used
 *
 * @author Diego Granados
 *
 */
public class SfcTableIndexMapper {

    private HashMap<Short, Short> mappingTable = new HashMap();

    protected SfcTableIndexMapper(short externalClassifierTable, short externalTransportIngressTable,
            short externalPathMapperTable, short externalPathMapperAclTable,
            short externalNextHopTable, short externalTransportEgressTable) {

        addMapping(externalClassifierTable, NwConstants.SFC_TRANSPORT_CLASSIFIER_TABLE);
        addMapping(externalTransportIngressTable, NwConstants.SFC_TRANSPORT_INGRESS_TABLE);
        addMapping(externalPathMapperTable, NwConstants.SFC_TRANSPORT_PATH_MAPPER_TABLE);
        addMapping(externalPathMapperAclTable, NwConstants.SFC_TRANSPORT_PATH_MAPPER_ACL_TABLE);
        addMapping(externalNextHopTable, NwConstants.SFC_TRANSPORT_NEXT_HOP_TABLE);
        addMapping(externalTransportEgressTable, NwConstants.SFC_TRANSPORT_EGRESS_TABLE);
    }

    private void addMapping(short externalTableIndex,
            short geniusEquivalentTableIndex) {
        if (externalTableIndex != SfcTableIndexMapperBuilder.EXTERNAL_TABLE_NOT_SET) {
            mappingTable.put(externalTableIndex, geniusEquivalentTableIndex);
        }
    }

    /**
     * External - Genius SFC table index translation
     *
     * @param externalSfcTableIndex
     *            the table index used by the external component
     * @return the table index reserved by Genius for that table (or an empty
     *         optional when there is no mappinf for that index)
     */
    public Optional<Short> getTableIndex(short externalSfcTableIndex) {
        return Optional.ofNullable(mappingTable.get(externalSfcTableIndex));
    }
}
