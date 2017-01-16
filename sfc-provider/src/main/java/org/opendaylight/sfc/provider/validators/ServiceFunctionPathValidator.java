/*
 * Copyright (c) 2017 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.validators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.common.api.DataValidationFailedException;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.sfc.provider.validators.util.ValidationConstants;
import org.opendaylight.sfc.provider.validators.util.DataValidationFailedWithMessageException;
import org.opendaylight.sfc.provider.validators.util.SfcDatastoreCache;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

import javassist.ClassPool;

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

        /**
         * After registration, the canCommit() method will be invoked in order to validate
         * SFP datastore object creations / modifications. This validation will check
         * coherence with referenced SF, SFC type definitions
         */
        DOMDataTreeCommitCohort myCohort = new DOMDataTreeCommitCohort() {
            ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
            ImmutableSet<YangModuleInfo> infos = BindingReflections.loadModuleInfos();
            BindingRuntimeContext bindingContext;
            BindingNormalizedNodeCodecRegistry codecRegistry =
                    new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator
                            .create(JavassistUtils.forClassPool(ClassPool.getDefault())));

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public CheckedFuture<PostCanCommitStep, DataValidationFailedException> canCommit(
                    Object txId, DOMDataTreeCandidate candidate,
                    SchemaContext ctx) {

                LOG.debug("registerValidationCohorts:canCommit:called! txId={}, candidate={}, context={} ", txId, candidate, ctx);

                DataTreeCandidateNode candidateRoot = candidate.getRootNode();
                NormalizedNode nn = candidateRoot.getDataAfter().get();

                LOG.debug("registerValidationCohorts:canCommit:updating codec contexts");
                moduleContext.addModuleInfos(infos);
                bindingContext = BindingRuntimeContext.create(moduleContext, ctx);
                codecRegistry.onBindingRuntimeContextUpdated(bindingContext);

                LOG.debug("registerValidationCohorts:Mapping service ready");

                LOG.debug("registerValidationCohorts:before deserializing:  {}", nn);
                //(nn is an immutableMapNode). Contains an unmodifiable collection
                // reference of all this thing
                //https://wiki.opendaylight.org/view/OpenDaylight_Controller:MD-SAL:Design:Normalized_DOM_Model
                Collection<MapEntryNode> c = (Collection<MapEntryNode>) nn.getValue();
                LOG.debug("registerValidationCohorts:collection containing the sfs:  {}", c);
                Iterator<MapEntryNode> menIter = (Iterator<MapEntryNode>)c.iterator();
                while (menIter.hasNext()) {
                    MapEntryNode meNode = menIter.next();
                    LOG.debug("registerValidationCohorts:sfp to process: {}", meNode);
                    NormalizedNode sfpAsNormalizedNode = (NormalizedNode)meNode;
                    LOG.debug("registerValidationCohorts:the first SF (as nn):  {}", sfpAsNormalizedNode);
                    DataObject dobj = codecRegistry.fromNormalizedNode(ValidationConstants.SFP_PATH_YII, sfpAsNormalizedNode).getValue();
                    LOG.debug("registerValidationCohorts:the first SFP (as dataobject):  {}", dobj);
                    ServiceFunctionPath sfp = (ServiceFunctionPath)dobj;
                    LOG.debug("registerValidationCohorts:the implemented interface: {}", dobj.getImplementedInterface());
                    LOG.debug("registerValidationCohorts:the first SF (as binding representation):  {}", sfp);
                    try {
                        if (!validateServiceFunctionPath(sfp)) {
                                return ValidationConstants.FAILED_CAN_COMMIT_SFP_FUTURE;
                        }
                    } catch (DataValidationFailedException dvfe) {
                        return Futures.immediateFailedCheckedFuture(dvfe);
                    }
                }
                return ValidationConstants.SUCCESS_CAN_COMMIT_FUTURE;
            }};

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
    private boolean validateServiceFunctionPath(
            ServiceFunctionPath serviceFunctionPath) throws DataValidationFailedWithMessageException {
        if (serviceFunctionPath != null) {
            LOG.debug("ServiceFunctionPathListener:validateServiceFunctionPath:starting..(new sfc name: {})",
                    serviceFunctionPath.getName());

            List<SfName> serviceFunctionNamesForPath = new ArrayList<>();
            // SfcServiceFunction sfcSf = null;
            for (ServicePathHop sfpH : serviceFunctionPath
                    .getServicePathHop()) {
                LOG.debug("ServiceFunctionPathListener:validateServiceFunctionPath::   new sfp hop found; sfname={}, group name={})",
                        sfpH.getServiceFunctionName().getValue(),
                        sfpH.getServiceFunctionGroupName());
                serviceFunctionNamesForPath
                        .add(sfpH.getServiceFunctionName());
            }

            // got the list - validation time!
            List<String> sfChainTypes;
            try {
                sfChainTypes = SfcDatastoreCache.sfChainToSfTypeList
                        .get(serviceFunctionPath.getServiceChainName());
            } catch (ExecutionException e) {
                throw ValidationConstants.SFP_FAILED_CAN_COMMIT_EXCEPTION_SF_MISSING;
            }

            // if there are no chains defined, then fail
            if (sfChainTypes == null || sfChainTypes.isEmpty()) {
                LOG.error("validateServiceFunctionPath:: ERROR! (no sf chains defined!)");
                throw ValidationConstants.SFP_FAILED_CAN_COMMIT_EXCEPTION_SFC_MISSING;
            }

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
            boolean errorFound = false;
            for (int i = 0; i < sfChainTypes.size(); i++) {
                String sfChainTypeName = sfChainTypes.get(i);
                SfName sfName = serviceFunctionNamesForPath.get(i);
                String sfTypeNameFromSFP = SfcDatastoreCache.sfToSfTypeCache
                        .getUnchecked(sfName);

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
