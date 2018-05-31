/*
 * Copyright (c) 2015, 2017 Intel Corp. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.StatisticByTimestamp;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.Capabilities;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.PortsBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.SFPortsBandwidthUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilization;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SfstateExporter extends AbstractExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(SfstateExporter.class);

    public static final String SERVICE_FUNCTION_STATE = "service-function-state";
    public static final String NAME = "name";
    public static final String SERVICE_STATISTICS = "service-statistic";
    public static final String SERVICE_STATISTICS_BY_TIMESTAMP = "statistic-by-timestamp";
    public static final String TIMESTAMP = "timestamp";
    public static final String BYTES_IN = "bytes-in";
    public static final String BYTES_OUT = "bytes-out";
    public static final String PACKETS_IN = "packets-in";
    public static final String PACKETS_OUT = "packet-out";
    public static final String SF_SERVICE_PATH = "sf-service-path";
    public static final String SFC_SF_DESC_MON = "sfc-sf-desc-mon";
    public static final String DESCRIPTION_INFO = "description-info";
    public static final String DATA_PLANE_IP = "data-plane-ip";
    public static final String DATA_PLANE_PORT = "data-plane-port";
    public static final String TYPE = "type";
    public static final String NUMBER_OF_DATAPORTS = "number-of-dataports";
    public static final String CAPABILITIES = "capabilities";
    public static final String SUPPORTED_PACKET_RATE = "supported-packet-rate";
    public static final String SUPPORTED_BANDWIDTH = "supported-bandwidth";
    public static final String SUPPORTED_ACL_NUMBER = "supported-ACL-number";
    public static final String RIB_SIZE = "RIB-size";
    public static final String FIB_SIZE = "FIB-size";
    public static final String PORTS_BANDWIDTH = "ports-bandwidth";
    public static final String PORT_BANDWIDTH = "port-bandwidth";
    public static final String PORT_ID = "port-id";
    public static final String IPADDRESS = "ipaddress";
    public static final String MACADDRESS = "macaddress";

    public static final String MONITOR_INFO = "monitoring-info";
    public static final String LIVENESS = "liveness";
    public static final String RESOURCE_UTILIZATION = "resource-utilization";
    public static final String PACKET_RATE_UTILIZATION = "packet-rate-utilization";
    public static final String BANDWIDTH_UTILIZATION = "bandwidth-utilization";
    public static final String CPU_UTILIZATION = "CPU-utilization";
    public static final String MEMORY_UTILIZATION = "memory-utilization";
    public static final String AVAILABLE_MEMORY = "available-memory";
    public static final String RIB_UTILIZATION = "RIB-utilization";
    public static final String FIB_UTILIZATION = "FIB-utilization";
    public static final String POWER_UTILIZATION = "power-utilization";
    public static final String SF_PORTS_BANDWIDTH_UTILIZATION = "SF-ports-bandwidth-utilization";
    public static final String PORT_BANDWIDTH_UTILIZATION = "port-bandwidth-utilization";
    public static final String RX_PACKET = "rx-packet";
    public static final String TX_PACKET = "tx-packet";
    public static final String RX_BYTES = "rx-bytes";
    public static final String TX_BYTES = "tx-bytes";
    public static final String RX_BYTES_RATE = "rx-bytes-rate";
    public static final String TX_BYTES_RATE = "tx-bytes-rate";
    public static final String RX_PACKET_RATE = "rx-packet-rate";
    public static final String TX_PACKET_RATE = "tx-packet-rate";

    public static final String SERVICE_FUNCTION_DESCRIPTION_MONITOR_PREFIX = "service-function-description-monitor:";

    @Override
    public String exportJson(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof ServiceFunctionState) {
            ServiceFunctionState sfstate = (ServiceFunctionState) dataObject;

            ObjectNode sfstateNode = mapper.createObjectNode();

            if (sfstate.getName() != null && sfstate.getName().getValue() != null) {
                sfstateNode.put(NAME, sfstate.getName().getValue());
            }
            if (sfstate.getStatisticByTimestamp() != null) {
                sfstateNode.put(SERVICE_STATISTICS_BY_TIMESTAMP,
                        getStatisticByTimestampObjectNode(sfstate.getStatisticByTimestamp()));
            }

            if (sfstate.getSfServicePath() != null) {
                ArrayNode servicePathArray = mapper.createArrayNode();
                for (SfServicePath sfServicePath : sfstate.getSfServicePath()) {
                    ObjectNode servicePathNode = this.getSfServicePathObjectNode(sfServicePath);
                    servicePathArray.add(servicePathNode);
                }
                sfstateNode.putArray(SF_SERVICE_PATH).addAll(servicePathArray);
            }

            sfstateNode.put(SERVICE_FUNCTION_DESCRIPTION_MONITOR_PREFIX + SFC_SF_DESC_MON,
                    getSfDescriptionMonitorObjectNode(sfstate));
            ArrayNode sfstateArray = mapper.createArrayNode();
            sfstateArray.add(sfstateNode);

            try {
                Object sfstateObject = mapper.treeToValue(sfstateArray, Object.class);
                ret = mapper.writeValueAsString(sfstateObject);
                ret = "{\"" + SERVICE_FUNCTION_STATE + "\":" + ret + "}";
                LOG.debug("Created Service Function State JSON: {}", ret);
            } catch (JsonProcessingException e) {
                LOG.error("Error during creation of JSON for Service Function State {}", sfstate.getName());
            }
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionState");
        }

        return ret;
    }

    @Override
    public String exportJsonNameOnly(DataObject dataObject) {
        String ret = null;

        if (dataObject instanceof ServiceFunctionState) {
            ServiceFunctionState obj = (ServiceFunctionState) dataObject;

            ObjectNode node = mapper.createObjectNode();
            if (obj.getName() != null) {
                node.put(NAME, obj.getName().getValue());
            }
            ArrayNode sfstateArray = mapper.createArrayNode();
            sfstateArray.add(node);
            ret = "{\"" + SERVICE_FUNCTION_STATE + "\":" + sfstateArray.toString() + "}";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionState");
        }

        return ret;
    }

    private ObjectNode getSfDescriptionMonitorObjectNode(ServiceFunctionState sfstate) {
        if (sfstate == null || sfstate.augmentation(ServiceFunctionState1.class) == null) {
            return null;
        }
        ServiceFunctionState1 serviceFunctionState1 = sfstate.augmentation(ServiceFunctionState1.class);

        if (serviceFunctionState1 != null) {
            return this.getDescriptionMonitorObjectNode(serviceFunctionState1.getSfcSfDescMon());
        }

        return null;
    }

    private ObjectNode getDescriptionMonitorObjectNode(SfcSfDescMon sfcSfDescMon) {
        if (sfcSfDescMon == null) {
            return null;
        }
        ObjectNode sfcSfDescMonNode = mapper.createObjectNode();

        if (sfcSfDescMon.getDescriptionInfo() != null) {
            ObjectNode sfDescriptionInfoNode = mapper.createObjectNode();
            DescriptionInfo sfDescriptionInfo = sfcSfDescMon.getDescriptionInfo();
            if (sfDescriptionInfo.getDataPlaneIp() != null) {
                sfDescriptionInfoNode.put(DATA_PLANE_IP,
                        sfDescriptionInfo.getDataPlaneIp().getIpv4Address().getValue());
            }

            if (sfDescriptionInfo.getDataPlanePort() != null) {
                sfDescriptionInfoNode.put(DATA_PLANE_PORT, sfDescriptionInfo.getDataPlanePort().getValue());
            }

            if (sfDescriptionInfo.getType() != null) {
                sfDescriptionInfoNode.put(TYPE, sfDescriptionInfo.getType());
            }

            if (sfDescriptionInfo.getNumberOfDataports() != null) {
                sfDescriptionInfoNode.put(NUMBER_OF_DATAPORTS, sfDescriptionInfo.getNumberOfDataports());
            }

            if (sfDescriptionInfo.getCapabilities() != null) {
                ObjectNode capabilitiesNode = mapper.createObjectNode();
                Capabilities capabilities = sfDescriptionInfo.getCapabilities();

                capabilitiesNode.put(SUPPORTED_PACKET_RATE, capabilities.getSupportedPacketRate());
                capabilitiesNode.put(SUPPORTED_BANDWIDTH, capabilities.getSupportedBandwidth());
                capabilitiesNode.put(SUPPORTED_ACL_NUMBER, capabilities.getSupportedACLNumber());
                capabilitiesNode.put(RIB_SIZE, capabilities.getRIBSize());
                capabilitiesNode.put(FIB_SIZE, capabilities.getFIBSize());

                if (capabilities.getPortsBandwidth() != null) {
                    ArrayNode portsBandwidthArray = mapper.createArrayNode();
                    ObjectNode portBandwidthArrayNode = mapper.createObjectNode();
                    PortsBandwidth portsBandwidth = capabilities.getPortsBandwidth();
                    for (PortBandwidth portBandwidth : portsBandwidth.getPortBandwidth()) {
                        ObjectNode portBandwidthNode = mapper.createObjectNode();
                        portBandwidthNode.put(PORT_ID, portBandwidth.getPortId());
                        portBandwidthNode.put(IPADDRESS, portBandwidth.getIpaddress().getValue());
                        portBandwidthNode.put(MACADDRESS, portBandwidth.getMacaddress().getValue());
                        portBandwidthNode.put(SUPPORTED_BANDWIDTH, portBandwidth.getSupportedBandwidth());
                        portsBandwidthArray.add(portBandwidthNode);
                    }
                    portBandwidthArrayNode.put(PORT_BANDWIDTH, portsBandwidthArray);
                    capabilitiesNode.put(PORTS_BANDWIDTH, portBandwidthArrayNode);
                }
                sfDescriptionInfoNode.put(CAPABILITIES, capabilitiesNode);
            }
            sfcSfDescMonNode.put(DESCRIPTION_INFO, sfDescriptionInfoNode);
        }

        if (sfcSfDescMon.getMonitoringInfo() != null) {
            ObjectNode sfMonitoringInfoNode = mapper.createObjectNode();
            MonitoringInfo sfMonitoringInfo = sfcSfDescMon.getMonitoringInfo();

            if (sfMonitoringInfo.isLiveness() != null) {
                sfMonitoringInfoNode.put(LIVENESS, sfMonitoringInfo.isLiveness());
            }

            if (sfMonitoringInfo.getResourceUtilization() != null) {
                ObjectNode resourceUtilizationNode = mapper.createObjectNode();
                ResourceUtilization resourceUtilization = sfMonitoringInfo.getResourceUtilization();

                resourceUtilizationNode.put(PACKET_RATE_UTILIZATION, resourceUtilization.getPacketRateUtilization());
                resourceUtilizationNode.put(BANDWIDTH_UTILIZATION, resourceUtilization.getBandwidthUtilization());
                resourceUtilizationNode.put(CPU_UTILIZATION, resourceUtilization.getCPUUtilization());
                resourceUtilizationNode.put(MEMORY_UTILIZATION, resourceUtilization.getMemoryUtilization());
                resourceUtilizationNode.put(AVAILABLE_MEMORY, resourceUtilization.getAvailableMemory());
                resourceUtilizationNode.put(RIB_UTILIZATION, resourceUtilization.getRIBUtilization());
                resourceUtilizationNode.put(FIB_UTILIZATION, resourceUtilization.getFIBUtilization());
                resourceUtilizationNode.put(POWER_UTILIZATION, resourceUtilization.getPowerUtilization());

                if (resourceUtilization.getSFPortsBandwidthUtilization() != null) {
                    ArrayNode portsBandwidthUtilizationArray = mapper.createArrayNode();
                    ObjectNode portBandwidthUtilizationArrayNode = mapper.createObjectNode();
                    SFPortsBandwidthUtilization portsBandwidthUtilization = resourceUtilization
                            .getSFPortsBandwidthUtilization();
                    for (PortBandwidthUtilization portBandwidthUtilization : portsBandwidthUtilization
                            .getPortBandwidthUtilization()) {
                        ObjectNode portBandwidthUtilizationNode = mapper.createObjectNode();
                        portBandwidthUtilizationNode.put(PORT_ID, portBandwidthUtilization.getPortId());
                        portBandwidthUtilizationNode.put(RX_PACKET,
                                portBandwidthUtilization.getRxPacket().getValue().intValue());
                        portBandwidthUtilizationNode.put(TX_PACKET,
                                portBandwidthUtilization.getTxPacket().getValue().intValue());
                        portBandwidthUtilizationNode.put(RX_BYTES,
                                portBandwidthUtilization.getRxBytes().getValue().intValue());
                        portBandwidthUtilizationNode.put(TX_BYTES,
                                portBandwidthUtilization.getTxBytes().getValue().intValue());
                        portBandwidthUtilizationNode.put(RX_BYTES_RATE, portBandwidthUtilization.getRxBytesRate());
                        portBandwidthUtilizationNode.put(TX_BYTES_RATE, portBandwidthUtilization.getTxBytesRate());
                        portBandwidthUtilizationNode.put(RX_PACKET_RATE, portBandwidthUtilization.getRxPacketRate());
                        portBandwidthUtilizationNode.put(TX_PACKET_RATE, portBandwidthUtilization.getTxPacketRate());
                        portBandwidthUtilizationNode.put(BANDWIDTH_UTILIZATION,
                                portBandwidthUtilization.getBandwidthUtilization());
                        portsBandwidthUtilizationArray.add(portBandwidthUtilizationNode);
                    }
                    portBandwidthUtilizationArrayNode.put(PORT_BANDWIDTH_UTILIZATION, portsBandwidthUtilizationArray);
                    resourceUtilizationNode.put(SF_PORTS_BANDWIDTH_UTILIZATION, portBandwidthUtilizationArrayNode);
                }
                sfMonitoringInfoNode.put(RESOURCE_UTILIZATION, resourceUtilizationNode);
            }
            sfcSfDescMonNode.put(MONITOR_INFO, sfMonitoringInfoNode);
        }

        return sfcSfDescMonNode;
    }

    private ArrayNode getStatisticByTimestampObjectNode(List<StatisticByTimestamp> serviceStatistics) {
        if (serviceStatistics == null) {
            return null;
        }

        ArrayNode statisticsByTimeArray = mapper.createArrayNode();

        for (StatisticByTimestamp statByTimestamp : serviceStatistics) {
            ObjectNode serviceStatisticNode = mapper.createObjectNode();
            serviceStatisticNode.put(BYTES_IN,
                    statByTimestamp.getServiceStatistic().getBytesIn().getValue().longValue());
            serviceStatisticNode.put(BYTES_OUT,
                    statByTimestamp.getServiceStatistic().getBytesOut().getValue().longValue());
            serviceStatisticNode.put(PACKETS_IN,
                    statByTimestamp.getServiceStatistic().getPacketsIn().getValue().longValue());
            serviceStatisticNode.put(PACKETS_OUT,
                    statByTimestamp.getServiceStatistic().getPacketsOut().getValue().longValue());
            ObjectNode statisticByTimeNode = mapper.createObjectNode();
            statisticByTimeNode.put(TIMESTAMP, statByTimestamp.getTimestamp().longValue());
            statisticByTimeNode.put(SERVICE_STATISTICS, serviceStatisticNode);
            statisticsByTimeArray.add(statisticByTimeNode);
        }

        return statisticsByTimeArray;
    }

    private ObjectNode getSfServicePathObjectNode(SfServicePath sfServicePath) {
        if (sfServicePath == null) {
            return null;
        }
        ObjectNode sfServicePathNode = mapper.createObjectNode();
        if (sfServicePath.getName() != null && sfServicePath.getName().getValue() != null) {
            sfServicePathNode.put(NAME, sfServicePath.getName().getValue());
        }

        if (sfServicePath.getStatisticByTimestamp() != null) {
            ArrayNode serviceStatisticsNode = getStatisticByTimestampObjectNode(
                    sfServicePath.getStatisticByTimestamp());
            sfServicePathNode.put(SERVICE_STATISTICS_BY_TIMESTAMP, serviceStatisticsNode);
        }
        return sfServicePathNode;
    }
}
