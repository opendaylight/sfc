# flake8: noqa
#
# Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import socket
import logging
import threading
import subprocess

from queue import Queue
from netfilterqueue import NetfilterQueue

from nsh.encode import build_packet
from common.sfc_globals import sfc_globals
from nsh.common import VXLANGPE, BASEHEADER, CONTEXTHEADER


__author__ = 'Martin Lauko, Dusan Madar'
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "martin.lauko@pantheon.sk, madar.dusan@gmail.com"
__status__ = "alpha"


"""
NFQ classifier - manage everything related with NetFilterQueue: from starting
ACL and packet listeners to creation of appropriate iptables rules and marking
received packets accordingly.
"""


# NOTE: naming conventions
# nfq -> NetFilterQueue
# fwd -> forwarder/forwarding
# tun -> tunnel
# sp -> Service Path
# spi -> Service Path Id
# acl -> access list
# ace -> access list entry

logger = logging.getLogger('classifier')
logger.setLevel(logging.DEBUG)


#: NFQ constants
NFQ = 'NFQUEUE'
NFQ_QUEUE_NUMBER = 2
NFQ_CHAIN_PREFIX = 'nfq-rsp-'


#: ACE items to iptables flags/types mapping
ace_2_iptables = {'source-ips': {'flag': '-s',
                                 'type': ('source-ipv4-address',
                                          'source-ipv6-address')
                                 },
                  'destination-ips': {'flag': '-d',
                                      'type': ('destination-ipv4-address',
                                               'destination-ipv6-address')
                                      },
                  'protocols': {'flag': '-p',
                                'type': {7: 'tcp',
                                         17: 'udp'}
                                },
                  'ports': {'source-port-range': '--sport',
                            'destination-port-range': '--dport'}
                  }


def run_cmd(cmd):
    """
    Execute a BASH command

    :param cmd: command to be executed
    :type cmd: list

    """
    cmd = [str(cmd_part) for cmd_part in cmd]

    try:
        logger.debug('Executing command: `%s`', ' '.join(cmd))

        subprocess.check_call(cmd)
    except subprocess.CalledProcessError:
        logger.exception('Command execution failed')


def run_cmd_as_root(cmd):
    """
    Execute a BASH command with root privileges

    :param cmd: command to be executed
    :type cmd: list

    """
    cmd.insert(0, 'sudo')
    run_cmd(cmd)


def run_iptables_cmd(arguments):
    """
    Execute iptables command with given arguments

    :param arguments: iptables arguments
    :type arguments: list

    """
    base_iptables_cmd = ['iptables', '-t', 'raw']
    base_iptables_cmd.extend(arguments)

    run_cmd_as_root(base_iptables_cmd)


class Singleton(type):
    instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls.instances:
            singleton_cls = super(Singleton, cls).__call__(*args, **kwargs)
            cls.instances[cls] = singleton_cls

        return cls.instances[cls]


class NfqClassifier(metaclass=Singleton):
    def __init__(self):
        """
        NFQ classifier

        ASSUMED:
        - RSP already exists when an ACL referencing it is obtained

        NOTE:
        - ACLs and NFQ packets are managed in separate threads

        Information regarding redirection to RSP(s) are stored as a simple RSP
        to SFF locator mapping (rsp_id -> sff_locator); which is represented as
        a dictionary:

        {rsp_id: {'ip': <ip>,
                  'port': <port>},
        ...
        }

        How it works:
        0. sfc_agent creates RSP(s) (SFs, SFFs and everything else required)
        1. sfc_agent receives an ACL(s) and passes it to the ACL collector
        2. ACL collector passes received ACL for processing
        3. ACL is preprocessed - RSP, tunnel and forwarding data are appended
        4. iptables rules for the RSP are applied based on the preprocessed ACL
        5. iptables rules for packets marking are applied based on ACE(s)

        After this process is over, every packet successfully matched to an
        iptables rule will be NSH encapsulated and traverses appropriate RSP.

        """
        self.rsp_2_sff = {}

        self.acl_queue = Queue()
        self.nfq_queue = NetfilterQueue()

        # socket used to forward NSH encapsulated packets
        self.fwd_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

        # identifiers of the currently processed RSP, set by process_acl()
        self.current_rsp_id = None
        self.current_iptables_chain = None

        # will be set after NFQ and ACL threads are started
        self.nfq_thread = None
        self.acl_thread = None

    def _describe_rsp(self, rsp_id):
        """
        Set self.current_rsp_id and self.current_iptables_chain attributes

        NOTE: called in self.process_acl()

        :param rsp_id: RSP identifier
        :type rsp_id: int

        """
        self.current_rsp_id = rsp_id
        self.current_iptables_chain = NFQ_CHAIN_PREFIX + str(rsp_id)

    def _process_packet(self, packet):
        """
        Main NFQ callback for each queued packet.
        Drop the packet if RSP is unknown, pass it for processing otherwise.

        :param packet: packet to process
        :type packet: `:class:netfilterqueue.Packet`

        """
        try:
            # packet mark contains a RSP identifier
            rsp_id = packet.get_mark()

            logger.debug('NFQ received a %s, marked "%d"', packet, rsp_id)

            if rsp_id in self.rsp_2_sff:
                self.forward_packet(packet, rsp_id)
            else:
                logger.warning('Dropping packet as it did\'t match any rule')
                packet.drop()

        except:
            logger.exception('NFQ failed to receive a packet')

    def forward_packet(self, packet, rsp_id):
        """
        TODO:

        :param packet: packet to process
        :type packet: `:class:netfilterqueue.Packet`
        :param rsp_id: RSP identifier
        :type rsp_id: int

        """
        # TODO: add contex metadata if present in tunnel params
        ctx_header = CONTEXTHEADER(0, 0, 0, 0)

        # TODO: tunnel_id (0x0500) is hard-coded
        vxlan_header = VXLANGPE(int('00000100', 2), 0, 0x894F, 0x0500, 64)
        # TODO: starting index (255) is hard-coded
        base_header = BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1,
                                 rsp_id, 255)

        payload = packet.get_payload()
        nsh_encapsulation = build_packet(vxlan_header, base_header, ctx_header)

        fwd_to = self.rsp_2_sff[rsp_id]
        nsh_packet = nsh_encapsulation + payload

        self.fwd_socket.sendto(nsh_packet, (fwd_to['ip'], fwd_to['port']))

    def preprocess_acl(self, acl_data):
        """
        Preprocess ACL data - append RSP, tunnel and forwarding information.
        Feed the acl_queue with completed data.

        :param acl_data: ACL
        :type acl_data: dict

        """
        for acl_data in acl_data['access-list']:
            rsp = (acl_data['access-list-entries']
                           [0]
                           ['actions']
                           ['service-function-acl:rendered-service-path'])

            all_rsps = sfc_globals.get_path()
            sff = (all_rsps[rsp]
                           ['rendered-service-path-hop']
                           [0]
                           ['service-function-forwarder'])

            all_sffs = sfc_globals.get_sff_topo()
            fwd_params = (all_sffs[sff]
                                  ['sff-data-plane-locator']
                                  [0]
                                  ['data-plane-locator'])

            path_id = all_rsps[rsp]['path-id']
            starting_index = all_rsps[rsp]['starting-index']

            acl_data['path-id'] = path_id

            # TODO: add `context-metadata` to tunnel params
            acl_data['tunnel-params'] = {'nsp': path_id,
                                         'starting-index': starting_index}

            acl_data['forwarding-params'] = {'ip': fwd_params['ip'],
                                             'port': fwd_params['port']}

            self.acl_queue.put(acl_data)

    def parse_ace(self, ace_data):
        """
        Parse Access List Entries (ACE) of a given ACL and put together an
        iptables command representing the rule(s) that should be applied.

        ACE is parsed item by item and the `ace_rule_cmd` list is extended in
        each step. Setting packets mark is the last step before returning.

        NOTE: order of the command arguments (and values) is essential!

        :param ace_data: access list entries
        :type ace_data: list (of dicts)

        :return list

        """
        ace_rule_cmd = ['-I', self.current_iptables_chain]

        for ace in ace_data:
            matches = ace['matches']

            if 'ip-protocol' in matches:
                protocols = ace_2_iptables['protocols']['type']
                protocol_flag = ace_2_iptables['protocols']['flag']

                for protocol in protocols:
                    try:
                        protocol = protocols[matches.pop('ip-protocol')]
                        ace_rule_cmd.extend([protocol_flag, protocol])
                        break

                    except KeyError:
                        logger.warning('Unknown ip-protocol "%s"', protocol)

            src_ips = ace_2_iptables['source-ips']['type']
            for src_ip in src_ips:
                if src_ip in matches:
                    src_ip_flag = ace_2_iptables['source-ips']['flag']
                    src_ip = matches.pop(src_ip)

                    ace_rule_cmd.extend([src_ip_flag, src_ip])
                    break

            dst_ips = ace_2_iptables['destination-ips']['type']
            for dst_ip in dst_ips:
                if dst_ip in matches:
                    dst_ip_flag = ace_2_iptables['destination-ips']['flag']
                    dst_ip = matches.pop(dst_ip)

                    ace_rule_cmd.extend([dst_ip_flag, dst_ip])
                    break

            ports = ace_2_iptables['ports']
            for port_range in ports:
                if port_range in matches:
                    port_flag = ports[port_range]

                    port_range = matches.pop(port_range)
                    upper = str(port_range['upper-port'])
                    lower = str(port_range['lower-port'])

                    port = '%s:%s' % (lower, upper)
                    if upper == lower:
                        port = upper

                    ace_rule_cmd.extend([port_flag, port])

            source_mac = 'source-mac-address'
            if source_mac in matches:
                ace_rule_cmd.extend(['-m', 'mac', '--mac-source'])
                ace_rule_cmd.append(matches[source_mac])

        ace_rule_cmd.extend(['-j', 'MARK', '--set-mark', self.current_rsp_id])
        return ace_rule_cmd

    def process_acl(self, acl_data):
        """
        Parse ACL data and create iptables rules accordingly

        :param acl_data: ACL
        :type acl_data: dict

        """
        self._describe_rsp(acl_data['path-id'])

        if 'delete' in acl_data:
            self.remove_rsp()
            return

        ip = acl_data['forwarding-params']['ip']
        port = acl_data['forwarding-params']['port']
        self.create_rsp(ip, port)

        rule_cmd_items = self.parse_ace(acl_data['access-list-entries'])
        run_iptables_cmd(rule_cmd_items)

    def register_rsp(self):
        """
        Create iptables rules for the current RSP

        In other words: create an iptables chain for the given RSP, direct all
        incoming packets through this chain and send matched packets (matching
        is mark based) to the NetfilterQueue.
        """
        logger.debug('Creating iptables rule for RSP: %s', self.current_rsp_id)

        # create [-N] new chain for the RSP
        run_iptables_cmd(['-N', self.current_iptables_chain])

        # insert [-I] a jump to the created chain
        run_iptables_cmd(['-I', 'PREROUTING',
                          '-j', self.current_iptables_chain])

        # append [-A] a redirection of matched packets to the NetfilterQueue
        run_iptables_cmd(['-A', self.current_iptables_chain,
                          '-m', 'mark', '--mark', self.current_rsp_id,
                          '-j', NFQ, '--queue-num', NFQ_QUEUE_NUMBER])

    def unregister_rsp(self):
        """
        Remove iptables rules for the current RSP
        """
        logger.debug('Removing iptables rule for RSP: %s', self.current_rsp_id)

        # delete [-D] the jump to the chain
        run_iptables_cmd(['-D', 'PREROUTING',
                          '-j', self.current_iptables_chain])

        # flush [-F] the chain
        run_iptables_cmd(['-F', self.current_iptables_chain])

        # delete [-X] chain
        run_iptables_cmd(['-X', self.current_iptables_chain])

    def unregister_all_rsps(self):
        """
        Remove iptables rules for ALL RSPs
        """
        logger.debug('Removing iptables rule(s) for ALL RSPs')

        # flush [-F] the 'raw' table
        run_iptables_cmd(['-F'])

        # delete [-X] ALL chains
        run_iptables_cmd(['-X'])

    def create_rsp(self, ip, port):
        """
        Create iptables rules for the current RSP and add it to the data-store

        :param ip: current RSPs' SFF IP address
        :type ip: str
        :param port: current RSPs' SFF port
        :type port: int

        """
        self.register_rsp()

        self.rsp_2_sff[self.current_rsp_id] = {}
        self.rsp_2_sff[self.current_rsp_id]['ip'] = ip
        self.rsp_2_sff[self.current_rsp_id]['port'] = port

    def remove_rsp(self):
        """
        Remove iptables rules for the current RSP and remove it from the
        data-store
        """
        self.unregister_rsp()
        del self.rsp_2_sff[self.current_rsp_id]

    def remove_all_rsps(self):
        """
        Remove iptables rules for ALL RSPs and re-init the data-store
        """
        self.unregister_all_rsps()
        self.rsp_2_sff = {}

    def _packet_collector(self):
        """
        Main NFQ related method. Configure the queue and wait for packets.

        NOTE: NetfilterQueue.run() blocs!

        """
        try:
            logger.info('Binding to NFQ queue number "%s"', NFQ_QUEUE_NUMBER)
            self.nfq_queue.bind(NFQ_QUEUE_NUMBER, self._process_packet)
        except:
            msg = ('Failed to bind to the NFQ queue number "%s". '
                   'HINT: try to run command `sudo iptables -L` to check if '
                   'the required queue is available.' % NFQ_QUEUE_NUMBER)

            logger.exception(msg)
            raise

        try:
            logger.info('Starting NFQ - waiting for packets ...')
            self.nfq_queue.run()
        except:
            logger.exception('Failed to start NFQ')
            raise

    def collect_packets(self):
        """
        Start a thread for NFQ packets collection
        """
        nfq_thread = threading.Thread(target=self._packet_collector)
        nfq_thread.daemon = True
        nfq_thread.start()

        self.nfq_thread = nfq_thread

    def _acl_collector(self):
        """
        Wait for ACLs, forward collected ACL data for processing
        """
        while True:
            try:
                acl_data = self.acl_queue.get()
                if not acl_data:
                    continue
            except:
                logger.exception('Failed to collect ACL data')
                continue

            try:
                self.process_acl(acl_data)
            except:
                logger.exception('Failed to process ACL data')
                continue

    def collect_acl(self):
        """
        Start a thread for ACL collection
        """
        logger.info('Starting ACL collector')

        acl_thread = threading.Thread(target=self._acl_collector)
        acl_thread.daemon = True
        acl_thread.start()

        self.acl_thread = acl_thread


def start_classifier():
    """
    Start both NFQ and ACL collectors/listeners
    """
    nfq_classifier = NfqClassifier()

    nfq_classifier.collect_acl()
    nfq_classifier.collect_packets()


def clear_classifier():
    """
    Clear all created iptables rules (if any)
    """
    nfq_classifier = NfqClassifier()

    nfq_classifier.remove_all_rsps()
