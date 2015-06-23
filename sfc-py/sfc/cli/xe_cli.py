#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "jguichar@cisco.com"
__status__ = "alpha"


"""
XE CLI processing module
"""


def process_received_service_path(spi, rsp):
    """ """
    # create list to hold service_hop entries extracted from SFP
    service_hops = []
    for key in rsp:
        # extract and store each IP address in the RSP
        ip_address = rsp[key]['ip']
        # append each IP address in the RSP to the service hop list
        service_hops.append('sff ' + ip_address)

    # reverse the order of the service_hops to get right XE syntax
    service_hops[::-1]

    # determine how many service hops in the service path
    number_service_hops = len(service_hops)
    # currently a limitation in IOS XE
    if number_service_hops <= 4:
        # determine how many 'nulls' to put into the XE cli syntax
        number_of_nulls = 4 - number_service_hops
        # start the cli syntax adding service path id
        xe_cli = 'service-insertion service-path ' + str(spi)

        # enter the necessary number of 'nulls' into the xe_cli variable
        for _ in range(number_of_nulls):
            xe_cli += ' null'

        for _ in range(number_service_hops):
            # enter service hop address into xe_cli variable
            xe_cli += ' ' + service_hops[number_service_hops - 1]
            # decrement the counter
            number_service_hops -= 1
        print('\nCLI to be entered is: \n', xe_cli)
    else:
        print('number of service hops ', number_service_hops,
              'unsupported in IOS XE')
        xe_cli = ''

    return xe_cli


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


def ssh_execute_cli(cli, sff_locator):
    """ """
    remoteConnectionSetup = paramiko.SSHClient()
    remoteConnectionSetup.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    remoteConnectionSetup.connect(sff_locator,
                                  username='cisco', password='cisco',
                                  allow_agent=False, look_for_keys=False)
    # invoke the shell so can send multiple commands
    sshChannel = remoteConnectionSetup.invoke_shell()

    # make sure in enable mode so we can configure the router
    is_enabled = sshChannel.recv(1000)
    if "#" not in is_enabled:
        enable_router(sshChannel)

    # execute the necessary commands to configure the router
    send_command_and_wait_for_execution(sshChannel, "conf t\n", "#", False)
    send_command_and_wait_for_execution(sshChannel, cli + '\n', "#", False)
    remoteConnectionSetup.close()  # close the connection


def process_xe_cli(data_plane_path):
    """ """
    print('\nXE module received data plane path: \n', data_plane_path)
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
        # process the cli
        cli_to_push = process_received_service_path(spi, rsp)
        # send cli to configure XE router
        ssh_execute_cli(cli_to_push, sff_locator)

    return

# {254: {'port': 6633, 'ip': '2.2.2.2'}, 255: {'port': 6633, 'ip': '10.1.1.1'}}
# service-insertion service-path 3 null null null sf 21.0.0.24
