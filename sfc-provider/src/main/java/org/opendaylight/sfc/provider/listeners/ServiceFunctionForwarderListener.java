/*
 * Copyright (c) 2016, 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SnName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
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
    public void add(@Nonnull InstanceIdentifier<ServiceFunctionForwarder> instanceIdentifier,
                    @Nonnull ServiceFunctionForwarder serviceFunctionForwarder) {
        LOG.info("Adding Service Function Forwarder: {}", serviceFunctionForwarder.getName());
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<ServiceFunctionForwarder> instanceIdentifier,
                       @Nonnull ServiceFunctionForwarder serviceFunctionForwarder) {
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
    public void update(@Nonnull InstanceIdentifier<ServiceFunctionForwarder> instanceIdentifier,
                       @Nonnull ServiceFunctionForwarder originalServiceFunctionForwarder,
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

        List<SffDataPlaneLocator> updatedLocators = updatedSff.getSffDataPlaneLocator();
        if (updatedLocators == null || updatedLocators.isEmpty()) {
            LOG.debug("Updated SFF has no locators");
            return rspNames;
        }

        List<ServiceFunctionDictionary> updatedDictList = updatedSff.getServiceFunctionDictionary();
        if (updatedDictList == null || updatedDictList.isEmpty()) {
            LOG.debug("Updated SFF has no dictionary entries");
            return rspNames;
        }

        List<SffDataPlaneLocator> originalLocators = originalSff.getSffDataPlaneLocator();
        if (originalLocators == null || originalLocators.isEmpty()) {
            LOG.debug("Original SFF has no locators");
            return Collections.emptyList();
        }

        List<ServiceFunctionDictionary> originalDictList = originalSff.getServiceFunctionDictionary();
        if (originalDictList == null || originalDictList.isEmpty()) {
            LOG.debug("Original SFF has no dictionary entries");
            return Collections.emptyList();
        }

        // What follows might require quite a bit of processing for a big SFF.
        // Enhancements to the data model could help in this regard.

        // Find out about removed locators
        Set<SffDataPlaneLocatorName> removedLocatorNames;
        removedLocatorNames = Sets.difference(new HashSet<>(originalLocators), new HashSet<>(updatedLocators)).stream()
            .map(SffDataPlaneLocator::getName)
            .collect(Collectors.toSet());

        // Find out about removed dictionary entries
        Set<ServiceFunctionDictionary> removedDictEntries;
        removedDictEntries =  Sets.difference(new HashSet<>(originalDictList), new HashSet<>(updatedDictList));
        Set<ServiceFunctionDictionary> invalidDictEntries = new HashSet<>(removedDictEntries);
        // A removed locator use in a dictionary entry invalidates it
        if (!removedLocatorNames.isEmpty()) {
            invalidDictEntries = updatedDictList.stream().collect(
                HashSet::new,
                (hash, dict) -> {
                    SffSfDataPlaneLocator sffSfDataPlaneLocator = dict.getSffSfDataPlaneLocator();
                    if (sffSfDataPlaneLocator == null) {
                        return;
                    }
                    boolean isInvalid = removedLocatorNames.contains(sffSfDataPlaneLocator.getSffDplName())
                           || removedLocatorNames.contains(sffSfDataPlaneLocator.getSffForwardDplName())
                           || removedLocatorNames.contains(sffSfDataPlaneLocator.getSffReverseDplName());
                    if (isInvalid) {
                        hash.add(dict);
                    }
                },
                HashSet::addAll);
        }
        // SFs cannot be used in the RSP if they have no dictionary entry
        List<SfName> invalidSfs = invalidDictEntries.stream()
                .map(ServiceFunctionDictionary::getName)
                .collect(Collectors.toList());
        // Removed locators may not affect single SFF RSPs
        boolean onlyAllowThisSff = removedLocatorNames.size() > 0;

        // check affected RSPs
        return rspNames.stream()
                .filter(rspName -> !isRspValid(rspName, sffName, onlyAllowThisSff, invalidSfs))
                .collect(Collectors.toList());
    }

    /**
     * Inspect RSP validness according to the following criteria:
     * If <code>isSingleSff</code> is set to <code>true</code>, it will
     * be checked that the entire path traverses only <code>sffName</code>.
     * It will also be checked that none of the service functions
     * included in <code>invalidSfList</code> are used for the path
     * in any hop along with <code>sffName</code>.
     *
     * @param rspName       the RSP name.
     * @param sffName       the SFF name.
     * @param isSingleSff   if the RSP must have a single SFF and
     *                      has to be <code>sffName</code>.
     * @param invalidSfList the disallowed SFs to be used along with
     *                      <code>sffName</code> for the RSP.
     * @return true if the RSP is valid according to the criteria defined
     *         above.
     */
    private boolean isRspValid(final RspName rspName,
                               final SffName sffName,
                               final boolean isSingleSff,
                               final List<SfName> invalidSfList) {
        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        for (RenderedServicePathHop hop : renderedServicePath.getRenderedServicePathHop()) {
            SffName serviceFunctionForwarder = hop.getServiceFunctionForwarder();
            SfName serviceFunctionName = hop.getServiceFunctionName();
            boolean sameSff = sffName.equals(serviceFunctionForwarder);
            if (!sameSff && isSingleSff) {
                return false;
            }
            if (sameSff && invalidSfList.contains(serviceFunctionName)) {
                return false;
            }
        }
        return true;
    }
}
