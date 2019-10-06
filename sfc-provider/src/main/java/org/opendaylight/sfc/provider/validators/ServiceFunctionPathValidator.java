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
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.sfc.provider.validators.util.DataValidationFailedWithMessageException;
import org.opendaylight.sfc.provider.validators.util.SfcDatastoreCache;
import org.opendaylight.sfc.provider.validators.util.ValidationConstants;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class performs pre-commit validation for new / modified SFPs in order to
 * guarantee that the SFs it specifies are consistent with the types specified
 * in the associated SF chain.
 *
 * @author Diego Granados (diego.jesus.granados.lopez@ericsson.com)
 */
@Singleton
public class ServiceFunctionPathValidator {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionPathValidator.class);

    private final DOMDataTreeCommitCohortRegistry registry;

    @Inject
    public ServiceFunctionPathValidator(final DOMDataBroker domDataBroker) {
        this.registry = (DOMDataTreeCommitCohortRegistry) domDataBroker.getSupportedExtensions()
                .get(org.opendaylight.controller.md.sal.dom.api.DOMDataTreeCommitCohortRegistry.class);
        registerValidationCohorts();
    }

    private void registerValidationCohorts() {
        ServiceFunctionPathCohort myCohort = new ServiceFunctionPathCohort(this);

        LOG.debug("registerValidationCohorts: sfp cohort created");
        registry.registerCommitCohort(ValidationConstants.SFP_ID, myCohort);
        LOG.info("registerValidationCohorts:initialized. registered cohort: {}", myCohort);
    }

    /**
     * Performs validation of a service function path.
     *
     * @param serviceFunctionPath
     *            a candidate SFP that is being added / updated in a currently
     *            open transaction
     * @return true when validation is passed (i.e. when the SFs contained in
     *         the SFP are coherent (type-wise) with the type definitions in the
     *         associated SFC; false afterwards)
     * @throws DataValidationFailedWithMessageException
     *             when validation cannot be performed because some of the
     *             referenced SFs / SFCs do not exist
     */
    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    protected boolean validateServiceFunctionPath(ServiceFunctionPath serviceFunctionPath)
            throws DataValidationFailedWithMessageException {
        if (serviceFunctionPath != null) {
            LOG.debug("ServiceFunctionPathListener:validateServiceFunctionPath:starting..(new sfc name: {})",
                    serviceFunctionPath.getName());

            if (serviceFunctionPath.getServicePathHop() == null) {
                // no hops defined in SFP -> nothing to validate
                LOG.info("ServiceFunctionPathListener:validateServiceFunctionPath:"
                        + "SFP without explicit hop definition -> validation not required");
                return true;
            }

            // 1. Get the size of the list of SF names in the SFP
            int numberOfSpecifiedSFs = serviceFunctionPath.getServicePathHop().size();

            // 2. Get the SF types referenced in the chain (fail if can't find
            // the chain)
            List<String> sfChainTypes;
            try {
                sfChainTypes = SfcDatastoreCache.getSfChainToSfTypeList()
                        .get(serviceFunctionPath.getServiceChainName());
            } catch (ExecutionException e) {
                LOG.debug("validateServiceFunctionPath - getSfChainToSfTypeList failed", e);
                throw ValidationConstants.SFP_FAILED_CAN_COMMIT_EXCEPTION_SFC_MISSING;
            }

            // 3. Referential integrity (SFC)
            if (sfChainTypes == null || sfChainTypes.isEmpty()) {
                LOG.error("validateServiceFunctionPath:: ERROR! (no sf chains defined!)");
                throw ValidationConstants.SFP_FAILED_CAN_COMMIT_EXCEPTION_SFC_MISSING;
            }

            // 4. Correct number of values in SFC types, SFs in the SFP
            LOG.debug(
                    "validateServiceFunctionPath:retrieved SFC {} for SFP {}); they have {}, {} elements respectively",
                    serviceFunctionPath.getServiceChainName().getValue(), serviceFunctionPath.getName().getValue(),
                    sfChainTypes.size(), numberOfSpecifiedSFs);

            // A chain can have more elements than the list of service
            // functions, but not the other way around
            // (A SFP can choose to set all or only some of the SFs in the path)
            if (sfChainTypes.size() < numberOfSpecifiedSFs) {
                LOG.error("validateServiceFunctionPath: ERROR! (incorrect chain-path list sizes [chain={}, path={}])",
                        sfChainTypes.size(), numberOfSpecifiedSFs);
                return false;
            }

            // 5. SF type matching
            boolean errorFound = false;
            for (int i = 0; i < numberOfSpecifiedSFs; i++) {
                SfName sfName = serviceFunctionPath.getServicePathHop().get(i).getServiceFunctionName();
                if (sfName == null) {
                    continue;
                }
                String sfChainTypeName = sfChainTypes
                        .get(serviceFunctionPath.getServicePathHop().get(i).getHopNumber().toJava());

                String sfTypeNameFromSFP;
                try {
                    sfTypeNameFromSFP = SfcDatastoreCache.getSfToSfTypeCache().get(sfName);
                } catch (ExecutionException e) {
                    LOG.debug("validateServiceFunctionPath - getSfToSfTypeCache failed", e);
                    throw ValidationConstants.SFP_FAILED_CAN_COMMIT_EXCEPTION_SF_MISSING;
                }

                if (!sfChainTypeName.equals(sfTypeNameFromSFP)) {
                    LOG.error("Error on SFP validation! element with index {} is not of the correct type [{}/{}]", i,
                            sfChainTypeName, sfTypeNameFromSFP);
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
