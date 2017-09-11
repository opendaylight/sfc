/**
 * Copyright (c) 2017 Ericsson S.A. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.shell;

import java.util.List;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Karaf command to show the Service Function Type provisioned.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 *
 */
@Service
@Command(scope = "sfc",
        name = "sft-list", description = "Show the provisioned Service Function Types")
public class ServiceFunctionTypeCommand extends AbstractCommand {
    @Option(name = "-name", aliases = {
            "--name"}, description = "Name of the Service Function Type", required = false, multiValued = false)
    private String name;

    private final ShellTable table;

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionTypeCommand.class);

    public ServiceFunctionTypeCommand() {
        table = new ShellTable();
        table.column(new Col("Name"));
    }

    @Override
    public Object execute() throws Exception {
        LOG.debug("Service Function Type name: {}", name);

        if (name != null) {
            renderContent(SfcProviderServiceTypeAPI.readServiceFunctionType(new SftTypeName(name)));
        } else {
            LOG.debug("Getting the list of Service Function Types");
            ServiceFunctionTypes allServiceFunctionTypes = SfcProviderServiceTypeAPI.readAllServiceFunctionTypes();

            if (allServiceFunctionTypes != null) {
                List<ServiceFunctionType> serviceFunctionTypes = allServiceFunctionTypes.getServiceFunctionType();
                serviceFunctionTypes.forEach(this::renderContent);
            }
        }
        table.print(getConsole());
        return null;
    }

    private void renderContent(ServiceFunctionType serviceFunctionType) {
        if (serviceFunctionType != null) {
            LOG.debug("Service Function Type data: {}", serviceFunctionType);
            table.addRow().addContent(serviceFunctionType.getType().getValue());
        }
    }
}
