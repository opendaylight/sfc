/*
* Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcNetconfDataProvider;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFDescriptionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFMonitoringInfoOutput;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFMonitoringInfoOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.ServiceFunctionDescriptionMonitorReportService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.description.output.DescriptionInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.description.output.DescriptionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.monitoring.info.output.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.monitoring.info.output.MonitoringInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.Capabilities;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.SFPortsBandwidthUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.SFPortsBandwidthUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.PortsBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.PortsBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilizationKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Future;
import com.google.common.base.Preconditions;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SfcProviderSfDescriptionMonitorAPI{
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfDescriptionMonitorAPI.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static ConsumerContext sessionData;

    public SfcProviderSfDescriptionMonitorAPI() {
            setSession();
    }

    protected void setSession()  {
        printTraceStart(LOG);
        try {
            if(odlSfc.getBroker() != null) {
                if(sessionData==null) {
                    sessionData = odlSfc.getBroker().registerConsumer(SfcNetconfDataProvider.GetNetconfDataProvider());
                    Preconditions.checkState(sessionData != null,"SfcNetconfDataProvider register is not available.");
                }
            }
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
    }

    public GetSFDescriptionOutput getSFDescriptionInfoFromNetconf(String mountpoint)  {
        printTraceStart(LOG);
        GetSFDescriptionOutput result = new GetSFDescriptionOutputBuilder().build();
        try {
            if(getSfDescriptionMonitorService(mountpoint)!=null) {
                Future<RpcResult<GetSFDescriptionOutput>> futureResult = getSfDescriptionMonitorService(mountpoint).getSFDescription();
                result = futureResult.get().getResult();
            } else {
                result = createAStaticServiceFunctionDescription();
            }
            return result;
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return null;
    }

    public GetSFMonitoringInfoOutput getSFMonitorInfoFromNetconf(String mountpoint)  {
        printTraceStart(LOG);
        GetSFMonitoringInfoOutput result = new GetSFMonitoringInfoOutputBuilder().build();
        try {
            if(getSfDescriptionMonitorService(mountpoint)!=null) {
                Future<RpcResult<GetSFMonitoringInfoOutput>> futureResult = getSfDescriptionMonitorService(mountpoint).getSFMonitoringInfo();
                result = futureResult.get().getResult();
            } else {
                result = createAStaticServiceFunctionMonitor();
            }
            return result;
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return null;
    }

    private ServiceFunctionDescriptionMonitorReportService getSfDescriptionMonitorService(String mountpoint) {
        NodeId nodeId = new NodeId(mountpoint);
        InstanceIdentifier<?> path = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).toInstance();
        MountPointService mountService = SfcNetconfDataProvider.GetNetconfDataProvider().getMountService();
        Preconditions.checkState(mountService != null, "Mount service is null");
        Optional<MountPoint> mountPointInstance = mountService.getMountPoint(path);
        if(mountPointInstance.isPresent()) {
            final Optional<RpcConsumerRegistry> service = mountPointInstance.get().getService(RpcConsumerRegistry.class);
            if(service.isPresent()){
                return service.get().getRpcService(ServiceFunctionDescriptionMonitorReportService.class);
            } else  {
                return null;
            }
        } else {
            return null;
        }
    }

    private GetSFDescriptionOutput createAStaticServiceFunctionDescription() {
        List<PortBandwidth> portBandwidthList = new ArrayList<PortBandwidth>();

        Long[] data = null;
        data = new Long[10];
        for(int i = 0; i < 10; i++){
            data[i] = Long.parseLong(Integer.toString(i+1));
        }
        PortBandwidthKey portBandwidthKey = new PortBandwidthKey(data[0]);
        PortBandwidth portBandwidth= new PortBandwidthBuilder()
            .setIpaddress(new Ipv4Address("196.168.55.1"))
            .setKey(portBandwidthKey)
            .setMacaddress(new MacAddress("00:1e:67:a2:5f:f4"))
            .setPortId(data[0])
            .setSupportedBandwidth(data[1]).build();
        portBandwidthList.add(portBandwidth);

        PortsBandwidth portsBandwidth = new PortsBandwidthBuilder()
            .setPortBandwidth(portBandwidthList).build();
        //sf cap
        Capabilities cap = new CapabilitiesBuilder()
            .setPortsBandwidth(portsBandwidth)
            .setFIBSize(data[2])
            .setRIBSize(data[3])
            .setSupportedACLNumber(data[4])
            .setSupportedBandwidth(data[5])
            .setSupportedPacketRate(data[6]).build();

        //sf description
        DescriptionInfo descInfo = new DescriptionInfoBuilder()
            .setCapabilities(cap)
            .setNumberOfDataports(data[0]).build();
        GetSFDescriptionOutput result = new GetSFDescriptionOutputBuilder()
            .setDescriptionInfo(descInfo).build();

        return result;
    }

    private GetSFMonitoringInfoOutput createAStaticServiceFunctionMonitor() {
        List<PortBandwidthUtilization> portBandwidthUtilList = new ArrayList<PortBandwidthUtilization>();

        Long[] data = null;
        data = new Long[10];
        for(int i = 0; i < 10; i++){
            data[i] = Long.parseLong(Integer.toString(i+1));
        }
        PortBandwidthUtilizationKey portBandwidthUtilKey = new PortBandwidthUtilizationKey(data[0]);
        PortBandwidthUtilization portBandwidthUtil = new PortBandwidthUtilizationBuilder()
            .setBandwidthUtilization(data[2])
            .setKey(portBandwidthUtilKey)
            .setPortId(data[0]).build();
        portBandwidthUtilList.add(portBandwidthUtil);

        SFPortsBandwidthUtilization sfPortsBandwidthUtil = new SFPortsBandwidthUtilizationBuilder()
            .setPortBandwidthUtilization(portBandwidthUtilList).build();

        ResourceUtilization resrcUtil = new ResourceUtilizationBuilder()
            .setAvailableMemory(data[1])
            .setBandwidthUtilization(data[2])
            .setCPUUtilization(data[3])
            .setFIBUtilization(data[4])
            .setRIBUtilization(data[5])
            .setMemoryUtilization(data[6])
            .setPacketRateUtilization(data[7])
            .setPowerUtilization(data[8])
            .setSFPortsBandwidthUtilization(sfPortsBandwidthUtil).build();

        //sf monitor data
        boolean liveness = true;
        MonitoringInfo monInfo = new MonitoringInfoBuilder()
            .setResourceUtilization(resrcUtil)
            .setLiveness(liveness).build();

        GetSFMonitoringInfoOutput result = new GetSFMonitoringInfoOutputBuilder()
            .setMonitoringInfo(monInfo).build();

        return result;
    }

}
