/**
 * Copyright (c) 2017 Ericsson S.A. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.shell;

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.Session;

/**
 * Abstract command with some common functionality to be inherited by the rest
 * of the commands.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 *
 */
public abstract class AbstractCommand implements Action {

    @Reference
    protected Session session;

    public Session getSession() {
        return session;
    }

    public PrintStream getConsole() {
        return getSession().getConsole();
    }
}
