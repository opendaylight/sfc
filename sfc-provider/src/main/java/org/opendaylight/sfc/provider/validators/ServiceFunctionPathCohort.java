/*
 * Copyright (c) 2017 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.validators;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Collection;
import java.util.Iterator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.common.api.DataValidationFailedException;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.sfc.provider.validators.util.ValidationConstants;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-commit semantic validation for service function path datastore objects
 *
 * <p>
 * After registration, the canCommit() method will be invoked in order to
 * validate SFP object creations / modifications. This validation will check
 * coherence with referenced SF, SFC type definitions
 */
public class ServiceFunctionPathCohort implements DOMDataTreeCommitCohort {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionPathCohort.class);
    private final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
    private final ImmutableSet<YangModuleInfo> infos = BindingReflections.loadModuleInfos();
    private BindingRuntimeContext bindingContext;
    private final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry();
    private final ServiceFunctionPathValidator sfpv;

    public ServiceFunctionPathCohort(ServiceFunctionPathValidator sfpv) {
        this.sfpv = sfpv;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public FluentFuture<PostCanCommitStep> canCommit(Object txId, SchemaContext ctx,
            Collection<DOMDataTreeCandidate> candidates) {

        for (DOMDataTreeCandidate candidate : candidates) {
            LOG.debug("canCommit:called! txId={}, candidate={}, context={} ", txId, candidate, ctx);

            DataTreeCandidateNode candidateRoot = candidate.getRootNode();
            NormalizedNode nn = candidateRoot.getDataAfter().orElse(null);
            if (nn == null) {
                LOG.debug("canCommit:no sfp after the change");
                continue;
            }

            LOG.debug("canCommit:updating codec contexts");
            moduleContext.addModuleInfos(infos);
            bindingContext = BindingRuntimeContext.create(moduleContext, ctx);
            codecRegistry.onBindingRuntimeContextUpdated(bindingContext);

            LOG.debug("canCommit:Mapping service ready");

            LOG.debug("canCommit:before deserializing:  {}", nn);
            // (nn is an immutableMapNode). Contains an unmodifiable collection
            // reference of all this thing
            // https://wiki.opendaylight.org/view/OpenDaylight_Controller:MD-SAL:Design:Normalized_DOM_Model
            Collection collection = (Collection<MapEntryNode>) nn.getValue();
            LOG.debug("canCommit:collection containing the sfs:  {}", collection);
            Iterator<MapEntryNode> menIter = collection.iterator();
            while (menIter.hasNext()) {
                MapEntryNode meNode = menIter.next();
                LOG.debug("canCommit:sfp to process: {}", meNode);
                LOG.debug("canCommit:the first SF (as nn):  {}", meNode);
                DataObject dobj = codecRegistry.fromNormalizedNode(ValidationConstants.SFP_PATH_YII, meNode)
                        .getValue();
                LOG.debug("canCommit:registerValidationCohorts:the first SFP (as dataobject):  {}", dobj);
                ServiceFunctionPath sfp = (ServiceFunctionPath) dobj;
                LOG.debug("canCommit:registerValidationCohorts:the implemented interface: {}",
                        dobj.implementedInterface());
                LOG.debug("canCommit:the first SF (as binding representation):  {}", sfp);
                try {
                    if (!sfpv.validateServiceFunctionPath(sfp)) {
                        return ValidationConstants.FAILED_CAN_COMMIT_SFP_FUTURE;
                    }
                } catch (DataValidationFailedException dvfe) {
                    return FluentFutures.immediateFailedFluentFuture(dvfe);
                }
            }
        }

        return PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE;
    }
}
