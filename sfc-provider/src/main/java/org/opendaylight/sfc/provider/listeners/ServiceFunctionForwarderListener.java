/*
 * Copyright (c) 2016, 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SnName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service Function
 * Forwarders taking the appropriate actions.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
@Singleton
public class ServiceFunctionForwarderListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionForwarder> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionForwarderListener.class);

    @Inject
    public ServiceFunctionForwarderListener(DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class));
    }

    @Override
    public void add(@Nonnull ServiceFunctionForwarder serviceFunctionForwarder) {
        LOG.info("Adding Service Function Forwarder: {}", serviceFunctionForwarder.getName());
    }

    @Override
    public void remove(@Nonnull ServiceFunctionForwarder serviceFunctionForwarder) {
        SffName sffName = serviceFunctionForwarder.getName();
        // Get RSPs of SFF
        LOG.info("Deleting Service Function Forwarder {}", sffName);
        List<RspName> rspNames = SfcProviderServiceForwarderAPI.readRspNamesFromSffState(sffName);

        LOG.info("Deleting Service Function Paths for RSPs {}", rspNames);
        for (RspName rspName : rspNames) {
            RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
            // This will in-turn delete the RSP from config and oper
            SfcProviderServicePathAPI.deleteServiceFunctionPath(rsp.getParentServiceFunctionPath());
        }
    }

    @Override
    public void update(@Nonnull ServiceFunctionForwarder originalServiceFunctionForwarder,
                       @Nonnull ServiceFunctionForwarder updatedServiceFunctionForwarder) {
        LOG.info("Updating Service Function Forwarder: {}", originalServiceFunctionForwarder.getName());
        List<RspName> rspNames = findAffectedRsp(originalServiceFunctionForwarder, updatedServiceFunctionForwarder);

        LOG.info("Deleting Service Function Paths for RSPs {}", rspNames);
        for (RspName rspName : rspNames) {
            RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
            LOG.info("Deleting SFP {}", rsp.getParentServiceFunctionPath().getValue());
            // This will in-turn delete the RSP from config and oper
            SfcProviderServicePathAPI.deleteServiceFunctionPath(rsp.getParentServiceFunctionPath());
        }
    }

    /**
     * Obtains the list of RSP affected by a change on the SFF.
     *
     * @param originalSff the original SFF.
     * @param updatedSff  the updated SFF.
     * @return a list of {@link RspName} of the affected RSP.
     */
    private List<RspName> findAffectedRsp(ServiceFunctionForwarder originalSff, ServiceFunctionForwarder updatedSff) {
        SffName sffName = originalSff.getName();
        List<RspName> rspNames = SfcProviderServiceForwarderAPI.readRspNamesFromSffState(sffName);

        // If service node changed, all RSPs are affected
        SnName originalSnName = originalSff.getServiceNode();
        SnName updatedSnName = updatedSff.getServiceNode();
        if (!Objects.equals(originalSnName, updatedSnName)) {
            LOG.debug("SFF service node updated: original {} updated {}", originalSnName, updatedSnName);
            return rspNames;
        }

        // If ip address changed, all RSPs are affected
        IpAddress originalIpAddress = originalSff.getIpMgmtAddress();
        IpAddress updatedIpAddress = updatedSff.getIpMgmtAddress();
        if (!Objects.equals(originalIpAddress, updatedIpAddress)) {
            LOG.debug("SFF IpAddress updated: original {} updated {}", originalIpAddress, updatedIpAddress);
            return rspNames;
        }

        // If any data plane locator changed, all RSPs are affected
        // TODO Current data model does not allow to know which DPL is used on a RSP
        List<SffDataPlaneLocator> originalLocators = originalSff.getSffDataPlaneLocator();
        List<SffDataPlaneLocator> updatedLocators = updatedSff.getSffDataPlaneLocator();
        boolean isAnyLocatorChanged = originalLocators != null && !originalLocators.isEmpty() && (
                updatedLocators == null || !updatedLocators.containsAll(originalLocators));
        if (isAnyLocatorChanged) {
            LOG.debug("SFF locators changed: original {} updated {}", originalLocators, updatedLocators);
            return rspNames;
        }

        // If a dictionary changed, any RSP making use of it is affected
        List<ServiceFunctionDictionary> originalDictList =
                originalSff.getServiceFunctionDictionary() != null ? originalSff
                        .getServiceFunctionDictionary() : Collections.emptyList();
        List<ServiceFunctionDictionary> updatedDictList = updatedSff.getServiceFunctionDictionary() != null ? updatedSff
                .getServiceFunctionDictionary() : Collections.emptyList();
        List<ServiceFunctionDictionary> removedDictList = updatedDictList
                .isEmpty() ? originalDictList : originalDictList.stream().filter(d -> !updatedDictList.contains(d))
                .collect(Collectors.toList());
        if (!removedDictList.isEmpty()) {
            LOG.debug("SFF dictionaries removed {}", removedDictList);
            return rspNames.stream().map(SfcProviderRenderedPathAPI::readRenderedServicePath)
                    .filter(rsp -> isAnyDictionaryUsedInRsp(sffName, rsp, removedDictList))
                    .map(RenderedServicePath::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Whether any hop of a RSP makes use of any SF dictionary of a given list for
     * the given SFF.
     *
     * @param sffName             the SFF name.
     * @param renderedServicePath the RSP.
     * @param dictionaries        the list of SF dictionaries.
     * @return true if the RSP makes use of the dictionary.
     */
    private boolean isAnyDictionaryUsedInRsp(final SffName sffName, final RenderedServicePath renderedServicePath,
                                             final List<ServiceFunctionDictionary> dictionaries) {
        List<SfName> dictionarySfNames = dictionaries.stream().map(ServiceFunctionDictionary::getName)
                .collect(Collectors.toList());
        return renderedServicePath.getRenderedServicePathHop().stream()
                .filter(rspHop -> sffName.equals(rspHop.getServiceFunctionForwarder()))
                .anyMatch(rspHop -> dictionarySfNames.contains(rspHop.getServiceFunctionName()));
    }
}
