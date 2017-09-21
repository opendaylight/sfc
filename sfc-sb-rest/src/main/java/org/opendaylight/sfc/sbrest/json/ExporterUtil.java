/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Function;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Lisp;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

public class ExporterUtil {

    public static final String TRANSPORT = "transport";
    public static final String EID = "eid";
    public static final String PORT = "port";
    public static final String VLAN_ID = "vlan-id";
    public static final String FUNCTION_NAME = "function-name";
    public static final String VXLAN_GPE_DPL = "vxlan-gpe";
    public static final String SF_DPL_NAME = "sf-dpl-name";
    public static final String SFF_DPL_NAME = "sff-dpl-name";

    public static final String FUNCTION = "function";
    public static final String IP = "ip";
    public static final String LISP = "lisp";
    public static final String MAC = "mac";

    public static final String VXLAN_GPE = "vxlangpe";
    public static final String GRE = "gre";
    public static final String OTHER = "other";
    public static final String SERVICE_LOCATOR_PREFIX = "service-locator:";

    public static String convertIpAddress(IpAddress ip) {
        String ret = null;
        if (ip != null) {
            if (ip.getIpv4Address() != null) {
                ret = ip.getIpv4Address().getValue();
            } else if (ip.getIpv6Address() != null) {
                ret = ip.getIpv6Address().getValue();
            }
        }
        return ret;
    }

    protected static ObjectNode getSffSfDataPlaneLocatorObjectNode(SffSfDataPlaneLocator sffSfDpl) {
        if (sffSfDpl == null) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode sffSfDplNode = mapper.createObjectNode();

        if (sffSfDpl.getSfDplName() != null) {
            sffSfDplNode.put(SF_DPL_NAME, sffSfDpl.getSfDplName().getValue());
        }

        if (sffSfDpl.getSffDplName() != null) {
            sffSfDplNode.put(SFF_DPL_NAME, sffSfDpl.getSffDplName().getValue());
        }

        return sffSfDplNode;
    }

    protected static ObjectNode getDataPlaneLocatorObjectNode(DataPlaneLocator dataPlaneLocator) {
        if (dataPlaneLocator == null) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode locatorNode = null;

        if (dataPlaneLocator.getLocatorType() != null) {
            locatorNode = mapper.createObjectNode();
            String type = dataPlaneLocator.getLocatorType().getImplementedInterface().getSimpleName()
                    .toLowerCase(Locale.getDefault());
            switch (type) {
                case FUNCTION:
                    Function functionLocator = (Function) dataPlaneLocator.getLocatorType();
                    locatorNode.put(FUNCTION_NAME, functionLocator.getFunctionName());
                    break;
                case IP:
                    Ip ipLocator = (Ip) dataPlaneLocator.getLocatorType();
                    if (ipLocator.getIp() != null) {
                        locatorNode.put(IP, convertIpAddress(ipLocator.getIp()));
                        if (ipLocator.getPort() != null) {
                            locatorNode.put(PORT, ipLocator.getPort().getValue());
                        }
                    }
                    break;
                case LISP:
                    Lisp lispLocator = (Lisp) dataPlaneLocator.getLocatorType();
                    if (lispLocator.getEid() != null) {
                        locatorNode.put(EID, convertIpAddress(lispLocator.getEid()));
                    }
                    break;
                case MAC:
                    Mac macLocator = (Mac) dataPlaneLocator.getLocatorType();
                    if (macLocator.getMac() != null) {
                        locatorNode.put(MAC, macLocator.getMac().getValue());
                    }
                    locatorNode.put(VLAN_ID, macLocator.getVlanId());
                    break;
                default:
                    break;
            }
        }

        if (dataPlaneLocator.getTransport() != null) {
            if (locatorNode == null) {
                locatorNode = mapper.createObjectNode();
            }
            locatorNode.put(TRANSPORT, getDataPlaneLocatorTransport(dataPlaneLocator));
        }

        return locatorNode;
    }

    protected static String getDataPlaneLocatorTransport(DataPlaneLocator dataPlaneLocator) {
        if (dataPlaneLocator == null || dataPlaneLocator.getTransport() == null) {
            return null;
        }

        String transport = null;
        switch (dataPlaneLocator.getTransport().getSimpleName().toLowerCase(Locale.getDefault())) {
            case VXLAN_GPE:
                transport = SERVICE_LOCATOR_PREFIX + VXLAN_GPE_DPL;
                break;
            case GRE:
                transport = SERVICE_LOCATOR_PREFIX + GRE;
                break;
            case OTHER:
                transport = SERVICE_LOCATOR_PREFIX + OTHER;
                break;
            default:
                transport = SERVICE_LOCATOR_PREFIX + OTHER;
                break;
        }

        return transport;
    }
}
