__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "rapenno@gmail.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


# Contains all SFPs in a format easily consumable by the data plane when
# processing NSH packets. data_plane_path[sfp-id][sfp-index] will return the
# locator of the SF/SFF.

class SFC_Agent_Globals:
    my_sff_name = None
    sff_os_set = {"OVS", "XE"}
    sff_os = "ODL"

    def set_my_sff_name(self, sff_name):
        self.my_sff_name = sff_name

    def get_my_sff_name(self):
        return self.my_sff_name

    def set_sff_os(self, new_sff_os):
        self.sff_os = new_sff_os

    def get_sff_os(self):
        return self.sff_os

agent_globals = SFC_Agent_Globals()

def get_agent_globals():
    return agent_globals

data_plane_path = {}

# Contains all SFFs known by the agent
sff_topo = {}

# Contains all Paths as dictionary of path items by path_name as key
path = {}

# A dictionary of all SFF threads and its associated data this agent is aware.
sff_threads = {}

of_tableid = 0



# Global Accessors





def set_path(arg):
    global path
    path = arg



# GET APIs




def get_path():
    #global path
    return path


def get_sff_topo():
    #global sff_topo
    return sff_topo


def get_data_plane_path():
    #global data_plane_path
    return data_plane_path


def get_sff_threads():
    #global data_plane_path
    return sff_threads


def get_of_table_id():
    #global table ID for OF rules
    return of_tableid

#RESET APIs


def reset_sff_topo():
    global sff_topo
    sff_topo = {}


def reset_data_plane_path():
    global data_plane_path
    data_plane_path = {}


def reset_path():
    global path
    path = {}


# ODL IP:port
ODLIP = "localhost:8181"

# Static URLs for testing
SF_URL = "http://" + ODLIP + "/restconf/config/service-function:service-functions/"
SFC_URL = "http://" + ODLIP + "/restconf/config/service-function-chain:service-function-chains/"
SFF_URL = "http://" + ODLIP + "/restconf/config/service-function-forwarder:service-function-forwarders/"
SFT_URL = "http://" + ODLIP + "/restconf/config/service-function-type:service-function-types/"
SFP_URL = "http://" + ODLIP + "/restconf/config/service-function-path:service-function-paths/"

SFF_PARAMETER_URL = "http://{}/restconf/config/service-function-forwarder:service-function-forwarders/"

SFF_NAME_PARAMETER_URL = "http://{}/restconf/config/service-function-forwarder:service-function-forwarders/" + \
                         "service-function-forwarder/{}"
SFF_SF_DATA_PLANE_LOCATOR_URL = "http://{}/restconf/config/service-function-forwarder:service-function-forwarders/" + \
                                "service-function-forwarder/{}/service-function-dictionary/{}/" + \
                                "sff-sf-data-plane-locator/"

USERNAME = "admin"
PASSWORD = "admin"