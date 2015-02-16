/*
* Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.sfc.provider.api;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.SfcNetconfDataProvider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.net.URI;

import com.google.common.base.Preconditions;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;
public class SfcProviderSfDescriptionMonitorAPI{
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfDescriptionMonitorAPI.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static ProviderSession sessionData;
    private static URI NETCONF_URI = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0");
    private static QName NETCONF_QNAME = QName.create(NETCONF_URI, null, "netconf");
    private static QName NETCONF_TYPE_QNAME = QName.create(NETCONF_QNAME, "type");
    private static QName NETCONF_FILTER_QNAME = QName.create(NETCONF_QNAME, "filter");
    private static QName NETCONF_DATA_QNAME = QName.create(NETCONF_QNAME, "data");
    private static final QName SF_DESCRIPTION_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt?revision=2014-11-05)get-SF-description");
    private static final QName SF_MONITOR_INFO_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt?revision=2014-11-05)get-SF-monitoring-info");
    private static final QName SF_DESCRIPTION_CHILD_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)SF-description");
    private static final QName SF_MONITOR_CHILD_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)SF-monitoring-info");
    private static final QName PORT_BANDWIDTH_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)port-bandwidth");
    private static final QName IPADDRESS_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)ipaddress");
    private static final QName MACADDERSS_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)macaddress");
    private static final QName NUM_OF_PORTS_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)number-of-dataports");
    private static final QName SUPPORTED_PACKET_RATE_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)supported-packet-rate");
    private static final QName SUPPORTED_BANDWIDTH_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)supported-bandwidth");
    private static final QName SUPPORTED_ACL_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)supported-ACL-number");
    private static final QName FIB_SIZE_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)FIB-size");
    private static final QName RIB_SIZE_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)RIB-size");
    private static final QName PORT_ID_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)port-id");
    private static final QName PORT_SUPPORTED_BANDWIDTH_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)supported-bandwidth");
    private static final QName LIVENESS_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)liveness");
    private static final QName PORT_BANDWITH_UTILIZATION_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)port-bandwidth-utilization");
    private static final QName PACKET_RATE_UTILIZATION_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)packet-rate-utilization");
    private static final QName BANDWITH_UTILIZATION_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)bandwidth-utilization");
    private static final QName CPU_UTILIZATION_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)CPU-utilization");
    private static final QName MEMORY_UTILIZATION_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)memory-utilization");
    private static final QName AVAILABLE_MEMORY_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)available-memory");
    private static final QName RIB_UTILIZATION_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)RIB-utilization");
    private static final QName FIB_UTILIZATION_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)FIB-utilization");
    private static final QName POWER_UTILIZATION_QNAME = QName.create("(urn.intel.params:xml:ns:sf-desc-mon-rpt)power-utilization");
    private Map<QName, String> sfDescMonInfoMap = new HashMap<QName, String>();

    public SfcProviderSfDescriptionMonitorAPI() {
        sfDescMonInfoMap.put(PORT_BANDWIDTH_QNAME,"port-bandwidth");
        sfDescMonInfoMap.put(NUM_OF_PORTS_QNAME,"number-of-dataports");
        sfDescMonInfoMap.put(SUPPORTED_PACKET_RATE_QNAME,"supported-packet-rate");
        sfDescMonInfoMap.put(SUPPORTED_BANDWIDTH_QNAME,"supported-bandwidth");
        sfDescMonInfoMap.put(SUPPORTED_ACL_QNAME,"supported-ACL-number");
        sfDescMonInfoMap.put(FIB_SIZE_QNAME,"FIB-size");
        sfDescMonInfoMap.put(RIB_SIZE_QNAME,"RIB-size");
        sfDescMonInfoMap.put(PORT_ID_QNAME,"port-id");
        sfDescMonInfoMap.put(PORT_SUPPORTED_BANDWIDTH_QNAME,"supported-bandwidth");
        sfDescMonInfoMap.put(PORT_BANDWITH_UTILIZATION_QNAME,"port-bandwidth-utilization");
        sfDescMonInfoMap.put(PACKET_RATE_UTILIZATION_QNAME,"packet-rate-utilization");
        sfDescMonInfoMap.put(BANDWITH_UTILIZATION_QNAME,"bandwidth-utilization");
        sfDescMonInfoMap.put(CPU_UTILIZATION_QNAME,"CPU-utilization");
        sfDescMonInfoMap.put(MEMORY_UTILIZATION_QNAME,"memory-utilization");
        sfDescMonInfoMap.put(AVAILABLE_MEMORY_QNAME,"available-memory");
        sfDescMonInfoMap.put(RIB_UTILIZATION_QNAME,"RIB-utilization");
        sfDescMonInfoMap.put(FIB_UTILIZATION_QNAME,"FIB-utilization");
        sfDescMonInfoMap.put(POWER_UTILIZATION_QNAME,"power-utilization");

        setSession();
    }

    protected void setSession()  {
        printTraceStart(LOG);
        try {
            if(sessionData==null) {
                sessionData = odlSfc.getBroker().registerProvider(SfcNetconfDataProvider.GetNetconfDataProvider());
                Preconditions.checkState(sessionData != null,"SfcNetconfDataProvider register is not available.");
            }
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
    }


    protected CompositeNodeTOImpl wrap(QName name, Node<?> node) {
        if (node != null) {
            return new CompositeNodeTOImpl(name, null, Collections.<Node<?>> singletonList(node));
        } else {
            return new CompositeNodeTOImpl(name, null, Collections.<Node<?>> emptyList());
        }
    }

    protected String getValueFromMap(QName qname) {
        String value;
        for(Map.Entry<QName,String> entry: sfDescMonInfoMap.entrySet()) {
            if ((entry.getKey()).equals(qname)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Map<String, Object> getSFDescriptionInfoFromNetconf(String mountpoint)  {
        printTraceStart(LOG);

        Optional<DOMMountPoint> mountPoint;
        Map<String, Object> sfDescInfo  = new HashMap<String, Object>();
        final QName nodes = QName.create("urn:opendaylight:inventory","2013-08-19","nodes");
        final QName node = QName.create(nodes,"node");
        final QName idName = QName.create(nodes,"id");
        try {
            // data path
            final YangInstanceIdentifier path = YangInstanceIdentifier.builder().
                    node(nodes).nodeWithKey(node,idName,mountpoint).build();

            DOMMountPointService mountService = SfcNetconfDataProvider.GetNetconfDataProvider().getMountService();
            if (mountService != null)  {
                mountPoint = mountService.getMountPoint(path);
                if (mountPoint.isPresent()) {
                    final Optional<RpcProvisionRegistry> service = mountPoint.get().getService(RpcProvisionRegistry.class);
                    Preconditions.checkState(service.isPresent());
                    RpcResult<CompositeNode> future = service.get().invokeRpc(SF_DESCRIPTION_QNAME, wrap(SF_DESCRIPTION_QNAME, null)).get();
                    CompositeNode data = future.getResult().getFirstCompositeByName(NETCONF_DATA_QNAME);
                    sfDescInfo = parseSFDescriptionInfo(data);
                } else {
                    LOG.error("In getSFDescriptionInfoFromNetconf(), DOMMountPoint is null");
                    return null;
                }
            }
            else {
                LOG.error("In getSFDescriptionInfoFromNetconf(), DOMMountPointService is null");
                return null;
            }
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return sfDescInfo;
    }

    public Map<String, Object> getSFMonitorInfoFromNetconf(String mountpoint)  {
        printTraceStart(LOG);

        Optional<DOMMountPoint> mountPoint;
        Map<String, Object> sfMonInfoMap  = new HashMap<String, Object>();
        final QName nodes = QName.create("urn:opendaylight:inventory","2013-08-19","nodes");
        final QName node = QName.create(nodes,"node");
        final QName idName = QName.create(nodes,"id");
        try {
            // data path
            final YangInstanceIdentifier path = YangInstanceIdentifier.builder().
                    node(nodes).nodeWithKey(node,idName, mountpoint).build();

            DOMMountPointService mountService = SfcNetconfDataProvider.GetNetconfDataProvider().getMountService();
            if (mountService != null)  {
                mountPoint = mountService.getMountPoint(path);
                if (mountPoint.isPresent()) {
                    final Optional<RpcProvisionRegistry> service = mountPoint.get().getService(RpcProvisionRegistry.class);
                    Preconditions.checkState(service.isPresent());
                    RpcResult<CompositeNode> future = service.get().invokeRpc(SF_MONITOR_INFO_QNAME, wrap(SF_MONITOR_INFO_QNAME, null)).get();
                    CompositeNode data = future.getResult().getFirstCompositeByName(NETCONF_DATA_QNAME);
                    sfMonInfoMap = parseSFMonitorInfo(data);
                } else {
                    LOG.error("In getSFMonitorInfoFromNetconf(), DOMMountPoint is null");
                    return null;
                }
            }
            else {
                LOG.error("In getSFMonitorInfoFromNetconf(), DOMMountPointService is null");
                return null;
            }
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return sfMonInfoMap;
    }

    protected Map<String, Object> parseSFDescriptionInfo(CompositeNode data)  {
        printTraceStart(LOG);

        Map<String, Object> sfDescInfoMap = new HashMap<String, Object>();
        Map<String, Object> capMap = new HashMap<String, Object>();
        List<Map<String, Object>> portsMap = new ArrayList<Map<String, Object>>();
        List<Node<?>> immutNode = data.getChildren();
        List<Node<?>> portsTmp = new ArrayList<Node<?>>();
        List<Node<?>> sfDescInfoChildren = new ArrayList<Node<?>>();
        List<Node<?>> sfCap = new ArrayList<Node<?>>();

        try {
            for (Node<?> node: immutNode)  {
                if (node instanceof ImmutableCompositeNode)  {
                    ImmutableCompositeNode sfDescInfo = (ImmutableCompositeNode)node;

                    if (SF_DESCRIPTION_CHILD_QNAME.equals(sfDescInfo.getNodeType())) {
                        sfDescInfoChildren = sfDescInfo.getChildren();
                    }
                }
            }
            //basic infomation
            for (Node<?> nodesB: sfDescInfoChildren)  {
                if (nodesB instanceof SimpleNodeTOImpl)  {
                    SimpleNodeTOImpl basicInfo = (SimpleNodeTOImpl)nodesB;
                    QName basicInfoType = basicInfo.getNodeType();
                    String basicInfoTypeString = getValueFromMap(basicInfoType);
                    if (basicInfoTypeString!=null)  {
                        sfDescInfoMap.put(basicInfoTypeString, Long.parseLong((String)basicInfo.getValue()));
                    } else {
                        LOG.warn("SF description info unknown data {}",basicInfoType.getLocalName());
                    }
                } else if (nodesB instanceof ImmutableCompositeNode)  {
                    ImmutableCompositeNode cap = (ImmutableCompositeNode)nodesB;
                    sfCap = cap.getChildren();
                }
            }
            //capabilities infomation
            for (Node<?> n: sfCap)  {
                if (n instanceof SimpleNodeTOImpl) {
                    SimpleNodeTOImpl capSimpleNode = (SimpleNodeTOImpl)n;
                    QName capSimpleNodeType = capSimpleNode.getNodeType();
                    String capSimpleNodeTypeString = getValueFromMap(capSimpleNodeType);
                    if (capSimpleNodeTypeString!=null)  {
                        capMap.put(capSimpleNodeTypeString, Long.parseLong((String)capSimpleNode.getValue()));
                    } else {
                        LOG.warn("SF description info unknown data {}",capSimpleNodeType.getLocalName());
                    }
                } else if (n instanceof ImmutableCompositeNode)  {
                    ImmutableCompositeNode capPorts = (ImmutableCompositeNode)n;
                    portsTmp = capPorts.getChildren();
                }
            }

            //ports capabilities information
            for (Node<?> ps: portsTmp)  {
                if (ps instanceof ImmutableCompositeNode)  {
                    ImmutableCompositeNode ports = (ImmutableCompositeNode)ps;
                    if (PORT_BANDWIDTH_QNAME.equals(ports.getNodeType()))  {
                        List<Node<?>> portTmp = ports.getChildren();
                        Map<String, Object> portMap = new HashMap<String, Object>();
                        for (Node<?> p: portTmp)  {
                            if (p instanceof SimpleNodeTOImpl)  {
                                SimpleNodeTOImpl port = (SimpleNodeTOImpl)p;
                                QName portType = port.getNodeType();
                                String portTypeString = getValueFromMap(portType);
                                if (portTypeString!=null)  {
                                    portMap.put(portTypeString, Long.parseLong((String)port.getValue()));
                                } else if (IPADDRESS_QNAME.equals(portType))  {
                                    final Ipv4Address ipv4Addr = new Ipv4Address((String)port.getValue());
                                    portMap.put(portType.getLocalName(), ipv4Addr);
                                } else if (MACADDERSS_QNAME.equals(portType))  {
                                    MacAddress macAddr = new MacAddress((String)port.getValue());
                                    portMap.put(portType.getLocalName(), macAddr);
                                } else {
                                    LOG.warn("SF description info unknown data {}",portType.getLocalName());
                                }
                            }
                        }
                        portsMap.add(portMap);
                    }
                }
            }
            capMap.put("ports",portsMap);
            sfDescInfoMap.put("capabilities",capMap);
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return sfDescInfoMap;
    }

    protected Map<String, Object> parseSFMonitorInfo(CompositeNode data)  {
        printTraceStart(LOG);
        Map<String, Object> sfMonInfoMap = new HashMap<String, Object>();
        Map<String, Object> utilMap = new HashMap<String, Object>();
        List<Map<String, Object>> portsMap = new ArrayList<Map<String, Object>>();
        List<Node<?>> immutNode = data.getChildren();
        List<Node<?>> sfMonInfoChildren = new ArrayList<Node<?>>();
        List<Node<?>> utilChildren = new ArrayList<Node<?>>();
        List<Node<?>> portsTmp = new ArrayList<Node<?>>();

        try {
            for (Node<?> node: immutNode)  {
                if (node instanceof ImmutableCompositeNode)  {
                    ImmutableCompositeNode sfMonInfo = (ImmutableCompositeNode)node;

                    if (SF_MONITOR_CHILD_QNAME.equals(sfMonInfo.getNodeType())) {
                        sfMonInfoChildren = sfMonInfo.getChildren();
                    }
                }
            }
            //liveness
            for (Node<?> nodesB: sfMonInfoChildren)  {
                if (nodesB instanceof SimpleNodeTOImpl)  {
                    SimpleNodeTOImpl nodeSimple = (SimpleNodeTOImpl)nodesB;
                    QName nodeSimpleType = nodeSimple.getNodeType();

                    if (LIVENESS_QNAME.equals(nodeSimpleType))  {
                        boolean liveness;
                        if (((String)nodeSimple.getValue()).equals("false")) {
                            liveness = false;
                            sfMonInfoMap.put(nodeSimpleType.getLocalName(), liveness);
                        } else if (((String)nodeSimple.getValue()).equals("true")) {
                            liveness = true;
                            sfMonInfoMap.put(nodeSimpleType.getLocalName(), liveness);
                        } else {
                            LOG.warn("SF Monitor Info unknown data {}",nodeSimpleType.getLocalName());
                        }
                    }
                } else if (nodesB instanceof ImmutableCompositeNode)  {
                    ImmutableCompositeNode util = (ImmutableCompositeNode)nodesB;
                    utilChildren = util.getChildren();
                }
            }
            //utilization information
            for (Node<?> utilChild: utilChildren)  {
                if (utilChild instanceof SimpleNodeTOImpl) {
                    SimpleNodeTOImpl utilSimpleNode = (SimpleNodeTOImpl)utilChild;
                    QName utilSimpleNodetype = utilSimpleNode.getNodeType();
                    String utilSimpleNodetypeString = getValueFromMap(utilSimpleNodetype);
                    if (utilSimpleNodetypeString!=null)  {
                        utilMap.put(utilSimpleNodetypeString, Long.parseLong((String)utilSimpleNode.getValue()));
                    } else {
                        LOG.warn("SF monitor info unknown data {}",utilSimpleNodetype.getLocalName());
                    }
                } else if (utilChild instanceof ImmutableCompositeNode)  {
                    ImmutableCompositeNode utilPorts = (ImmutableCompositeNode)utilChild;
                    portsTmp = utilPorts.getChildren();
                }
            }
            //ports utilization information
            for (Node<?> ps: portsTmp)  {
                if (ps instanceof ImmutableCompositeNode)  {
                    ImmutableCompositeNode ports = (ImmutableCompositeNode)ps;
                    if (PORT_BANDWITH_UTILIZATION_QNAME.equals(ports.getNodeType()))  {
                        List<Node<?>> portTmp = ports.getChildren();
                        Map<String, Object> portMap = new HashMap<String, Object>();
                        for (Node<?> p: portTmp)  {
                            if (p instanceof SimpleNodeTOImpl)  {
                                SimpleNodeTOImpl port = (SimpleNodeTOImpl)p;
                                QName portType = port.getNodeType();
                                String portTypeString = getValueFromMap(portType);
                                if (portTypeString!=null)  {
                                    portMap.put(portTypeString, Long.parseLong((String)port.getValue()));
                                } else {
                                    LOG.warn("SF monitor info unknown data {}",portType.getLocalName());
                                }
                            }
                        }
                        portsMap.add(portMap);
                    }
                }
            }
            utilMap.put("ports",portsMap);
            sfMonInfoMap.put("utilization",utilMap);
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
        return sfMonInfoMap;
    }
}
