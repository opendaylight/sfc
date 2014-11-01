package org.opendaylight.ofsfc.provider.utils;

import java.math.BigInteger;

public class SfcOfL2Constants {
    public static final BigInteger METADATA_MASK_SFP_MATCH = new BigInteger("000000000000FFFF", 16);
    public static final BigInteger METADATA_BASE_MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);
    public static final BigInteger COOKIE_SFC_BASE = new BigInteger("1000000", 16);

    public static final short TABLE_INDEX_INGRESS_TRANSPORT_TABLE = 0;
    public static final short TABLE_INDEX_INGRESS = 1;
    public static final short TABLE_INDEX_CLASSIFICATION = 2;
    public static final short TABLE_INDEX_NEXT_HOP = 3;
    public static final short TABLE_INDEX_FIRST_HOP = 4;
    public static final short TABLE_INDEX_TRANSPORT_EGRESS = 10;

    public static final int FLOW_PRIORITY_CLASSIFICATION = 256;
    public static final int FLOW_PRIORITY_NEXT_HOP = 256;
    public static final int FLOW_PRIORITY_INGRESS = 256;
    public static final int FLOW_PRIORITY_DEFAULT_NEXT_HOP = 100;
    public static final int FLOW_PRIORITY_TRANSPORT = 100;

    public static final String FLOWID_PREFIX = "SFC";
    public static final String FLOWID_SEPARATOR = ".";
    public static final String FLOW_INGRESS = "INGRESS FLOW";
    public static final String FLOW_CLASSIFICATION = "ACL FLOW";
    public static final String FLOW_NEXTHOP = "NEXTHOP FLOW";
    public static final String FLOW_FIRST_HOP = "FIRST_HOP FLOW";
    public static final String FLOW_NEXHOP_DEFAULT = "DEFAULT FLOW";
    public static final String FLOW_TRANSPORT_INGRESS = "INGRESS TRANSPORT";
    public static final String FLOW_TRANSPORT_EGRESS = "EGRESS TRANSPORT";

}
