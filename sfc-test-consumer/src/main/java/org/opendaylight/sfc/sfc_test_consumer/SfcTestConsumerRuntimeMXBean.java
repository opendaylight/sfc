/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sfc_test_consumer;

public interface SfcTestConsumerRuntimeMXBean {
    Boolean testAReadSf();

    Boolean testBReadSfc();

    Boolean testAPutSf();

    Boolean testBDeleteSfc();

    Boolean testBPutSfs();

    Boolean testADeleteSf();

    Boolean testCPutData();

    Boolean testBPutSfc();
}
