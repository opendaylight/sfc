/*
 * Copyright (c) 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.utils;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfcOfProviderUtilsTestMock extends SfcOfBaseProviderUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfProviderUtilsTestMock.class);
    private Map<SfName, ServiceFunction> serviceFunctions;
    private Map<String, ServiceFunctionGroup> serviceFunctionGroups;
    private Map<SffName, ServiceFunctionForwarder> serviceFunctionForwarders;

    public SfcOfProviderUtilsTestMock() {
        LOG.info("SfcOfProviderUtilsTestMock constructor");
        serviceFunctions = new HashMap<SfName, ServiceFunction>();
        serviceFunctionGroups = new HashMap<String, ServiceFunctionGroup>();
        serviceFunctionForwarders = new HashMap<SffName, ServiceFunctionForwarder>();
    }

    public void addServiceFunction(SfName sfName, ServiceFunction sf) {
        serviceFunctions.put(sfName, sf);
    }

    public void addServiceFunctionForwarder(SffName sffName, ServiceFunctionForwarder sff) {
        serviceFunctionForwarders.put(sffName, sff);
    }

    public void addServiceFunctionGroup(String sfgName, ServiceFunctionGroup sfg) {
        serviceFunctionGroups.put(sfgName, sfg);
    }

    // Only needed for multi-threading, empty for now
    @Override
    public void addRsp(long rspId) {}

    // Only needed for multi-threading, empty for now
    @Override
    public void removeRsp(long rspId) {}

    public void resetCache() {
        LOG.info("SfcOfProviderUtilsTestMock resetCache");
        serviceFunctions.clear();
        serviceFunctionGroups.clear();
        serviceFunctionForwarders.clear();
    }

    @Override
    public ServiceFunction getServiceFunction(SfName sfName, long pathId) {
        return serviceFunctions.get(sfName);
    }

    @Override
    public ServiceFunctionForwarder getServiceFunctionForwarder(SffName sffName, long pathId) {
        return serviceFunctionForwarders.get(sffName);
    }

    @Override
    public ServiceFunctionGroup getServiceFunctionGroup(String sfgName, long pathId) {
        return serviceFunctionGroups.get(sfgName);
    }

    @Override
    public Long getPortNumberFromName(String bridgeName, String portName, long rspId) {
        return new Long(0);
    }

}
