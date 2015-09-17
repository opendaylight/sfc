/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.ServiceStatistics;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.DescriptionInfo;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.Capabilities;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.PortsBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.description.capabilities.ports.bandwidth.PortBandwidth;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.ResourceUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.SFPortsBandwidthUtilization;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rpt.rev141105.sf.monitoring.info.resource.utilization.sf.ports.bandwidth.utilization.PortBandwidthUtilization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfsExporterFactory  implements ExporterFactory {

    public static final String _SERVICE_FUNCTION_STATE = SfsExporter._SERVICE_FUNCTION_STATE;
    public static final String _NAME = SfsExporter._NAME;

    @Override
    public Exporter getExporter() {
        return new SfsExporter();
    }
}

class SfsExporter extends AbstractExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(SfsExporter.class);

    public static final String _SERVICE_FUNCTION_STATE = "service-function-state";
    public static final String _NAME = "name";
    public static final String _SERVICE_STATISTICS = "service-statistics";
    public static final String _BYTES_IN = "bytes-in";
    public static final String _BYTES_OUT = "bytes-out";
    public static final String _PACKETS_IN = "packets-in";
    public static final String _PACKETS_OUT = "packet-out";
    public static final String _SF_SERVICE_PATH = "sf-service-path";
    public static final String _SFC_SF_DESC_MON = "sfc-sf-desc-mon";
    public static final String _DESCRIPTION_INFO = "description-info";
    public static final String _DATA_PLANE_IP = "data-plane-ip";
    public static final String _DATA_PLANE_PORT = "data-plane-port";
    public static final String _TYPE = "type";
    public static final String _NUMBER_OF_DATAPORTS = "number-of-dataports";
    public static final String _CAPABILITIES = "capabilities";
    public static final String _SUPPORTED_PACKET_RATE = "supported-packet-rate";
    public static final String _SUPPORTED_BANDWIDTH = "supported-bandwidth";
    public static final String _SUPPORTED_ACL_NUMBER = "supported-ACL-number";
    public static final String _RIB_SIZE = "RIB-size";
    public static final String _FIB_SIZE = "FIB-size";
    public static final String _PORTS_BANDWIDTH = "ports-bandwidth";
    public static final String _PORT_BANDWIDTH = "port-bandwidth";
    public static final String _PORT_ID = "port-id";
    public static final String _IPADDRESS = "ipaddress";
    public static final String _MACADDRESS = "macaddress";

    public static final String _MONITOR_INFO = "monitoring-info";
    public static final String _LIVENESS = "liveness";
    public static final String _RESOURCE_UTILIZATION = "resource-utilization";
    public static final String _PACKET_RATE_UTILIZATION = "packet-rate-utilization";
    public static final String _BANDWIDTH_UTILIZATION = "bandwidth-utilization";
    public static final String _CPU_UTILIZATION = "CPU-utilization";
    public static final String _MEMORY_UTILIZATION = "memory-utilization";
    public static final String _AVAILABLE_MEMORY = "available-memory";
    public static final String _RIB_UTILIZATION = "RIB-utilization";
    public static final String _FIB_UTILIZATION = "FIB-utilization";
    public static final String _POWER_UTILIZATION = "power-utilization";
    public static final String _SF_PORTS_BANDWIDTH_UTILIZATION = "SF-ports-bandwidth-utilization";
    public static final String _PORT_BANDWIDTH_UTILIZATION = "port-bandwidth-utilization";
    public static final String _RX_PACKET = "rx-packet";
    public static final String _TX_PACKET = "tx-packet";
    public static final String _RX_BYTES = "rx-bytes";
    public static final String _TX_BYTES = "tx-bytes";
    public static final String _RX_BYTES_RATE = "rx-bytes-rate";
    public static final String _TX_BYTES_RATE = "tx-bytes-rate";
    public static final String _RX_PACKET_RATE = "rx-packet-rate";
    public static final String _TX_PACKET_RATE = "tx-packet-rate";

    public static final String SERVICE_FUNCTION_DESCRIPTION_MONITOR_PREFIX = "service-function-description-monitor:";

    @Override
    public String exportJson(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof ServiceFunctionState) {
            ServiceFunctionState sfs = (ServiceFunctionState) dataObject;
            ArrayNode sfsArray = mapper.createArrayNode();
            ObjectNode sfsNode = mapper.createObjectNode();

            sfsNode.put(_NAME, sfs.getName());
            if (sfs.getServiceStatistics() != null) {
                sfsNode.put(_SERVICE_STATISTICS, getServiceStatisticsObjectNode(sfs.getServiceStatistics()));
            }

            if (sfs.getSfServicePath() != null) {
                ArrayNode servicePathArray = mapper.createArrayNode();
                for (SfServicePath sfServicePath : sfs.getSfServicePath()) {
                    ObjectNode servicePathNode = this.getSfServicePathObjectNode(sfServicePath);
                    servicePathArray.add(servicePathNode);
                }
                sfsNode.putArray(_SF_SERVICE_PATH).addAll(servicePathArray);
            }

            sfsNode.put(SERVICE_FUNCTION_DESCRIPTION_MONITOR_PREFIX+_SFC_SF_DESC_MON, getSfDescriptionMonitorObjectNode(sfs));

            sfsArray.add(sfsNode);

            try {
                Object sfsObject = mapper.treeToValue(sfsArray, Object.class);
                ret = mapper.writeValueAsString(sfsObject);
                ret = "{\"" + _SERVICE_FUNCTION_STATE + "\":" + ret + "}";
                LOG.debug("Created Service Function State JSON: {}", ret);
            } catch (JsonProcessingException e) {
                LOG.error("Error during creation of JSON for Service Function State {}", sfs.getName());
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
            node.put(_NAME, obj.getName());
            ArrayNode sfsArray = mapper.createArrayNode();
            sfsArray.add(node);
            ret = "{\"" + _SERVICE_FUNCTION_STATE + "\":" + sfsArray.toString() + "}";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionState");
        }

        return ret;
    }

    private ObjectNode getSfDescriptionMonitorObjectNode (ServiceFunctionState sfs) {
        if (sfs == null || sfs.getAugmentation(ServiceFunctionState1.class) == null) {
            return null;
        }
        ServiceFunctionState1 serviceFunctionState1 = sfs.getAugmentation(ServiceFunctionState1.class);

        if (serviceFunctionState1 != null) {
            return this.getDescriptionMonitorObjectNode(serviceFunctionState1.getSfcSfDescMon());
        }

        return null;
    }

    private ObjectNode getDescriptionMonitorObjectNode (SfcSfDescMon sfcSfDescMon) {
        if (sfcSfDescMon == null) {
            return null;
        }
        ObjectNode sfcSfDescMonNode = mapper.createObjectNode();

        if (sfcSfDescMon.getDescriptionInfo()!=null) {
            ObjectNode sfDescriptionInfoNode = mapper.createObjectNode();
            DescriptionInfo sfDescriptionInfo = sfcSfDescMon.getDescriptionInfo();
            if (sfDescriptionInfo.getDataPlaneIp() != null) {
                sfDescriptionInfoNode.put(_DATA_PLANE_IP, sfDescriptionInfo.getDataPlaneIp().getIpv4Address().getValue());
            }

            if (sfDescriptionInfo.getDataPlanePort() != null) {
                sfDescriptionInfoNode.put(_DATA_PLANE_PORT, sfDescriptionInfo.getDataPlanePort().getValue());
            }

            if (sfDescriptionInfo.getType() != null) {
                sfDescriptionInfoNode.put(_TYPE, sfDescriptionInfo.getType());
            }

            if (sfDescriptionInfo.getNumberOfDataports() != null) {
                sfDescriptionInfoNode.put(_NUMBER_OF_DATAPORTS, sfDescriptionInfo.getNumberOfDataports());
            }

            if (sfDescriptionInfo.getCapabilities()!=null) {
                ObjectNode capabilitiesNode = mapper.createObjectNode();
                Capabilities capabilities = sfDescriptionInfo.getCapabilities();

                capabilitiesNode.put(_SUPPORTED_PACKET_RATE, capabilities.getSupportedPacketRate());
                capabilitiesNode.put(_SUPPORTED_BANDWIDTH, capabilities.getSupportedBandwidth());
                capabilitiesNode.put(_SUPPORTED_ACL_NUMBER, capabilities.getSupportedACLNumber());
                capabilitiesNode.put(_RIB_SIZE, capabilities.getRIBSize());
                capabilitiesNode.put(_FIB_SIZE, capabilities.getFIBSize());

                if (capabilities.getPortsBandwidth()!=null) {
                    ArrayNode portsBandwidthArray = mapper.createArrayNode();
                    ObjectNode portBandwidthArrayNode = mapper.createObjectNode();
                    PortsBandwidth portsBandwidth = capabilities.getPortsBandwidth();
                    for (PortBandwidth portBandwidth : portsBandwidth.getPortBandwidth()) {
                        ObjectNode portBandwidthNode = mapper.createObjectNode();
                        portBandwidthNode.put(_PORT_ID, portBandwidth.getPortId());
                        portBandwidthNode.put(_IPADDRESS, portBandwidth.getIpaddress().getValue());
                        portBandwidthNode.put(_MACADDRESS, portBandwidth.getMacaddress().getValue());
                        portBandwidthNode.put(_SUPPORTED_BANDWIDTH, portBandwidth.getSupportedBandwidth());
                        portsBandwidthArray.add(portBandwidthNode);
                    }
                    portBandwidthArrayNode.put(_PORT_BANDWIDTH, portsBandwidthArray);
                    capabilitiesNode.put(_PORTS_BANDWIDTH, portBandwidthArrayNode);
                }
                sfDescriptionInfoNode.put(_CAPABILITIES, capabilitiesNode);
            }
            sfcSfDescMonNode.put(_DESCRIPTION_INFO,sfDescriptionInfoNode);
        }

        if (sfcSfDescMon.getMonitoringInfo()!=null) {
            ObjectNode sfMonitoringInfoNode = mapper.createObjectNode();
            MonitoringInfo sfMonitoringInfo = sfcSfDescMon.getMonitoringInfo();

            if (sfMonitoringInfo.isLiveness() != null) {
                sfMonitoringInfoNode.put(_LIVENESS, sfMonitoringInfo.isLiveness());
            }

            if (sfMonitoringInfo.getResourceUtilization()!=null) {
                ObjectNode resourceUtilizationNode = mapper.createObjectNode();
                ResourceUtilization resourceUtilization = sfMonitoringInfo.getResourceUtilization();

                resourceUtilizationNode.put(_PACKET_RATE_UTILIZATION, resourceUtilization.getPacketRateUtilization());
                resourceUtilizationNode.put(_BANDWIDTH_UTILIZATION, resourceUtilization.getBandwidthUtilization());
                resourceUtilizationNode.put(_CPU_UTILIZATION, resourceUtilization.getCPUUtilization());
                resourceUtilizationNode.put(_MEMORY_UTILIZATION, resourceUtilization.getMemoryUtilization());
                resourceUtilizationNode.put(_AVAILABLE_MEMORY, resourceUtilization.getAvailableMemory());
                resourceUtilizationNode.put(_RIB_UTILIZATION, resourceUtilization.getRIBUtilization());
                resourceUtilizationNode.put(_FIB_UTILIZATION, resourceUtilization.getFIBUtilization());
                resourceUtilizationNode.put(_POWER_UTILIZATION, resourceUtilization.getPowerUtilization());

                if (resourceUtilization.getSFPortsBandwidthUtilization()!=null) {
                    ArrayNode portsBandwidthUtilizationArray = mapper.createArrayNode();
                    ObjectNode portBandwidthUtilizationArrayNode = mapper.createObjectNode();
                    SFPortsBandwidthUtilization portsBandwidthUtilization = resourceUtilization.getSFPortsBandwidthUtilization();
                    for (PortBandwidthUtilization portBandwidthUtilization : portsBandwidthUtilization.getPortBandwidthUtilization()) {
                        ObjectNode portBandwidthUtilizationNode = mapper.createObjectNode();
                        portBandwidthUtilizationNode.put(_PORT_ID, portBandwidthUtilization.getPortId());
                        portBandwidthUtilizationNode.put(_RX_PACKET, portBandwidthUtilization.getRxPacket().getValue().intValue());
                        portBandwidthUtilizationNode.put(_TX_PACKET, portBandwidthUtilization.getTxPacket().getValue().intValue());
                        portBandwidthUtilizationNode.put(_RX_BYTES, portBandwidthUtilization.getRxBytes().getValue().intValue());
                        portBandwidthUtilizationNode.put(_TX_BYTES, portBandwidthUtilization.getTxBytes().getValue().intValue());
                        portBandwidthUtilizationNode.put(_RX_BYTES_RATE, portBandwidthUtilization.getRxBytesRate());
                        portBandwidthUtilizationNode.put(_TX_BYTES_RATE, portBandwidthUtilization.getTxBytesRate());
                        portBandwidthUtilizationNode.put(_RX_PACKET_RATE, portBandwidthUtilization.getRxPacketRate());
                        portBandwidthUtilizationNode.put(_TX_PACKET_RATE, portBandwidthUtilization.getTxPacketRate());
                        portBandwidthUtilizationNode.put(_BANDWIDTH_UTILIZATION, portBandwidthUtilization.getBandwidthUtilization());
                        portsBandwidthUtilizationArray.add(portBandwidthUtilizationNode);
                    }
                    portBandwidthUtilizationArrayNode.put(_PORT_BANDWIDTH_UTILIZATION, portsBandwidthUtilizationArray);
                    resourceUtilizationNode.put(_SF_PORTS_BANDWIDTH_UTILIZATION, portBandwidthUtilizationArrayNode);
                }
                sfMonitoringInfoNode.put(_RESOURCE_UTILIZATION, resourceUtilizationNode);
            }
            sfcSfDescMonNode.put(_MONITOR_INFO,sfMonitoringInfoNode);
        }

        return sfcSfDescMonNode;
    }

    private ObjectNode getServiceStatisticsObjectNode (ServiceStatistics serviceStatistics) {
        if (serviceStatistics == null) {
            return null;
        }

        ObjectNode serviceStatisticsNode = mapper.createObjectNode();

        if (serviceStatistics.getBytesIn() != null) {
            serviceStatisticsNode.put(_BYTES_IN, serviceStatistics.getBytesIn().getValue().intValue());
        }

        if (serviceStatistics.getBytesOut() != null) {
            serviceStatisticsNode.put(_BYTES_OUT, serviceStatistics.getBytesOut().getValue().intValue());
        }

        if (serviceStatistics.getPacketOut() != null) {
            serviceStatisticsNode.put(_PACKETS_IN, serviceStatistics.getPacketsIn().getValue().intValue());
        }

        if (serviceStatistics.getPacketsIn() != null) {
            serviceStatisticsNode.put(_PACKETS_OUT, serviceStatistics.getPacketOut().getValue().intValue());
        }

        return serviceStatisticsNode;
    }

    private ObjectNode getSfServicePathObjectNode (SfServicePath sfServicePath) {
        if (sfServicePath == null) {
            return null;
        }

        ObjectNode sfServicePathNode = mapper.createObjectNode();

        if (sfServicePath.getName() != null) {
            sfServicePathNode.put(_NAME, sfServicePath.getName());
        }

        if (sfServicePath.getServiceStatistics()!=null) {
            ObjectNode serviceStatisticsNode = mapper.createObjectNode();
            ServiceStatistics serviceStatistics = sfServicePath.getServiceStatistics();

            if (serviceStatistics.getBytesIn() != null) {
                serviceStatisticsNode.put(_BYTES_IN, serviceStatistics.getBytesIn().getValue().intValue());
            }

            if (serviceStatistics.getBytesOut() != null) {
                serviceStatisticsNode.put(_BYTES_OUT, serviceStatistics.getBytesOut().getValue().intValue());
            }

            if (serviceStatistics.getPacketOut() != null) {
                serviceStatisticsNode.put(_PACKETS_IN, serviceStatistics.getPacketsIn().getValue().intValue());
            }

            if (serviceStatistics.getPacketsIn() != null) {
                serviceStatisticsNode.put(_PACKETS_OUT, serviceStatistics.getPacketOut().getValue().intValue());
            }
            sfServicePathNode.put(_SERVICE_STATISTICS, serviceStatisticsNode);
        }

        return sfServicePathNode;
    }
}
