/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.util.openflow;

public final class OpenflowConstants {

    // identifies initialization flows installed in the SFFs - since they don't belong
    // to a particular NSP / RSP path ID
    public static final long SFC_FLOWS = 0xdeadbeef;

    // Constant for marking next protocol=NSH in GPE
    public static final short TUN_GPE_NP_NSH = 0x4;

    // Ethernet NextProtocol/EtherType for NSH
    public static final long ETHERTYPE_NSH = 0x894F;

    public static final short NSH_NP_ETH = 0x3;

    private OpenflowConstants() {
    }
}
