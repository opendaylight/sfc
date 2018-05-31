/*
 * Copyright (c) 2014, 2017 by Ericsson and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.renderers.openflow.utils;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.port.details.OfsPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This Abstract Base class allows us to completely isolate the
 * SfcOfRspProcessor and SfcOfSfgDataListener from the sfc-provider-api
 * and the controller data store, which makes it much easier to Unit
 * Test both classes.
 *
 * @author Brady Johnson
 */
public abstract class SfcOfBaseProviderUtils {

    protected static final Logger LOG = LoggerFactory.getLogger(SfcOfBaseProviderUtils.class);

    public abstract void addRsp(long rspId);

    public abstract void removeRsp(long rspId);

    public abstract ServiceFunction getServiceFunction(SfName sfName, long rspId);

    public abstract ServiceFunctionType getServiceFunctionType(SfName sfName, long rspId);

    public abstract ServiceFunctionForwarder getServiceFunctionForwarder(SffName sffName, long rspId);

    public abstract ServiceFunctionGroup getServiceFunctionGroup(String sfgName, long rspId);

    public abstract Long getPortNumberFromName(String bridgeName, String portName, long rspId);

    // Get the SFF DPLs that are not used by SFs. Useful when there are multiple
    // DPL types: one for the SFs and another for the SFF trunk.
    public abstract List<SffDataPlaneLocator> getSffNonSfDataPlaneLocators(ServiceFunctionForwarder sff);

    public abstract void setTableOffsets(SffName sffName, long tableBase);

    /**
     * Return a named SffDataPlaneLocator on a SFF.
     *
     * @param sff
     *            - The SFF to search in
     * @param dplName
     *            - The name of the DPL to look for
     * @return SffDataPlaneLocator or null if not found
     */
    public SffDataPlaneLocator getSffDataPlaneLocator(ServiceFunctionForwarder sff, SffDataPlaneLocatorName dplName) {
        SffDataPlaneLocator sffDpl = null;

        if (sff == null || sff.getSffDataPlaneLocator() == null || dplName == null || dplName.getValue() == null) {
            return null;
        }

        List<SffDataPlaneLocator> sffDataPlanelocatorList = sff.getSffDataPlaneLocator();
        for (SffDataPlaneLocator sffDataPlanelocator : sffDataPlanelocatorList) {
            if (sffDataPlanelocator.getName() != null
                    && sffDataPlanelocator.getName().getValue().equals(dplName.getValue())) {
                sffDpl = sffDataPlanelocator;
                break;
            }
        }

        return sffDpl;
    }

    /**
     * Return a named SfDataPlaneLocator on a SF.
     *
     * @param sf
     *            - The SF to search in
     * @param dplName
     *            - The name of the DPL to look for
     * @return SfDataPlaneLocator or null if not found
     */
    public SfDataPlaneLocator getSfDataPlaneLocator(ServiceFunction sf, SfDataPlaneLocatorName dplName) {
        SfDataPlaneLocator sfDpl = null;

        if (sf == null || sf.getSfDataPlaneLocator() == null || dplName == null || dplName.getValue() == null) {
            return null;
        }

        List<SfDataPlaneLocator> sfDataPlanelocatorList = sf.getSfDataPlaneLocator();
        for (SfDataPlaneLocator sfDataPlanelocator : sfDataPlanelocatorList) {
            if (sfDataPlanelocator.getName() != null
                    && sfDataPlanelocator.getName().getValue().equals(dplName.getValue())) {
                sfDpl = sfDataPlanelocator;
                break;
            }
        }

        return sfDpl;
    }

    /**
     * Return any of the SfDataPlaneLocator on the SF that connects to the named
     * SFF.
     *
     * @param sf
     *            the ServiceFunction to search through
     * @param sffName
     *            the SFF name to search for
     * @return SfDataPlaneLocator or null if not found
     */
    public SfDataPlaneLocator getSfDataPlaneLocator(ServiceFunction sf, final SffName sffName) {
        List<SfDataPlaneLocator> sfDataPlanelocatorList = sf.getSfDataPlaneLocator();
        for (SfDataPlaneLocator sfDpl : sfDataPlanelocatorList) {
            if (sfDpl.getServiceFunctionForwarder().equals(sffName)) {
                return sfDpl;
            }
        }

        return null;
    }

    /**
     * Returns the SfDataPlaneLocator for SFF Egress in relation to a path
     * direction. On a forward path, this is the forward SF DPL, while on a
     * reverse path, this is the reverse SF DPL.
     *
     * @param sff
     *            - The SFF
     * @param sf
     *            - The SF
     * @param isForwardPath
     *            - True if the path is a forward path, false otherwise
     * @return SfDataPlaneLocator or null if not found
     */
    public SfDataPlaneLocator getEgressSfDataPlaneLocator(ServiceFunctionForwarder sff,
                                                          ServiceFunction sf,
                                                          boolean isForwardPath) {
        SffSfDataPlaneLocator sffSfDpl = getSffSfDataPlaneLocator(sff, sf);

        if (sffSfDpl == null) {
            // revert to look by SFF name when using old provisioning model
            // with no SFF dictionary
            return sff != null ? getSfDataPlaneLocator(sf, sff.getName()) : null;
        }

        final SfDataPlaneLocatorName sfForwardDplName = sffSfDpl.getSfForwardDplName();
        final SfDataPlaneLocatorName sfReverseDplName = sffSfDpl.getSfReverseDplName();
        SfDataPlaneLocatorName sfDplName = isForwardPath ? sfForwardDplName : sfReverseDplName;
        if (sfDplName == null) {
            // fallback to non directional DPL
            sfDplName = sffSfDpl.getSfDplName();
        }

        return getSfDataPlaneLocator(sf, sfDplName);
    }

    /**
     * Returns the SffDataPlaneLocator for SFF Egress in relation to a path
     * direction. On a forward path, this is the forward SFF DPL, while on a
     * reverse path, this is the reverse SFF DPL.
     *
     * @param sff
     *            - The SFF
     * @param sf
     *            - The SF
     * @param isForwardPath
     *            - True if the path is a forward path, false otherwise
     * @return SffDataPlaneLocator or null if not found
     */
    public SffDataPlaneLocator getEgressSffDataPlaneLocator(ServiceFunctionForwarder sff,
                                                            ServiceFunction sf,
                                                            boolean isForwardPath) {
        SffSfDataPlaneLocator sffSfDpl = getSffSfDataPlaneLocator(sff, sf);

        if (sffSfDpl == null) {
            return null;
        }

        final SffDataPlaneLocatorName sffForwardDplName = sffSfDpl.getSffForwardDplName();
        final SffDataPlaneLocatorName sffReverseDplName = sffSfDpl.getSffReverseDplName();
        SffDataPlaneLocatorName sffDplName = isForwardPath ? sffForwardDplName : sffReverseDplName;
        if (sffDplName == null) {
            // fallback to non directional DPL
            sffDplName = sffSfDpl.getSffDplName();
        }

        return getSffDataPlaneLocator(sff, sffDplName);
    }

    /**
     * Returns the SfDataPlaneLocator for SFF Ingress in relation to a path
     * direction. On a forward path, this is the reverse SF DPL, while on a
     * reverse path, this is the forward SF DPL.
     *
     * @param sff
     *            - The SFF
     * @param sf
     *            - The SF
     * @param isForwardPath
     *            - True if the path is a forward path, false otherwise
     * @return SfDataPlaneLocator or null if not found
     */
    public SfDataPlaneLocator getIngressSfDataPlaneLocator(ServiceFunctionForwarder sff,
                                                           ServiceFunction sf,
                                                           boolean isForwardPath) {

        SffSfDataPlaneLocator sffSfDpl = getSffSfDataPlaneLocator(sff, sf);

        if (sffSfDpl == null) {
            // revert to look by SFF name when using old provisioning model
            // with no SFF dictionary
            return sff != null ? getSfDataPlaneLocator(sf, sff.getName()) : null;
        }

        final SfDataPlaneLocatorName sfForwardDplName = sffSfDpl.getSfForwardDplName();
        final SfDataPlaneLocatorName sfReverseDplName = sffSfDpl.getSfReverseDplName();
        SfDataPlaneLocatorName sfDplName = isForwardPath ? sfReverseDplName : sfForwardDplName;
        if (sfDplName == null) {
            // fallback to non directional DPL
            sfDplName = sffSfDpl.getSfDplName();
        }

        return getSfDataPlaneLocator(sf, sfDplName);
    }

    /**
     * Return the named SF ServiceFunctionDictionary SffSfDataPlaneLocator from
     * the sff sf-dictionary list.
     *
     * @param sff
     *            - The SFF to search in
     * @param sf
     *            - The SF to look for
     * @return SffSfDataPlaneLocator or null if not found
     */
    public SffSfDataPlaneLocator getSffSfDataPlaneLocator(ServiceFunctionForwarder sff, ServiceFunction sf) {
        if (sf == null) {
            return null;
        }
        return getSffSfDataPlaneLocator(sff, sf.getName());
    }

    /**
     * Return the named SF ServiceFunctionDictionary SffSfDataPlaneLocator from
     * the sff sf-dictionary list.
     *
     * @param sff
     *            - The SFF to search in
     * @param sfName
     *            - The name of the SF to look for
     * @return SffSfDataPlaneLocator or null if not found
     */
    public SffSfDataPlaneLocator getSffSfDataPlaneLocator(ServiceFunctionForwarder sff, SfName sfName) {
        SffSfDataPlaneLocator sffSfDpl = null;

        ServiceFunctionDictionary sffSfDict = getSffSfDictionary(sff, sfName);
        if (sffSfDict != null) {
            sffSfDpl = sffSfDict.getSffSfDataPlaneLocator();
        }

        return sffSfDpl;
    }

    /**
     * Return the named SF ServiceFunctionDictionary element from the sff
     * sf-dictionary list.
     *
     * @param sff
     *            the SFF to search through
     * @param sfName
     *            the SF name to look for
     * @return A ServiceFunctionDictionary entry or null if not found
     */
    public ServiceFunctionDictionary getSffSfDictionary(ServiceFunctionForwarder sff, SfName sfName) {
        ServiceFunctionDictionary sffSfDict = null;

        if (sff == null || sfName == null) {
            return null;
        }

        List<ServiceFunctionDictionary> sffSfDictList = sff.getServiceFunctionDictionary();
        if (sffSfDictList != null) {
            for (ServiceFunctionDictionary dict : sffSfDictList) {
                if (dict.getName().getValue().equals(sfName.getValue())) {
                    sffSfDict = dict;
                    break;
                }
            }
        }
        return sffSfDict;
    }

    /**
     * Return the mac address from a SF DPL, only if its a MAC DPL.
     *
     * @param sfDpl
     *            the SF DPL to process
     * @return macAddress string or null if its not a MAC DPL
     */
    public String getSfDplMac(SfDataPlaneLocator sfDpl) {
        String sfMac = null;

        LocatorType sffLocatorType = sfDpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class) && ((MacAddressLocator) sffLocatorType).getMac() != null) {
            sfMac = ((MacAddressLocator) sffLocatorType).getMac().getValue();
        }

        return sfMac;
    }

    /**
     * Return the mac address from a SFF DPL, only if its a MAC DPL.
     *
     * @param sfDpl the SFF DPL to process
     * @return macAddress string or null if its not a MAC DPL
     */
    public String getSffDplMac(SffDataPlaneLocator sfDpl) {
        String sffMac = null;

        LocatorType sffLocatorType = sfDpl.getDataPlaneLocator().getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class) && ((MacAddressLocator) sffLocatorType).getMac() != null) {
            sffMac = ((MacAddressLocator) sffLocatorType).getMac().getValue();
        }

        return sffMac;
    }

    /**
     * Given an SFF object and SFF-SF dictionary entry, return the switch port
     * string. Looks for the SFF DPL name in the SFF-SF dictionary, then looks
     * up that DPL name on the SFF.
     *
     * @param sff
     *            the SFF to process
     * @param dict
     *            used to get the SFF DPL name
     * @return switch port string, INPORT if not augmented, INPORT if not found
     */
    public String getDictPortInfoPort(final ServiceFunctionForwarder sff, final ServiceFunctionDictionary dict) {
        SffDataPlaneLocator sffDpl = getSffDataPlaneLocator(sff, dict.getSffSfDataPlaneLocator().getSffDplName());
        OfsPort ofsPort = getSffPortInfoFromDpl(sffDpl);

        if (ofsPort == null) {
            // This case is most likely because the sff-of augmentation wasnt
            // used
            // assuming the packet should just be sent on the same port it was
            // received on
            return OutputPortValues.INPORT.toString();
        }

        return ofsPort.getPortId();
    }

    /**
     * Given a possibly augmented SFF DPL, return the augmented OfsPort object.
     *
     * @param sffDpl
     *            The SFF DPL to process
     * @return OfsPort, null if not augmented, null if not found
     */
    public OfsPort getSffPortInfoFromDpl(final SffDataPlaneLocator sffDpl) {
        if (sffDpl == null) {
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = sffDpl.augmentation(SffDataPlaneLocator1.class);
        if (ofsDpl == null) {
            LOG.debug("No OFS DPL available for dpl [{}]", sffDpl.getName().getValue());
            return null;
        }

        return ofsDpl.getOfsPort();
    }

    /**
     * Given a possibly augmented SFF DPL, return the DPL switch port. The
     * augmentation will be a OfsPort object.
     *
     * @param dpl
     *            the SFF DPL to process
     * @return switch port string, INPORT if not augmented, INPORT if not found
     */
    public String getDplPortInfoPort(final SffDataPlaneLocator dpl) {
        OfsPort ofsPort = getSffPortInfoFromDpl(dpl);

        if (ofsPort == null) {
            // This case is most likely because the sff-of augmentation wasnt
            // used
            // assuming the packet should just be sent on the same port it was
            // received on
            return OutputPortValues.INPORT.toString();
        }

        return ofsPort.getPortId();
    }

    /**
     * Given a possibly augmented SFF DPL, return the DPL mac address. The
     * augmentation will be a OfsPort object.
     *
     * @param dpl
     *            the SFF DPL to process
     * @return mac address string, null if not augmented, null if not found
     */
    public String getDplPortInfoMac(final SffDataPlaneLocator dpl) {
        if (dpl == null) {
            return null;
        }

        String macStr = null;
        OfsPort ofsPort = getSffPortInfoFromDpl(dpl);

        if (ofsPort != null && ofsPort.getMacAddress() != null) {
            macStr = ofsPort.getMacAddress().getValue();
        }

        if (macStr == null && dpl.getDataPlaneLocator().getTransport()
                .equals(org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac.class)) {
            MacAddress mac = ((MacAddressLocator) dpl.getDataPlaneLocator().getLocatorType()).getMac();
            if (mac != null) {
                macStr = mac.getValue();
            }
        }

        return macStr;
    }

    /**
     * Given an SFF object and SFF-SF dictionary entry, return the SFF Mac.
     * Looks for the SFF DPL name in the SFF-SF dictionary, then looks up that
     * DPL name on the SFF.
     *
     * @param sff
     *            The SFF to process
     * @param dict
     *            contains the SFF DPL name to process
     * @return MAC Address string, null if the DPL is not mac, null if not found
     */
    public String getDictPortInfoMac(final ServiceFunctionForwarder sff, final ServiceFunctionDictionary dict) {
        SffDataPlaneLocator sffDpl = getSffDataPlaneLocator(sff, dict.getSffSfDataPlaneLocator().getSffDplName());
        String macStr = getDplPortInfoMac(sffDpl);

        // If the SFF DPL wasnt augmented, check if the DPL is of type mac, and
        // return that mac address
        if (macStr == null && sffDpl.getDataPlaneLocator().getTransport()
                .equals(org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac.class)) {
            MacAddress mac = ((MacAddressLocator) sffDpl.getDataPlaneLocator().getLocatorType()).getMac();
            if (mac != null) {
                macStr = mac.getValue();
            }
        }

        return macStr;
    }

    /**
     * Given an SFF name, return the augmented OpenFlow NodeName.
     *
     * @param sffName
     *            The SFF name to process
     * @param rspId
     *            the rsp the SFF is being processed on
     * @return OpenFlow NodeName, null if not augmented, null if not found
     */
    public String getSffOpenFlowNodeName(final SffName sffName, long rspId) {
        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName, rspId);
        // TODO return more addressing type - NodeId
        return getSffOpenFlowNodeName(sff);
    }

    /**
     * Given an SFF name, return the augmented OpenFlow NodeName.
     *
     * @param sffName
     *            The SFF name to process
     * @param rspId
     *            the rsp the SFF is being processed on
     * @param dpnid
     *            data plane node identifier for the switch. It is null when it
     *            is not known; when provided, it is used directly to build the
     *            openflow node name
     * @return OpenFlow NodeName, null if not augmented, null if not found
     */
    public String getSffOpenFlowNodeName(final SffName sffName, long rspId, final DpnIdType dpnid) {
        if (dpnid != null) {
            // part of logical sff: openflow node name = "openflow:dpnid"
            return "openflow:" + dpnid.getValue();
        }
        return getSffOpenFlowNodeName(sffName, rspId);
    }

    /**
     * Given an SFF object, return the augmented OpenFlow NodeName.
     *
     * @param sff
     *            The SFF name to process
     * @return OpenFlow NodeName or null if augmented or not found
     */
    public String getSffOpenFlowNodeName(final ServiceFunctionForwarder sff) {
        if (sff == null) {
            return null;
        }

        // Check if its an service-function-forwarder-ovs augmentation
        // if it is, then get the open flow node id there
        SffOvsBridgeAugmentation ovsSff = sff.augmentation(SffOvsBridgeAugmentation.class);
        if (ovsSff != null && ovsSff.getOvsBridge() != null) {
            return ovsSff.getOvsBridge().getOpenflowNodeId();
        }

        // it its not an sff-ovs, then just return the ServiceNode
        if (sff.getServiceNode() != null) {
            return sff.getServiceNode().getValue();
        }

        return null;
    }

    public SffDataPlaneLocator getSffSfDictSffDpl(SfName sfName, SffName sffName, long rspId) {
        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName, rspId);

        for (ServiceFunctionDictionary sffDict : sff.getServiceFunctionDictionary()) {
            if (sffDict.getName().equals(sfName)) {
                return getSffDataPlaneLocator(sff, sffDict.getSffSfDataPlaneLocator().getSffDplName());
            }
        }

        return null;
    }

    /**
     * Return a named SffDataPlaneLocator on a SFF.
     *
     * @param sffName
     *          The SFF name to search in
     * @param rspId
     *          rendered service path ID
     * @return list of SffDataPlaneLocator or null if not found
     */
    public List<SffDataPlaneLocator> getSffDataPlaneLocators(SffName sffName, long rspId) {
        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName, rspId);

        if (sff == null || sff.getSffDataPlaneLocator() == null) {
            return null;
        }
        return sff.getSffDataPlaneLocator();
    }
}
