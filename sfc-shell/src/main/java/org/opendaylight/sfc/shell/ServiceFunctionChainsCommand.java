/**
 * Copyright (c) 2017 Ericsson S.A. and others.  All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.shell;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping
        .ServiceFunctionChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Karaf CLI command to show the provisioned Service Function Chains.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
@Service
@Command(scope = "sfc", name = "sc-list", description = "Show the provisioned Service Function Chains")
public class ServiceFunctionChainsCommand extends AbstractCommand {

    @Option(name = "-name", aliases = {"--name"}, description = "Name of the Service Function Chain", required =
            false, multiValued = false)
    private String name;

    private final ShellTable table;

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionChainsCommand.class);

    public ServiceFunctionChainsCommand() {
        table = new ShellTable();
        table.column(new Col("Name"));
        table.column(new Col("SF Types"));
    }

    @Override
    public Object execute() throws Exception {
        LOG.debug("Service Function Chain name: {}", name);

        if (name != null) {
            renderContent(SfcProviderServiceChainAPI.readServiceFunctionChain(new SfcName(name)));
        } else {
            LOG.debug("Getting the list of Service Function Chains");
            ServiceFunctionChains allServiceFunctionChains = SfcProviderServiceChainAPI.readAllServiceFunctionChains();
            if (allServiceFunctionChains != null) {
                List<ServiceFunctionChain> serviceFunctionChains = allServiceFunctionChains.getServiceFunctionChain();
                serviceFunctionChains.forEach(this::renderContent);
            }
        }
        table.print(getConsole());
        return null;
    }

    private void renderContent(ServiceFunctionChain serviceFunctionChain) {
        if (serviceFunctionChain != null) {
            LOG.debug("Service Function Chain data: {}", serviceFunctionChain);
            table.addRow().addContent(serviceFunctionChain.getName().getValue(),
                                      serviceFunctionChain.getSfcServiceFunction()
                                              == null ? NO_VALUE : serviceFunctionChain.getSfcServiceFunction().stream()
                                              .map(sf -> sf.getType().getValue())
                                              .collect(Collectors.joining(SEPARATOR)));
        }
    }
}
