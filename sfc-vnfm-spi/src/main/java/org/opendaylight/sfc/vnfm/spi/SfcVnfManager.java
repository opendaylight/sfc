/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.vnfm.spi;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;

public interface SfcVnfManager {

    /**
     * This method creates service function based on service function type provided
     * @param sfType the type of service function
     * @return true if service function was created, false otherwise
     */
    boolean createSf(ServiceFunctionType sfType);

    /**
     * This method deletes service function from SfcVnfManager
     * @param sf the service function to delete from SfcVnfManager
     * @return true if service function was deleted, false otherwise
     */
    boolean deleteSf(ServiceFunction sf);

    /**
     * This method returns service statistics for provided service function
     * @param sf service function to provide service statistics for
     * @return service statistics for provided ServiceFunction
     */
    StatisticByTimestamp getSfStatistics(ServiceFunction sf);
}
