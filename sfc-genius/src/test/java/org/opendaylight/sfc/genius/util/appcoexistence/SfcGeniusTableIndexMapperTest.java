/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.genius.util.appcoexistence;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.NwConstants;

/**
 * This test checks the correct behavior of the table index mapper (a class
 * which perform translation between the SFC table indexes used internally by a
 * renderer and the table indexes assigned by Genius for those tables)
 *
 * @author Diego Granados
 */
public class SfcGeniusTableIndexMapperTest {

    private final short ORIGINAL_TRANSPORT_CLASSIFIER_TABLE_INDEX = 0;
    private final short ORIGINAL_TRANSPORT_INGRESS_TABLE_INDEX = 1;
    private final short ORIGINAL_PATH_MAPPER_TABLE_INDEX = 2;
    private final short ORIGINAL_PATH_MAPPER_ACL_TABLE_INDEX = 3;
    private final short ORIGINAL_NEXTHOP_TABLE_INDEX = 4;
    private final short ORIGINAL_TRANSPORT_EGRESS_TABLE_INDEX = 10;

    private final short UNMAPPED_INDEX = 13;

    @Test
    public void testMapping() {
        SfcTableIndexMapperBuilder builder = new SfcTableIndexMapperBuilder();
        builder.setClassifierTable(ORIGINAL_TRANSPORT_CLASSIFIER_TABLE_INDEX);
        builder.setTransportIngressTable(ORIGINAL_TRANSPORT_INGRESS_TABLE_INDEX);
        builder.setPathMapperTable(ORIGINAL_PATH_MAPPER_TABLE_INDEX);
        builder.setPathMapperAclTable(ORIGINAL_PATH_MAPPER_ACL_TABLE_INDEX);
        builder.setNextHopTable(ORIGINAL_NEXTHOP_TABLE_INDEX);
        builder.setTransportEgressTable(ORIGINAL_TRANSPORT_EGRESS_TABLE_INDEX);
        SfcTableIndexMapper tableIndexMapper = builder.build();

        Assert.assertEquals(
                tableIndexMapper
                        .getTableIndex(ORIGINAL_TRANSPORT_CLASSIFIER_TABLE_INDEX)
                        .get().shortValue(),
                NwConstants.SFC_TRANSPORT_CLASSIFIER_TABLE);
        Assert.assertEquals(
                tableIndexMapper
                        .getTableIndex(ORIGINAL_TRANSPORT_INGRESS_TABLE_INDEX)
                        .get().shortValue(),
                NwConstants.SFC_TRANSPORT_INGRESS_TABLE);
        Assert.assertEquals(
                tableIndexMapper.getTableIndex(ORIGINAL_PATH_MAPPER_TABLE_INDEX)
                        .get().shortValue(),
                NwConstants.SFC_TRANSPORT_PATH_MAPPER_TABLE);
        Assert.assertEquals(
                tableIndexMapper
                        .getTableIndex(ORIGINAL_PATH_MAPPER_ACL_TABLE_INDEX)
                        .get().shortValue(),
                NwConstants.SFC_TRANSPORT_PATH_MAPPER_ACL_TABLE);
        Assert.assertEquals(
                tableIndexMapper.getTableIndex(ORIGINAL_NEXTHOP_TABLE_INDEX)
                        .get().shortValue(),
                NwConstants.SFC_TRANSPORT_NEXT_HOP_TABLE);
        Assert.assertEquals(
                tableIndexMapper
                        .getTableIndex(ORIGINAL_TRANSPORT_EGRESS_TABLE_INDEX)
                        .get().shortValue(),
                NwConstants.SFC_TRANSPORT_EGRESS_TABLE);
        Assert.assertEquals(
                tableIndexMapper.getTableIndex(UNMAPPED_INDEX).isPresent(),
                false);
    }
}
