/*
 * Copyright (c) 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.openflow.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOfProviderUtilsTestMock extends SfcOfBaseProviderUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfProviderUtilsTestMock.class);
    private Map<SfName, ServiceFunction> serviceFunctions;
    private Map<String, ServiceFunctionGroup> serviceFunctionGroups;
    private Map<SffName, ServiceFunctionForwarder> serviceFunctionForwarders;
    private Map<SftTypeName, ServiceFunctionType> servceFunctionTypes;


    public SfcOfProviderUtilsTestMock() {
        LOG.info("SfcOfProviderUtilsTestMock constructor");
        serviceFunctions = new HashMap<>();
        serviceFunctionGroups = new HashMap<>();
        serviceFunctionForwarders = new HashMap<>();
        servceFunctionTypes = new HashMap<>();
    }

    public void addServiceFunction(SfName sfName, ServiceFunction sf) {
        serviceFunctions.put(sfName, sf);
    }

    public void addServiceFunctionType(SftTypeName sftType, ServiceFunctionType sfType) {
        servceFunctionTypes.put(sftType, sfType);
    }

    public void addServiceFunctionForwarder(SffName sffName, ServiceFunctionForwarder sff) {
        serviceFunctionForwarders.put(sffName, sff);
    }

    public void addServiceFunctionGroup(String sfgName, ServiceFunctionGroup sfg) {
        serviceFunctionGroups.put(sfgName, sfg);
    }

    // Only needed for multi-threading, empty for now
    @Override
    public void addRsp(long rspId) {
    }

    // Only needed for multi-threading, empty for now
    @Override
    public void removeRsp(long rspId) {
    }

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
    public ServiceFunctionType getServiceFunctionType(SfName sfName, long pathId) {
        ServiceFunction sf = serviceFunctions.get(sfName);
        return servceFunctionTypes.get(sf.getType());
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

    @Override
    public List<SffDataPlaneLocator> getSffNonSfDataPlaneLocators(ServiceFunctionForwarder sff) {
        List<SffDataPlaneLocator> nonSfDpls = new ArrayList<>();

        for (SffDataPlaneLocator sffDpl : sff.getSffDataPlaneLocator()) {
            boolean dplInSf = false;
            if (sff.getServiceFunctionDictionary() == null) {
                continue;
            }

            for (ServiceFunctionDictionary sffDict : sff.getServiceFunctionDictionary()) {
                if (sffDict.getSffSfDataPlaneLocator() == null
                        || sffDict.getSffSfDataPlaneLocator().getSffDplName() == null) {
                    continue;
                }
                if (sffDpl.getName().toString().equals(sffDict.getSffSfDataPlaneLocator().getSffDplName().toString())) {
                    dplInSf = true;
                    continue;
                }
            }

            if (!dplInSf) {
                LOG.debug("getNonSfDataPlaneLocators found NonSf DPL [{}] from SFF [{}]", sffDpl.getName().toString(),
                        sff.getName().toString());
                nonSfDpls.add(sffDpl);
            }
        }

        return nonSfDpls;
    }

    @Override
    public void setTableOffsets(SffName sffName, long tableBase) {
        // Empty for now
    }
}
