#
# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


from ..common.sfc_globals import sfc_globals
import logging

logger = logging.getLogger(__name__)

try:
    import paramiko
except ImportError:
    logger.error("Could not import paramiko module")


__author__ = "Jim Guichard"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "jguichar@cisco.com"
__status__ = "alpha"

"""
XR CLI processing module
"""

# hard code dictionary of service nodes with type and index
service_node = {'10.0.20.241': {'node_type': 'vxlan-gpe-nsh', 'index': 20},
                '10.0.21.233': {'node_type': 'vxlan-gpe-nsh', 'index': 21}}


def process_rendered_service_path(rsp):
    """ """
    # create list to hold service_hop entries extracted from SFP
    service_hops = []

    for key in rsp:
        # extract and store each IP address in the RSP
        ip_address = rsp[key]['ip']
        # append each service node and index in the RSP to the service hop list
        service_hops.append(
            'service-index ' + str(key) + ' service-node ' + str(service_node[str(ip_address)]['node_type'])
            + ' ' + str(service_node[ip_address]['index']))

    # compute ending service index value for cli
    ending_service_index = max(rsp) - len(service_hops)

    return service_hops, ending_service_index


def send_command_and_wait_for_execution(channel, command, wait_string,
                                        should_print):
    """ """
    # Send the su command
    channel.send(command)

    # Create a new receive buffer
    receive_buffer = ''

    while wait_string not in receive_buffer:
        # Flush the receive buffer
        receive_buffer = str(channel.recv(10000))

    # Print the receive buffer, if necessary
    if should_print:
        print(receive_buffer)

    return


def enable_router(sshChannel):
    """ """
    # make sure in enable mode so we can configure the router
    send_command_and_wait_for_execution(sshChannel, "enable\n", "Password:",
                                        False)
    send_command_and_wait_for_execution(sshChannel, "cisco\n", "#", False)
    send_command_and_wait_for_execution(sshChannel, "terminal length 0\n", "#",
                                        False)


def ssh_execute_cli(rsp_service_hops, ending_si_index, spi, sff_locator):
    """ """
    remoteConnectionSetup = paramiko.SSHClient()
    remoteConnectionSetup.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    remoteConnectionSetup.connect(sff_locator,
                                  username='cisco', password='cisco',
                                  allow_agent=False, look_for_keys=False)
    # invoke the shell so can send multiple commands
    sshChannel = remoteConnectionSetup.invoke_shell()

    # make sure in enable mode so we can configure the router
    return_buffer = sshChannel.recv(1000)
    is_enabled = return_buffer.decode("utf-8")
    if "#" not in is_enabled:
        enable_router(sshChannel)

    # build the rendered service-path cli
    rsp_cli = 'service-chain service-path ' + str(spi)

    # execute the necessary commands to configure the router
    send_command_and_wait_for_execution(sshChannel, "conf t\n", "#", False)
    send_command_and_wait_for_execution(sshChannel, rsp_cli + '\n', "#", False)
    print('debug: sending ', rsp_cli)

    # determine how many service hops in the service path
    number_service_hops = len(rsp_service_hops)

    # build the rsp service-index configuration and push
    for _ in range(number_service_hops):
        cli_to_push = rsp_service_hops[number_service_hops - 1]
        send_command_and_wait_for_execution(sshChannel, cli_to_push + '\n', "#", False)
        print('debug: sending ', cli_to_push)
        number_service_hops -= 1

    # complete the service-path cli and push it
    # once work on metadata will do a check to see if metadata present - following assumes no metadata
    ending_rsp_cli = 'service-index ' + str(
        ending_si_index) + ' terminate service-node output 200 lookup metadata-location 1'
    send_command_and_wait_for_execution(sshChannel, ending_rsp_cli + '\n', "#", False)
    send_command_and_wait_for_execution(sshChannel, "exit\n", "#", False)
    print('debug: sending ', ending_rsp_cli)

    # Process policy-map configuration
    policy_map_cli = 'policy-map type pbr nsh' + str(spi)
    send_command_and_wait_for_execution(sshChannel, policy_map_cli + '\n', "#", False)
    send_command_and_wait_for_execution(sshChannel, "class type traffic nsh\n", "#", False)
    ending_si_index += len(rsp_service_hops)
    ending_policy_cli = 'redirect service-path ' + str(spi) + ' service-index ' + str(ending_si_index) + ' metadata 1'
    send_command_and_wait_for_execution(sshChannel, ending_policy_cli + '\n', "#", False)
    send_command_and_wait_for_execution(sshChannel, "exit\n", "#", False)
    send_command_and_wait_for_execution(sshChannel, "exit\n", "#", False)

    # Process interface configuration
    send_command_and_wait_for_execution(sshChannel, "interface GigabitEthernet0/0/1/0\n", "#", False)
    send_command_and_wait_for_execution(sshChannel, "no service-policy type pbr input\n", "#", False)
    interface_cli = 'service-policy type pbr input nsh' + str(spi)
    send_command_and_wait_for_execution(sshChannel, interface_cli + '\n', "#", False)

    # commit the configuration
    send_command_and_wait_for_execution(sshChannel, "commit\n", "#", False)  # commit the configuration
    remoteConnectionSetup.close()  # close the connection


def process_xr_cli(data_plane_path):
    """ """
    print('\nXR module received data plane path: \n', data_plane_path)
    local_sff_topo = sfc_globals.get_sff_topo()
    local_my_sff_name = sfc_globals.get_my_sff_name()

    sff_locator = (local_sff_topo[local_my_sff_name]['sff-data-plane-locator']
                   [0]
                   ['data-plane-locator']
                   ['ip'])

    for key in data_plane_path:
        # store the SPI value
        spi = key
        # store the rendered service path
        rsp = data_plane_path[key]
        # process the Rendered Service Path cli
        rsp_service_hops, ending_si_index = process_rendered_service_path(rsp)

        # dummy locator for testing purposes - needs to be removed
        # locator = '172.29.66.230'

        # send cli to configure XR router
        # ssh_execute_cli(rsp_cli_to_push, sff_locator)

        ssh_execute_cli(rsp_service_hops, ending_si_index, spi, sff_locator)  # using dummy locator for testing

    return
