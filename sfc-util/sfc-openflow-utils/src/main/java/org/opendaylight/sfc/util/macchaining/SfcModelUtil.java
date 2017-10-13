/*
 * Copyright (c) 2016 Hewlett Packard Enterprise Development LP. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.util.macchaining;

import java.util.List;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;


public final class SfcModelUtil {

    private SfcModelUtil() {
    }

    public static SffDataPlaneLocator searchSrcDplInConnectedSffs(SffName sffSrcName, SffName dstSffName) {

        if (!sffSrcName.equals(dstSffName)) {
            ConnectedSffDictionary connectedSffs =
                    SfcProviderServiceForwarderAPI.getSffSffConnectedDictionary(sffSrcName, dstSffName);
            if (connectedSffs == null) {
                return null;
            }

            ServiceFunctionForwarder srcSff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffSrcName);

            List<SffDataPlaneLocator> dplsFromSrcSff = srcSff.getSffDataPlaneLocator();

            for (SffDataPlaneLocator srcSffDpl : dplsFromSrcSff) {
                if (srcSffDpl.getDataPlaneLocator().getTransport()
                        .equals(connectedSffs.getSffSffDataPlaneLocator().getTransport())
                        && srcSffDpl.getDataPlaneLocator().getLocatorType() instanceof MacAddressLocator) {
                    MacAddressLocator macSrcSff = (MacAddressLocator) srcSffDpl.getDataPlaneLocator().getLocatorType();
                    MacAddressLocator macToNextSff = (MacAddressLocator) connectedSffs.getSffSffDataPlaneLocator()
                            .getLocatorType();

                    if (macSrcSff.getMac().equals(macToNextSff.getMac())) {
                        return srcSffDpl;
                    }
                }
            }
        }
        return null;
    }
}
