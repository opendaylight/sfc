# flake8: noqa
#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "rapenno@gmail.com"
__status__ = "alpha"


#: constants
OF_TABLEID = 0


# Static URLs for (local) testing
ODL_PORT = 8181
ODL_IP = 'localhost'
LOCAL_ODL_LOCATOR = "{ip}:{port}".format(ip=ODL_IP, port=ODL_PORT)
base_url = "http://{odl}/restconf/config/".format(odl=LOCAL_ODL_LOCATOR)

SF_URL = base_url + "service-function:service-functions/"
SFC_URL = base_url + "service-function-chain:service-function-chains/"
SFF_URL = base_url + "service-function-forwarder:service-function-forwarders/"
SFT_URL = base_url + "service-function-type:service-function-types/"
SFP_URL = base_url + "service-function-path:service-function-paths/"


#: sfc_agent URLs
SFF_PARAMETER_URL = ("http://{}/restconf/config/"
                     "service-function-forwarder:service-function-forwarders/")

SFF_NAME_PARAMETER_URL = ("http://{}/restconf/config/"
                          "service-function-forwarder:"
                          "service-function-forwarders/"
                          "service-function-forwarder/{}")

SF_NAME_PARAMETER_URL = ("http://{}/restconf/config/"
                         "service-function:"
                         "service-functions/"
                         "service-function/{}")

SFF_SF_DATA_PLANE_LOCATOR_URL = ("http://{}/restconf/config/"
                                 "service-function-forwarder:"
                                 "service-function-forwarders/"
                                 "service-function-forwarder/{}/"
                                 "service-function-dictionary/{}/"
                                 "sff-sf-data-plane-locator/")


class SfcGlobals:
    """
    Contains all relevant data (SFs, SFFs, RSPs, data_plane, ODL locator and
    credentials) for the locally running sfc_agent.
    """
    sff_os = "ODL"
    my_sff_name = None
    sff_os_set = {"OVS", "XE"}

    path = {}
    sf_topo = {}
    sff_topo = {}
    sf_threads = {}
    sff_threads = {}
    data_plane_path = {}
    data_plane_control_port = 6000

    odl_locator = LOCAL_ODL_LOCATOR
    odl_credentials = ('admin', 'admin')

    def get_path(self):
        return self.path

    def reset_path(self):
        self.path = {}

    def get_sf_topo(self):
        return self.sf_topo

    def reset_sf_topo(self):
        self.sf_topo = {}

    def get_sff_topo(self):
        return self.sff_topo

    def reset_sff_topo(self):
        self.sff_topo = {}

    def get_sf_threads(self):
        return self.sf_threads

    def get_sff_threads(self):
        return self.sff_threads

    def set_my_sff_name(self, sff_name):
        self.my_sff_name = sff_name

    def get_my_sff_name(self):
        return self.my_sff_name

    def set_sff_os(self, new_sff_os):
        self.sff_os = new_sff_os

    def get_sff_os(self):
        return self.sff_os

    def get_data_plane_control_port(self):
        return self.data_plane_control_port

    def set_data_plane_control_port(self, control_port):
        self.data_plane_control_port = control_port

    def get_data_plane_path(self):
        return self.data_plane_path

    def reset_data_plane_path(self):
        self.data_plane_path = {}

    def get_odl_locator(self):
        return self.odl_locator

    def set_odl_locator(self, odl_locator):
        self.odl_locator = odl_locator

    def get_odl_credentials(self):
        return self.odl_credentials

    def set_odl_credentials(self, odl_credentials):
        self.odl_credentials = odl_credentials


sfc_globals = SfcGlobals()
