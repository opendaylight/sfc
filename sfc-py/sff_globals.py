__author__ = 'repenno'


# Contains all SFPs in a format easily consumable by the data plane when
# processing NSH packets. data_plane_path[sfp-id][sfp-index] will return the
# locator of the SF/SFF.
data_plane_path = {}

def get_data_plane_path():
    #global data_plane_path
    return data_plane_path

sff_topo = {}


def get_sff_topo():
    #global sff_topo
    return sff_topo
