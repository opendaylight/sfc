/**
 * Copyright (c) 2014 by Ericsson and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */


package org.opendaylight.sfc.l2renderer;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.port.details.OfsPort;

/**
 * This interface allows us to completely isolate the SfcL2RspProcessor and SfcL2SfgDataListener
 * from the sfc-provider-api and the controller data store, which makes it much easier to Unit
 * Test both classes.
 * 
 * @author Brady Johnson
 *
 */
public interface SfcL2ProviderUtilsInterface {
    public void resetCache();

    /**
     * Return a named SffDataPlaneLocator
     *
     * @param sff - The SFF to search in
     * @param dplName - The name of the DPL to look for
     * @return SffDataPlaneLocator or null if not found
     */
    public SffDataPlaneLocator getSffDataPlaneLocator(ServiceFunctionForwarder sff, String dplName);

    /**
     * Return the SfDataPlaneLocator
     *
     * @param sff - The SFF to search in
     * @param dplName - The name of the DPL to look for
     * @return SffDataPlaneLocator or null if not found
     */
    public SfDataPlaneLocator getSfDataPlaneLocator(ServiceFunction sf);

    /**
     * Return a named SffSfDataPlaneLocator
     *
     * @param sff - The SFF to search in
     * @param sfName - The name of the DPL to look for
     * @return SffSfDataPlaneLocator or null if not found
     */
    public SffSfDataPlaneLocator getSffSfDataPlaneLocator(ServiceFunctionForwarder sff, String sfName);

    public ServiceFunctionDictionary getSffSfDictionary(ServiceFunctionForwarder sff, String sfName);

    public String getSfDplMac(SfDataPlaneLocator sfDpl);

    public String getDictPortInfoPort(final ServiceFunctionDictionary dict);

    public OfsPort getSffPortInfoFromSffSfDict(final ServiceFunctionDictionary sffSfDict);

    public OfsPort getSffPortInfoFromDpl(final SffDataPlaneLocator sffDpl);

    public String getDplPortInfoPort(final SffDataPlaneLocator dpl);

    public String getDplPortInfoMac(final SffDataPlaneLocator dpl);


    public String getDictPortInfoMac(final ServiceFunctionDictionary dict);

    public String getSffOpenFlowNodeName(final String sffName);

    public String getSffOpenFlowNodeName(final ServiceFunctionForwarder sff);

    /**
     * Return the named ServiceFunction
     * Acts as a local cache to not have to go to DataStore so often
     * First look in internal storage, if its not there
     * get it from the DataStore and store it internally
     *
     * @param sfName - The SF Name to search for
     * @return - The ServiceFunction object, or null if not found
     */
    public ServiceFunction getServiceFunction(final String sfName);

    /**
     * Return the named ServiceFunctionForwarder
     * Acts as a local cache to not have to go to DataStore so often
     * First look in internal storage, if its not there
     * get it from the DataStore and store it internally
     *
     * @param sffName - The SFF Name to search for
     * @return The ServiceFunctionForwarder object, or null if not found
     */
    public ServiceFunctionForwarder getServiceFunctionForwarder(final String sffName);

    public ServiceFunctionGroup getServiceFunctionGroup(final String sfgName);

}
