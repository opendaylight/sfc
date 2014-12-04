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
data_plane_path = {}

#Contains all SFFs known by the agent
sff_topo = {}

# Contains all Paths in JSON format as received from ODL
path = {}


# Global Accessors

def get_path():
    #global path
    return path

def get_sff_topo():
    #global sff_topo
    return sff_topo

def get_data_plane_path():
    #global data_plane_path
    return data_plane_path