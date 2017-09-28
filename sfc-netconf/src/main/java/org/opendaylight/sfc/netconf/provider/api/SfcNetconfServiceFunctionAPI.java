/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.netconf.provider.api;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1Builder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMonBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFMonitoringInfoOutput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to map Netconf to SFC Service Function.
 *
 * @author Yi Yang (yi.y.yang@intel.com)
 * @version 0.1
 * @see org.opendaylight.sfc.netconf.provider.api.SfcNetconfServiceFunctionAPI
 * @since 2015-09-01
 */
public class SfcNetconfServiceFunctionAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfServiceFunctionAPI.class);
    private final SfcProviderSfDescriptionMonitorAPI getSfDescMon;

    public SfcNetconfServiceFunctionAPI(SfcProviderSfDescriptionMonitorAPI getSfDescMon) {
        this.getSfDescMon = getSfDescMon;
    }

    /**
     * Returns an Service Function object which can be stored in DataStore.
     *
     * @param nodeName
     *            Service Function name
     * @param ipAddress
     *            Service Function data plane IP
     * @param portNumber
     *            Service Function data plane port
     * @param type
     *            Service Function type
     * @return ServiceFunction Object
     */
    public static ServiceFunction buildServiceFunctionFromNetconf(SfName nodeName, IpAddress ipAddress,
            PortNumber portNumber, SftTypeName type) {
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress).setPort(portNumber);
        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        SfDataPlaneLocatorName sfDplName = new SfDataPlaneLocatorName(ipAddress.getIpv4Address().getValue());
        locatorBuilder.setName(sfDplName).setLocatorType(ipBuilder.build()).setTransport(VxlanGpe.class);
        SfDataPlaneLocator sfDataPlaneLocator = locatorBuilder.build();
        List<SfDataPlaneLocator> dataPlaneLocatorList = new ArrayList<>();
        dataPlaneLocatorList.add(sfDataPlaneLocator);

        ServiceFunctionKey key = new ServiceFunctionKey(nodeName);
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(nodeName).setKey(key).setType(type).setIpMgmtAddress(ipAddress)
                .setSfDataPlaneLocator(dataPlaneLocatorList);
        return serviceFunctionBuilder.build();
    }

    /**
     * Get the service function description information from netconf mountpoint.
     *
     * @param sfName
     *            Service Function name
     * @return DescriptionInfo SF description information
     */
    public DescriptionInfo getServiceFunctionDescription(String sfName) {
        /* Service Function name is netconf mount point */
        String mountpoint = sfName;

        // Get sf description information from netconf
        GetSFDescriptionOutput sfDescInfoOutput = getSfDescMon.getSFDescriptionInfoFromNetconf(mountpoint);
        if (sfDescInfoOutput == null) {
            LOG.warn("getSFDescriptionInfoFromNetconf returns null at mount point {}", mountpoint);
            return null;
        }

        DescriptionInfo descInfo = new DescriptionInfoBuilder(sfDescInfoOutput.getDescriptionInfo()).build();
        LOG.info("DescriptionInfo of SF {}: type: {}, data-plane-ip: {}, data-plane-port: {}", mountpoint,
                descInfo.getType(), descInfo.getDataPlaneIp().getIpv4Address().getValue(),
                descInfo.getDataPlanePort().getValue());
        return descInfo;
    }

    /**
     * put the service function description information into the OPERATIONAL
     * datastore.
     *
     * @param descInfo
     *            Service Function description information
     * @param sfName
     *            Service Function name
     * @return true if descInfo was successfully put, false otherwise
     */
    public boolean putServiceFunctionDescription(DescriptionInfo descInfo, SfName sfName) {
        boolean ret = false;
        printTraceStart(LOG);

        SfcSfDescMonBuilder sfDescMonBuilder = new SfcSfDescMonBuilder().setDescriptionInfo(descInfo);

        // get ServiceFunctionState
        ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier
                .builder(ServiceFunctionsState.class).child(ServiceFunctionState.class, serviceFunctionStateKey)
                .build();

        ServiceFunctionState dataSfcStateObject = SfcDataStoreAPI.readTransactionAPI(sfStateIID,
                LogicalDatastoreType.OPERATIONAL);

        // build the service function capability and utilization
        if (dataSfcStateObject != null) {
            ServiceFunctionState1 sf1Temp = dataSfcStateObject.getAugmentation(ServiceFunctionState1.class);
            if (sf1Temp != null) {
                SfcSfDescMon sfDescMonTemp = sf1Temp.getSfcSfDescMon();
                sfDescMonBuilder.setMonitoringInfo(sfDescMonTemp.getMonitoringInfo());
            }
        }

        SfcSfDescMon sfDescMon = sfDescMonBuilder.build();

        ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
        ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                .setKey(serviceFunctionStateKey).addAugmentation(ServiceFunctionState1.class, sfState1).build();

        if (dataSfcStateObject != null) {
            ret = SfcProviderServiceFunctionAPI.mergeServiceFunctionState(serviceFunctionState);
        } else {
            ret = SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
        }

        printTraceStop(LOG);
        return ret;
    }

    /**
     * Get the service function monitor information from netconf mountpoint.
     *
     * @param sfName
     *            Service Function name
     * @return MonitoringInfo Service Function monitor information
     */
    public MonitoringInfo getServiceFunctionMonitor(String sfName) {
        /* Service Function name is netconf mount point */
        String mountpoint = sfName;

        // get sf monitor data from netconf
        GetSFMonitoringInfoOutput sfMonInfoMap = getSfDescMon.getSFMonitorInfoFromNetconf(mountpoint);
        if (sfMonInfoMap == null) {
            LOG.warn("getSFMonitorInfoFromNetconf returns null at mount point {}", mountpoint);
            return null;
        }
        MonitoringInfo monInfo = new MonitoringInfoBuilder(sfMonInfoMap.getMonitoringInfo()).build();
        LOG.info("MonitoringInfo of SF {}: CPU utilization: {}, Memory utilization: {}", mountpoint,
                monInfo.getResourceUtilization().getCPUUtilization(),
                monInfo.getResourceUtilization().getMemoryUtilization());
        return monInfo;
    }

    /**
     * Put the service function monitor information into the OPERATIONAL
     * datastore.
     *
     * @param monInfo
     *            Service Function monitoring information
     * @param sfName
     *            Service Function name
     * @return true if monInfo was successfully put, fasle otherwise
     */
    public boolean putServiceFunctionMonitor(MonitoringInfo monInfo, SfName sfName) {
        boolean ret = false;
        printTraceStart(LOG);

        SfcSfDescMonBuilder sfDescMonBuilder = new SfcSfDescMonBuilder().setMonitoringInfo(monInfo);

        // get ServiceFunctionState
        ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier
                .builder(ServiceFunctionsState.class).child(ServiceFunctionState.class, serviceFunctionStateKey)
                .build();

        ServiceFunctionState dataSfcStateObject = SfcDataStoreAPI.readTransactionAPI(sfStateIID,
                LogicalDatastoreType.OPERATIONAL);

        // build the service function capability and utilization
        if (dataSfcStateObject != null) {
            ServiceFunctionState1 sf1Temp = dataSfcStateObject.getAugmentation(ServiceFunctionState1.class);
            if (sf1Temp != null) {
                SfcSfDescMon sfDescMonTemp = sf1Temp.getSfcSfDescMon();
                sfDescMonBuilder.setDescriptionInfo(sfDescMonTemp.getDescriptionInfo());
            }
        }

        SfcSfDescMon sfDescMon = sfDescMonBuilder.build();

        ServiceFunctionState1 sfState1 = new ServiceFunctionState1Builder().setSfcSfDescMon(sfDescMon).build();
        ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                .setKey(serviceFunctionStateKey).addAugmentation(ServiceFunctionState1.class, sfState1).build();

        if (dataSfcStateObject != null) {
            ret = SfcProviderServiceFunctionAPI.mergeServiceFunctionState(serviceFunctionState);
        } else {
            ret = SfcProviderServiceFunctionAPI.putServiceFunctionState(serviceFunctionState);
        }

        printTraceStop(LOG);
        return ret;
    }
}
