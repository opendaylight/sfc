/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.util.openflow;

public class OpenflowConstants {

    // identifies initialization flows installed in the SFFs - since they don't belong
    // to a particular NSP / RSP path ID
    public static final long SFC_FLOWS = 0xdeadbeef;
}
