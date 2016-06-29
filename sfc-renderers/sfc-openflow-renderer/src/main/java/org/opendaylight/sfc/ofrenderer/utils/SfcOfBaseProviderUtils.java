/*
 * Copyright (c) 2014 by Ericsson and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.utils;

import java.util.List;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
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

    abstract public void addRsp(long rspId);

    abstract public void removeRsp(long rspId);

    abstract public ServiceFunction getServiceFunction(final SfName sfName, long rspId);

    abstract public ServiceFunctionForwarder getServiceFunctionForwarder(final SffName sffName, long rspId);

    abstract public ServiceFunctionGroup getServiceFunctionGroup(final String sfgName, long rspId);

    abstract public Long getPortNumberFromName(final String bridgeName, final String portName, long rspId);
    /**
     * Return a named SffDataPlaneLocator on a SFF
     *
     * @param sff - The SFF to search in
     * @param dplName - The name of the DPL to look for
     * @return SffDataPlaneLocator or null if not found
     */
    public SffDataPlaneLocator getSffDataPlaneLocator(ServiceFunctionForwarder sff, SffDataPlaneLocatorName dplName) {
        SffDataPlaneLocator sffDpl = null;

        if(dplName == null || dplName.getValue() == null) {
            return null;
        }

        List<SffDataPlaneLocator> sffDataPlanelocatorList = sff.getSffDataPlaneLocator();
        for (SffDataPlaneLocator sffDataPlanelocator : sffDataPlanelocatorList) {
            if (sffDataPlanelocator.getName() != null) {
                if (sffDataPlanelocator.getName().getValue().equals(dplName.getValue())) {
                    sffDpl = sffDataPlanelocator;
                    break;
                }
            }
        }

        return sffDpl;
    }

    /**
     * Return the SfDataPlaneLocator on the SF that connects to the named SFF
     *
     * @param sf the ServiceFunction to search through
     * @param sffName the SFF name to search for
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
     * Given a ServiceFunction get the SF DPL name from the SffSfDataPlaneLocator
     * and return the SF DPL
     *
     * @param sf the ServiceFunction to search through
     * @param sffSfDpl The SffSf DPL to compare against
     * @return SfDataPlaneLocator if found, else null
     */
    public SfDataPlaneLocator getSfDataPlaneLocator(ServiceFunction sf, SffSfDataPlaneLocator sffSfDpl) {
        List<SfDataPlaneLocator> sfDataPlanelocatorList = sf.getSfDataPlaneLocator();

        for (SfDataPlaneLocator sfDpl : sfDataPlanelocatorList) {
            if (sfDpl.getName().getValue().equals(sffSfDpl.getSfDplName().getValue())) {
                return sfDpl;
            }
        }

        return null;
    }

    /**
     * Return the named SF ServiceFunctionDictionary SffSfDataPlaneLocator
     * from the sff sf-dictionary list
     *
     * @param sff - The SFF to search in
     * @param sfName - The name of the DPL to look for
     * @return SffSfDataPlaneLocator or null if not found
     */
    public SffSfDataPlaneLocator getSffSfDataPlaneLocator(ServiceFunctionForwarder sff, SfName sfName) {
        SffSfDataPlaneLocator sffSfDpl = null;

        ServiceFunctionDictionary sffSfDict = getSffSfDictionary(sff, sfName);
        if(sffSfDict != null) {
            sffSfDpl = sffSfDict.getSffSfDataPlaneLocator();
        }

        return sffSfDpl;
    }

    /**
     * Return the named SF ServiceFunctionDictionary element from the
     * sff sf-dictionary list
     *
     * @param sff the SFF to search through
     * @param sfName the SF name to look for
     * @return A ServiceFunctionDictionary entry or null if not found
     */
    public ServiceFunctionDictionary getSffSfDictionary(ServiceFunctionForwarder sff, SfName sfName) {
        ServiceFunctionDictionary sffSfDict = null;

        List<ServiceFunctionDictionary> sffSfDictList = sff.getServiceFunctionDictionary();
        for (ServiceFunctionDictionary dict : sffSfDictList) {
            if (dict.getName().getValue().equals(sfName.getValue())) {
                sffSfDict = dict;
                break;
            }
        }

        return sffSfDict;
    }

    /**
     * Return the mac address from a SF DPL, only if its a MAC DPL.
     *
     * @param sfDpl the SF DPL to process
     * @return macAddress string or null if its not a MAC DPL
     */
    public String getSfDplMac(SfDataPlaneLocator sfDpl) {
        String sfMac = null;

        LocatorType sffLocatorType = sfDpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            if (((MacAddressLocator) sffLocatorType).getMac() != null) {
                sfMac = ((MacAddressLocator) sffLocatorType).getMac().getValue();
            }
        }

        return sfMac;
    }

    /**
     * Given an SFF object and SFF-SF dictionary entry, return the switch port string.
     * Looks for the SFF DPL name in the SFF-SF dictionary, then looks up
     * that DPL name on the SFF.
     *
     * @param sff the SFF to process
     * @param dict used to get the SFF DPL name
     * @return switch port string, INPORT if not augmented, INPORT if not found
     */
    public String getDictPortInfoPort(final ServiceFunctionForwarder sff, final ServiceFunctionDictionary dict) {
        SffDataPlaneLocator sffDpl = getSffDataPlaneLocator(sff, dict.getSffSfDataPlaneLocator().getSffDplName());
        OfsPort ofsPort = getSffPortInfoFromDpl(sffDpl);

        if (ofsPort == null) {
            // This case is most likely because the sff-of augmentation wasnt used
            // assuming the packet should just be sent on the same port it was received on
            return OutputPortValues.INPORT.toString();
        }

        return ofsPort.getPortId();
    }

    /**
     * Given a possibly augmented SFF DPL, return the augmented OfsPort object.
     *
     * @param sffDpl The SFF DPL to process
     * @return OfsPort, null if not augmented, null if not found
     */
    public OfsPort getSffPortInfoFromDpl(final SffDataPlaneLocator sffDpl) {
        if (sffDpl == null) {
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = sffDpl.getAugmentation(SffDataPlaneLocator1.class);
        if (ofsDpl == null) {
            LOG.debug("No OFS DPL available for dpl [{}]", sffDpl.getName().getValue());
            return null;
        }

        return ofsDpl.getOfsPort();
    }

    /**
     * Given a possibly augmented SFF DPL, return the DPL switch port.
     * The augmentation will be a OfsPort object.
     *
     * @param dpl the SFF DPL to process
     * @return switch port string, INPORT if not augmented, INPORT if not found
     */
    public String getDplPortInfoPort(final SffDataPlaneLocator dpl) {
        OfsPort ofsPort = getSffPortInfoFromDpl(dpl);

        if (ofsPort == null) {
            // This case is most likely because the sff-of augmentation wasnt used
            // assuming the packet should just be sent on the same port it was received on
            return OutputPortValues.INPORT.toString();
        }

        return ofsPort.getPortId();
    }

    /**
     * Given a possibly augmented SFF DPL, return the DPL mac address.
     * The augmentation will be a OfsPort object.
     *
     * @param dpl the SFF DPL to process
     * @return mac address string, null if not augmented, null if not found
     */
    public String getDplPortInfoMac(final SffDataPlaneLocator dpl) {
        if (dpl == null) {
            return null;
        }

        String macStr = null;
        OfsPort ofsPort = getSffPortInfoFromDpl(dpl);

        if (ofsPort != null) {
            if (ofsPort.getMacAddress() != null) {
                macStr = ofsPort.getMacAddress().getValue();
            }
        }

        if (macStr == null) {
            if (dpl.getDataPlaneLocator().getTransport().equals(
                    org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac.class)) {
                MacAddress mac = ((MacAddressLocator) dpl.getDataPlaneLocator().getLocatorType()).getMac();
                if (mac != null) {
                    macStr = mac.getValue();
                }
            }
        }

        return macStr;
    }

    /**
     * Given an SFF object and SFF-SF dictionary entry, return the SFF Mac.
     * Looks for the SFF DPL name in the SFF-SF dictionary, then looks up
     * that DPL name on the SFF.
     *
     * @param sff The SFF to process
     * @param dict contains the SFF DPL name to process
     * @return MAC Address string, null if the DPL is not mac, null if not found
     */
    public String getDictPortInfoMac(final ServiceFunctionForwarder sff, final ServiceFunctionDictionary dict) {
        SffDataPlaneLocator sffDpl = getSffDataPlaneLocator(sff, dict.getSffSfDataPlaneLocator().getSffDplName());
        String macStr = getDplPortInfoMac(sffDpl);

        // If the SFF DPL wasnt augmented, check if the DPL is of type mac, and return that mac address
        if (macStr == null) {
            if (sffDpl.getDataPlaneLocator().getTransport().equals(
                    org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac.class)) {
                MacAddress mac = ((MacAddressLocator) sffDpl.getDataPlaneLocator().getLocatorType()).getMac();
                if (mac != null) {
                    macStr = mac.getValue();
                }
            }
        }

        return macStr;
    }

    /**
     * Given an SFF name, return the augmented OpenFlow NodeName
     *
     * @param sffName The SFF name to process
     * @param rspId the rsp the SFF is being processed on
     * @return OpenFlow NodeName, null if not augmented, null if not found
     */
    public String getSffOpenFlowNodeName(final SffName sffName, long rspId) {
        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName, rspId);
        // TODO return more addressing type - NodeId
        return getSffOpenFlowNodeName(sff);
    }

    /**
     * Given an SFF object, return the augmented OpenFlow NodeName
     *
     * @param sff The SFF name to process
     * @return OpenFlow NodeName or null if augmented or not found
     */
    public String getSffOpenFlowNodeName(final ServiceFunctionForwarder sff) {
        if (sff == null) {
            return null;
        }

        // Check if its an service-function-forwarder-ovs augmentation
        // if it is, then get the open flow node id there
        SffOvsBridgeAugmentation ovsSff = sff.getAugmentation(SffOvsBridgeAugmentation.class);
        if (ovsSff != null) {
            if (ovsSff.getOvsBridge() != null) {
                return ovsSff.getOvsBridge().getOpenflowNodeId();
            }
        }

        // it its not an sff-ovs, then just return the ServiceNode
        return sff.getServiceNode().getValue();
    }

}
