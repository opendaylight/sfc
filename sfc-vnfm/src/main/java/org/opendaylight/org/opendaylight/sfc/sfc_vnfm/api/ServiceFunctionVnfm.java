/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.org.opendaylight.sfc.sfc_vnfm.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;

/**
 * Interface for VNFM operations. Concrete implementations
 * such as a Tacker API will need to override these methods
 * <p>
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2015-10-18
 */
public interface ServiceFunctionVnfm {
    void createServiceFunction(String serviceFunctionName, Class<? extends ServiceFunctionTypeIdentity> serviceFunctionType);
    void deleteServiceFunction(String serviceFunctionName);
    void getServiceFunctionStats(String serviceFunctionName);
    void suspendServiceFunction(String serviceFunctionName);
}
