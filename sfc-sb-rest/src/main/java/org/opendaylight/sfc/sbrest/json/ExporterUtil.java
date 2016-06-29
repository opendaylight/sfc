/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Function;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Lisp;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;


public class ExporterUtil {

    public static final String _TRANSPORT = "transport";
    public static final String _EID = "eid";
    public static final String _IP = "ip";
    public static final String _PORT = "port";
    public static final String _MAC = "mac";
    public static final String _VLAN_ID = "vlan-id";
    public static final String _FUNCTION_NAME = "function-name";
    public static final String _VXLAN_GPE = "vxlan-gpe";
    public static final String _GRE = "gre";
    public static final String _OTHER = "other";
    public static final String _SF_DPL_NAME = "sf-dpl-name";
    public static final String _SFF_DPL_NAME = "sff-dpl-name";

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

        if(sffSfDpl.getSfDplName() != null) {
            sffSfDplNode.put(_SF_DPL_NAME, sffSfDpl.getSfDplName().getValue());
        }

        if(sffSfDpl.getSffDplName() != null) {
            sffSfDplNode.put(_SFF_DPL_NAME, sffSfDpl.getSffDplName().getValue());
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
            String type = dataPlaneLocator.getLocatorType().getImplementedInterface().getSimpleName().toLowerCase();
            switch (type) {
                case FUNCTION:
                    Function functionLocator = (Function) dataPlaneLocator.getLocatorType();
                    locatorNode.put(_FUNCTION_NAME, functionLocator.getFunctionName());
                    break;
                case IP:
                    Ip ipLocator = (Ip) dataPlaneLocator.getLocatorType();
                    if (ipLocator.getIp() != null) {
                        locatorNode.put(_IP, convertIpAddress(ipLocator.getIp()));
                        if (ipLocator.getPort() != null) {
                            locatorNode.put(_PORT, ipLocator.getPort().getValue());
                        }
                    }
                    break;
                case LISP:
                    Lisp lispLocator = (Lisp) dataPlaneLocator.getLocatorType();
                    if (lispLocator.getEid() != null)
                        locatorNode.put(_EID, convertIpAddress(lispLocator.getEid()));
                    break;
                case MAC:
                    Mac macLocator = (Mac) dataPlaneLocator.getLocatorType();
                    if (macLocator.getMac() != null)
                        locatorNode.put(_MAC, macLocator.getMac().getValue());
                    locatorNode.put(_VLAN_ID, macLocator.getVlanId());
            }
        }

        if (dataPlaneLocator.getTransport() != null) {
            if (locatorNode == null) {
                locatorNode = mapper.createObjectNode();
            }
            locatorNode.put(_TRANSPORT, getDataPlaneLocatorTransport(dataPlaneLocator));
        }

        return locatorNode;
    }

    protected static String getDataPlaneLocatorTransport(DataPlaneLocator dataPlaneLocator) {
        if (dataPlaneLocator == null || dataPlaneLocator.getTransport() == null) {
            return null;
        }

        String transport = null;
        switch (dataPlaneLocator.getTransport().getSimpleName().toLowerCase()) {
            case VXLAN_GPE:
                transport = SERVICE_LOCATOR_PREFIX + _VXLAN_GPE;
                break;
            case GRE:
                transport = SERVICE_LOCATOR_PREFIX + _GRE;
                break;
            case OTHER:
                transport = SERVICE_LOCATOR_PREFIX + _OTHER;
                break;
            default:
                transport = SERVICE_LOCATOR_PREFIX + _OTHER;
        }

        return transport;
    }
}
