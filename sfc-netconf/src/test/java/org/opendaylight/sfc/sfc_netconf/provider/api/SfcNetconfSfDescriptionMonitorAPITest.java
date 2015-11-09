/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_netconf.provider.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;


import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.Capabilities;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.PortsBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.PortsBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFDescriptionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFMonitoringInfoOutput;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.GetSFMonitoringInfoOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.SFPortsBandwidthUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.SFPortsBandwidthUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilizationBuilder;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilizationKey;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.ServiceFunctionDescriptionMonitorReportService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.yangtools.yang.common.RpcResult;
import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.any;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.junit.Before;

/**
 * SfcNetconfSfDescriptionMonitorAPITest Tester.
 *
 * @author Hongli Chen (honglix.chen@intel.com)
 * @version 1.0
 * @since 2015-11-02
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFunctionDescriptionMonitorReportService.class, SfcNetconfSfDescriptionMonitorAPI.class})
public class SfcNetconfSfDescriptionMonitorAPITest extends AbstractDataBrokerTest {
    private static final String IP_MGMT_ADDRESS = "192.168.1.2";
    private static final int DP_PORT = 6633;
    private static final SfName SF_NAME = new SfName("dummySF");
    private static final SfName SF_STATE_NAME = new SfName("dummySFS");
    private static final String SF_NAME1 = "sf1";
    private static SfcNetconfSfDescriptionMonitorAPI getSfDescMon;
    private MountPointService mountPointServiceMock;
    private Optional optionalBindingMountMock;
    private MountPoint mountPointMock;
    private Optional optionalMpRpcConsumerRegistry;
    private ServiceFunctionDescriptionMonitorReportService serviceMock;

    @Before
    public void setUp() throws Exception {
        getSfDescMon = PowerMockito.mock(SfcNetconfSfDescriptionMonitorAPI.class);
        mountPointServiceMock = mock(MountPointService.class);
        optionalBindingMountMock = mock(Optional.class);
        doReturn(true).when(optionalBindingMountMock).isPresent();
        mountPointMock = mock(MountPoint.class);
        doReturn(optionalBindingMountMock).when(mountPointServiceMock).getMountPoint(any(InstanceIdentifier.class));
        doReturn(mountPointMock).when(optionalBindingMountMock).get();
        optionalMpRpcConsumerRegistry = mock(Optional.class);
        RpcConsumerRegistry mpRpcConsumerRegistry = mock(RpcConsumerRegistry.class);
        doReturn(optionalMpRpcConsumerRegistry).when(mountPointMock).getService(RpcConsumerRegistry.class);
        doReturn(true).when(optionalMpRpcConsumerRegistry).isPresent();
        doReturn(mpRpcConsumerRegistry).when(optionalMpRpcConsumerRegistry).get();
        serviceMock = mock(ServiceFunctionDescriptionMonitorReportService.class);
        doReturn(serviceMock).when(mpRpcConsumerRegistry).getRpcService(ServiceFunctionDescriptionMonitorReportService.class);
    }

    @Test
    public void testGetSFDescriptionInfoFromNetconf() throws Exception {
        List<PortBandwidth> portBandwidthList = new ArrayList<>();

        Long[] data;
        data = new Long[10];
        for (int i = 0; i < 10; i++) {
            data[i] = Long.parseLong(Integer.toString(i + 1));
        }

        PortNumber portNumber = new PortNumber(DP_PORT);
        IpAddress ipAddress = new IpAddress(new Ipv4Address(IP_MGMT_ADDRESS));
        PortBandwidthKey portBandwidthKey = new PortBandwidthKey(data[0]);
        PortBandwidth portBandwidth = new PortBandwidthBuilder().setIpaddress(new Ipv4Address(IP_MGMT_ADDRESS))
            .setKey(portBandwidthKey)
            .setMacaddress(new MacAddress("00:1e:67:a2:5f:f4"))
            .setPortId(data[0])
            .setSupportedBandwidth(data[1])
            .build();
        portBandwidthList.add(portBandwidth);

        PortsBandwidth portsBandwidth = new PortsBandwidthBuilder().setPortBandwidth(portBandwidthList).build();
        // sf cap
        Capabilities cap = new CapabilitiesBuilder().setPortsBandwidth(portsBandwidth)
            .setFIBSize(data[2])
            .setRIBSize(data[3])
            .setSupportedACLNumber(data[4])
            .setSupportedBandwidth(data[5])
            .setSupportedPacketRate(data[6])
            .build();

        // sf description
        long numberOfDataports = 1;
        org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.description.output.DescriptionInfo descInfoTemp =
                new org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.description.output.DescriptionInfoBuilder()
                    .setCapabilities(cap)
                    .setDataPlanePort(portNumber)
                    .setDataPlaneIp(ipAddress)
                    .setType("DPI")
                    .setNumberOfDataports(numberOfDataports).build();

        DescriptionInfo descInfo = new DescriptionInfoBuilder(descInfoTemp).build();

        GetSFDescriptionOutput getSFDescriptionOutput = new GetSFDescriptionOutputBuilder()
                                                            .setDescriptionInfo(descInfoTemp).build();

        Future<RpcResult<GetSFDescriptionOutput>> future =
            Futures.immediateFuture(RpcResultBuilder.success(getSFDescriptionOutput).build());

        ServiceFunctionDescriptionMonitorReportService service = PowerMockito.mock(ServiceFunctionDescriptionMonitorReportService.class);

        PowerMockito.when(service.getSFDescription()).thenReturn(future);

        GetSFDescriptionOutput getSFDescriptionOutputResult = null;
        if (service != null) {
            Future<RpcResult<GetSFDescriptionOutput>> result = service.getSFDescription();
            RpcResult<GetSFDescriptionOutput> output = result.get();
            if (output.isSuccessful()) {
                getSFDescriptionOutputResult = output.getResult();
            }
            else {
                System.out.println("getSFDescription() failed.");
            }
        }
        assertEquals("Must be equal", getSFDescriptionOutput, getSFDescriptionOutputResult);
    }

    @Test
    public void testGetSFMonitorInfoFromNetconf() throws Exception {
        List<PortBandwidthUtilization> portBandwidthUtilList = new ArrayList<>();

        Long[] data = new Long[10];
        for (int i = 0; i < 10; i++) {
            data[i] = Long.parseLong(Integer.toString(i + 1));
        }
        PortBandwidthUtilizationKey portBandwidthUtilKey = new PortBandwidthUtilizationKey(data[0]);
        PortBandwidthUtilization portBandwidthUtil = new PortBandwidthUtilizationBuilder()
            .setBandwidthUtilization(data[2]).setKey(portBandwidthUtilKey).setPortId(data[0]).build();
        portBandwidthUtilList.add(portBandwidthUtil);

        SFPortsBandwidthUtilization sfPortsBandwidthUtil =
                new SFPortsBandwidthUtilizationBuilder().setPortBandwidthUtilization(portBandwidthUtilList).build();

        ResourceUtilization resrcUtil = new ResourceUtilizationBuilder().setAvailableMemory(data[1])
                .setBandwidthUtilization(data[2])
                .setCPUUtilization(data[3])
                .setFIBUtilization(data[4])
                .setRIBUtilization(data[5])
                .setMemoryUtilization(data[6])
                .setPacketRateUtilization(data[7])
                .setPowerUtilization(data[8])
                .setSFPortsBandwidthUtilization(sfPortsBandwidthUtil)
                .build();

        // sf monitor data
        org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.monitoring.info.output.MonitoringInfo monInfoTemp =
                new org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.get.sf.monitoring.info.output.MonitoringInfoBuilder()
                    .setResourceUtilization(resrcUtil)
                    .setLiveness(true).build();

        MonitoringInfo monInfo = new MonitoringInfoBuilder(monInfoTemp).build();
        GetSFMonitoringInfoOutput getSFMonitoringInfoOutput = new GetSFMonitoringInfoOutputBuilder()
                                                            .setMonitoringInfo(monInfoTemp).build();
        Future<RpcResult<GetSFMonitoringInfoOutput>> future =
        Futures.immediateFuture(RpcResultBuilder.success(getSFMonitoringInfoOutput).build());

        ServiceFunctionDescriptionMonitorReportService service = PowerMockito.mock(ServiceFunctionDescriptionMonitorReportService.class);

        PowerMockito.when(service.getSFMonitoringInfo()).thenReturn(future);

        GetSFMonitoringInfoOutput getSFMonitoringInfoOutputResult = null;
        if (service != null) {
            Future<RpcResult<GetSFMonitoringInfoOutput>> result = service.getSFMonitoringInfo();
            RpcResult<GetSFMonitoringInfoOutput> output = result.get();
            if (output.isSuccessful()) {
                getSFMonitoringInfoOutputResult = output.getResult();
            }
            else {
                System.out.println("getSFMonitoringInfo() failed.");
            }
        }
        assertEquals("Must be equal", getSFMonitoringInfoOutput, getSFMonitoringInfoOutputResult);
    }

    @Test
    public void testGetSfDescriptionMonitorService() throws Exception {
        InstanceIdentifier<Topology> NETCONF_TOPO_IID =
            InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class,
                   new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));
        InstanceIdentifier<?> nodeIID = NETCONF_TOPO_IID.child(Node.class, new NodeKey(new NodeId(SF_NAME1)));
        Optional<MountPoint> optionalMountPoint = mountPointServiceMock.getMountPoint(nodeIID);
        MountPoint mountPoint = optionalMountPoint.get();
        Optional<RpcConsumerRegistry> optionalRpcConsumerRegistry = mountPoint.getService(RpcConsumerRegistry.class);
        RpcConsumerRegistry rpcConsumerRegistry = optionalRpcConsumerRegistry.get();
        ServiceFunctionDescriptionMonitorReportService service = rpcConsumerRegistry.getRpcService(ServiceFunctionDescriptionMonitorReportService.class);
        assertEquals("Must be equal", serviceMock, service);
    }
}
