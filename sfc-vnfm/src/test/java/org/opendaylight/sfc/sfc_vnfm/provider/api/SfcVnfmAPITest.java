/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_vnfm.provider.api;

import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;

public class SfcVnfmAPITest extends AbstractDataBrokerTest {

    private final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private static final String IP_MGMT_ADDRESS = "192.168.1.2";
    private static final int DP_PORT = 6633;
    private static final SfName SF_NAME = new SfName("dummySF");
    private static final SfName SF_STATE_NAME = new SfName("dummySFS");

    @Before
    public void before() {
        DataBroker dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
    }
}
