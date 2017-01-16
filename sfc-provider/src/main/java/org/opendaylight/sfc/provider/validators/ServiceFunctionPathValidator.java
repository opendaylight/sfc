/*
 * Copyright (c) 2017 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.validators;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.sfc.provider.validators.util.ValidationConstants;
import org.opendaylight.sfc.provider.validators.util.DataValidationFailedWithMessageException;
import org.opendaylight.sfc.provider.validators.util.SfcDatastoreCache;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class performs pre-commit validation for new / modified SFPs in
 * order to guarantee that the SFs it specifies are consistent with
 * the types specified in the associated SF chain
 * @author Diego Granados (diego.jesus.granados.lopez@ericsson.com)
 */
public class ServiceFunctionPathValidator {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionPathValidator.class);

    private final DOMDataTreeCommitCohortRegistry registry;

    public ServiceFunctionPathValidator(final DOMDataBroker domDataBroker) {
        this.registry = (DOMDataTreeCommitCohortRegistry)domDataBroker
                .getSupportedExtensions()
                .get(org.opendaylight.controller.md.sal.dom.api.DOMDataTreeCommitCohortRegistry.class);
    }

    public void init() {
        LOG.debug("ServiceFunctionPathValidator:Initializing...");
        registerValidationCohorts();
    }

    private void registerValidationCohorts() {

            ServiceFunctionPathCohort myCohort = new ServiceFunctionPathCohort(this);

            LOG.debug("registerValidationCohorts: sfp cohort created");
            registry.registerCommitCohort(ValidationConstants.SFP_ID, myCohort);
            LOG.info("registerValidationCohorts:initialized. registered cohort: {}", myCohort);

    }

    public void close() throws Exception {
        LOG.debug("close(): closing sfp validator ...");
    }

    /**
     * Performs validation of a service function path
     * @param serviceFunctionPath a candidate SFP that is being added / updated in a currently open transaction
     * @return true when validation is passed (i.e. when the SFs contained in the SFP are coherent (type-wise)
     *         with the type definitions in the associated SFC; false afterwards)
     * @throws DataValidationFailedWithMessageException when validation cannot be performed because some of the
     *         referenced SFs / SFCs do not exist
     */
    protected boolean validateServiceFunctionPath(
            ServiceFunctionPath serviceFunctionPath) throws DataValidationFailedWithMessageException {
        if (serviceFunctionPath != null) {
            LOG.debug("ServiceFunctionPathListener:validateServiceFunctionPath:starting..(new sfc name: {})",
                    serviceFunctionPath.getName());

            // 1. Get the list of SF names in the SFP
            List<SfName> serviceFunctionNamesForPath = serviceFunctionPath.getServicePathHop().stream()
                    .map(sfph -> sfph.getServiceFunctionName())
                    .peek(sfphName -> LOG.debug("new sfp hop: sfp name =[{}]", sfphName.getValue()))
                    .collect(Collectors.toList());

            // 2. Get the SF types referenced in the chain (fail if can't find the chain)
            List<String> sfChainTypes;
            try {
                sfChainTypes = SfcDatastoreCache.getSfChainToSfTypeList()
                        .get(serviceFunctionPath.getServiceChainName());
            } catch (ExecutionException e) {
                throw ValidationConstants.SFP_FAILED_CAN_COMMIT_EXCEPTION_SFC_MISSING;
            }

            // 3. Referential integrity (SFC)
            if (sfChainTypes == null || sfChainTypes.isEmpty()) {
                LOG.error("validateServiceFunctionPath:: ERROR! (no sf chains defined!)");
                throw ValidationConstants.SFP_FAILED_CAN_COMMIT_EXCEPTION_SFC_MISSING;
            }

            // 4. Same number of values in SFC types, SFs in the SFP
            LOG.debug("validateServiceFunctionPath:retrieved SFC {} for SFP {}); they have {}, {} elements respectively",
                    serviceFunctionPath.getServiceChainName().getValue(),
                    serviceFunctionPath.getName().getValue(),
                    sfChainTypes.size(), serviceFunctionNamesForPath.size());

            if (sfChainTypes.size() != serviceFunctionNamesForPath.size()) {
                LOG.error(
                        "validateServiceFunctionPath: ERROR! (incorrect chain-path list sizes [chain={}, path={}])",
                        sfChainTypes.size(),
                        serviceFunctionNamesForPath.size());
                return false;
            }

            // 5. SF type matching
            boolean errorFound = false;
            for (int i = 0; i < sfChainTypes.size(); i++) {
                String sfChainTypeName = sfChainTypes.get(i);
                SfName sfName = serviceFunctionNamesForPath.get(i);
                String sfTypeNameFromSFP;
                try {
                    sfTypeNameFromSFP = SfcDatastoreCache.getSfToSfTypeCache()
                            .get(sfName);
                } catch (ExecutionException e) {
                    throw ValidationConstants.SFP_FAILED_CAN_COMMIT_EXCEPTION_SF_MISSING;
                }

                if (!sfChainTypeName.equals(sfTypeNameFromSFP)) {
                    LOG.error(
                            "validateServiceFunctionPath: error on SFP validation! element with index {} is not of the correct type [{}/{}]",
                            i, sfChainTypeName, sfTypeNameFromSFP);
                    errorFound = true;
                    break;
                }
            }
            if (!errorFound) {
                LOG.info("validateServiceFunctionPath:SFP validation passed!");
                return true;
            }
        }
        return false;
    }
}
