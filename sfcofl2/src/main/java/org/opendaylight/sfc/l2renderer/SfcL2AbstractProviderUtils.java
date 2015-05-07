/**
 * Copyright (c) 2014 by Ericsson and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */


package org.opendaylight.sfc.l2renderer;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.ServiceFunctionDictionary1;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.port.details.OfsPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This interface allows us to completely isolate the SfcL2RspProcessor and SfcL2SfgDataListener
 * from the sfc-provider-api and the controller data store, which makes it much easier to Unit
 * Test both classes.
 *
 * @author Brady Johnson
 *
 */
public abstract class SfcL2AbstractProviderUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SfcL2AbstractProviderUtils.class);

    abstract public void resetCache();

    abstract public ServiceFunction getServiceFunction(final String sfName);

    abstract public ServiceFunctionForwarder getServiceFunctionForwarder(final String sffName);

    abstract public ServiceFunctionGroup getServiceFunctionGroup(final String sfgName);

    /**
     * Return a named SffDataPlaneLocator
     *
     * @param sff - The SFF to search in
     * @param dplName - The name of the DPL to look for
     * @return SffDataPlaneLocator or null if not found
     */
    public SffDataPlaneLocator getSffDataPlaneLocator(ServiceFunctionForwarder sff, String dplName) {
        SffDataPlaneLocator sffDpl = null;

        List<SffDataPlaneLocator> sffDataPlanelocatorList = sff.getSffDataPlaneLocator();
        for (SffDataPlaneLocator sffDataPlanelocator : sffDataPlanelocatorList) {
            if(sffDataPlanelocator.getName().equals(dplName)) {
                sffDpl = sffDataPlanelocator;
                break;
            }
        }

        return sffDpl;
    }

    /**
     * Return the SfDataPlaneLocator
     *
     * @param sff - The SFF to search in
     * @param dplName - The name of the DPL to look for
     * @return SffDataPlaneLocator or null if not found
     */
    public SfDataPlaneLocator getSfDataPlaneLocator(ServiceFunction sf) {
        // TODO how to tell which SF DPL to use if it has more than one?
        List<SfDataPlaneLocator> sfDataPlanelocatorList = sf.getSfDataPlaneLocator();
        return sfDataPlanelocatorList.get(0);
    }

    /**
     * Return a named SffSfDataPlaneLocator
     *
     * @param sff - The SFF to search in
     * @param sfName - The name of the DPL to look for
     * @return SffSfDataPlaneLocator or null if not found
     */
    public SffSfDataPlaneLocator getSffSfDataPlaneLocator(ServiceFunctionForwarder sff, String sfName) {
        SffSfDataPlaneLocator sffSfDpl = null;

        List<ServiceFunctionDictionary> sffSfDictList = sff.getServiceFunctionDictionary();
        for (ServiceFunctionDictionary sffSfDict : sffSfDictList) {
            if(sffSfDict.getName().equals(sfName)) {
                sffSfDpl = sffSfDict.getSffSfDataPlaneLocator();
            }
        }

        return sffSfDpl;
    }

    public ServiceFunctionDictionary getSffSfDictionary(ServiceFunctionForwarder sff, String sfName) {
        ServiceFunctionDictionary sffSfDict = null;

        List<ServiceFunctionDictionary> sffSfDictList = sff.getServiceFunctionDictionary();
        for (ServiceFunctionDictionary dict : sffSfDictList) {
            if(dict.getName().equals(sfName)) {
                sffSfDict = dict;
                break;
            }
        }
        return sffSfDict;
    }

    public String getSfDplMac(SfDataPlaneLocator sfDpl) {
        String sfMac = null;

        LocatorType sffLocatorType = sfDpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            if(((MacAddressLocator) sffLocatorType).getMac() != null) {
                sfMac = ((MacAddressLocator) sffLocatorType).getMac().getValue();
            }
        }

        return sfMac;
    }

    public String getDictPortInfoPort(final ServiceFunctionDictionary dict) {
        OfsPort ofsPort = getSffPortInfoFromSffSfDict(dict);

        if(ofsPort == null) {
            // This case is most likely because the sff-of augmentation wasnt used
            // assuming the packet should just be sent on the same port it was received on
            return OutputPortValues.INPORT.toString();
        }

        return ofsPort.getPortId();
    }

    public OfsPort getSffPortInfoFromSffSfDict(final ServiceFunctionDictionary sffSfDict) {
        if(sffSfDict == null) {
            return null;
        }
        ServiceFunctionDictionary1 ofsSffSfDict = sffSfDict.getAugmentation(ServiceFunctionDictionary1.class);
        if(ofsSffSfDict == null) {
            LOG.debug("No OFS SffSf Dictionary available for dict [{}]", sffSfDict.getName());
            return null;
        }

        return ofsSffSfDict.getOfsPort();
    }

    public OfsPort getSffPortInfoFromDpl(final SffDataPlaneLocator sffDpl) {
        if(sffDpl == null) {
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = sffDpl.getAugmentation(SffDataPlaneLocator1.class);
        if(ofsDpl == null) {
            LOG.debug("No OFS DPL available for dpl [{}]", sffDpl.getName());
            return null;
        }

        return ofsDpl.getOfsPort();
    }

    public String getDplPortInfoPort(final SffDataPlaneLocator dpl) {
        OfsPort ofsPort = getSffPortInfoFromDpl(dpl);

        if(ofsPort == null) {
            // This case is most likely because the sff-of augmentation wasnt used
            // assuming the packet should just be sent on the same port it was received on
            return OutputPortValues.INPORT.toString();
        }

        return ofsPort.getPortId();
    }

    public String getDplPortInfoMac(final SffDataPlaneLocator dpl) {
        OfsPort ofsPort = getSffPortInfoFromDpl(dpl);

        if(ofsPort == null) {
            return null;
        }

        if(ofsPort.getMacAddress() == null) {
            return null;
        }

        return ofsPort.getMacAddress().getValue();
    }


    public String getDictPortInfoMac(final ServiceFunctionDictionary dict) {
        OfsPort ofsPort = getSffPortInfoFromSffSfDict(dict);

        if(ofsPort == null) {
            return null;
        }

        if(ofsPort.getMacAddress() == null) {
            return null;
        }

        return ofsPort.getMacAddress().getValue();
    }

    public String getSffOpenFlowNodeName(final String sffName) {
        ServiceFunctionForwarder sff = getServiceFunctionForwarder(sffName);
        return getSffOpenFlowNodeName(sff);
    }

    public String getSffOpenFlowNodeName(final ServiceFunctionForwarder sff) {
        if(sff == null) {
            return null;
        }

        // Check if its an service-function-forwarder-ovs augmentation
        // if it is, then get the open flow node id there
        SffOvsBridgeAugmentation ovsSff = sff.getAugmentation(SffOvsBridgeAugmentation.class);
        if(ovsSff != null) {
            if(ovsSff.getOvsBridge() != null) {
                return ovsSff.getOvsBridge().getOpenflowNodeId();
            }
        }

        // it its not an sff-ovs, then just return the ServiceNode
        return sff.getServiceNode();
    }

}
