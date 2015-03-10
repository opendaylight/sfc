# flake8: noqa
#
# Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import json
import socket
import logging
import requests
import ipaddress
import threading
import subprocess

from netfilterqueue import NetfilterQueue

from nsh.encode import build_packet
from common.sfc_globals import ODLIP, USERNAME, PASSWORD
from nsh.common import VXLANGPE, BASEHEADER, CONTEXTHEADER


__author__ = 'Martin Lauko, Dusan Madar'
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "martin.lauko@pantheon.sk, madar.dusan@gmail.com"
__status__ = "alpha"


"""
NFQ classifier - manage everything related with NetFilterQueue: from starting
packet listeners to creation of appropriate ip(6)tables rules and marking
received packets accordingly.
"""


# NOTE: naming conventions
# nfq -> NetFilterQueue
# nsh -> Network Service Headers
# fwd -> forwarder/forwarding
# acl -> access list
# ace -> access list entry
# `rule(s)` and `chain(s)` can be used interchangeably in ip(6)tables context


logger = logging.getLogger('classifier')
logger.setLevel(logging.DEBUG)

# silence `requests` module logging
requests_logger = logging.getLogger('requests')
requests_logger.setLevel(logging.WARNING)


#: constants
IPV4 = 4
IPv6 = 6
NFQ_NUMBER = 2


#: ACE items to ip(6)tables flags/types mapping
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


def run_iptables_cmd(arguments, ipv):
    """
    Execute ip(6)tables command with given arguments

    :param arguments: iptables arguments
    :type arguments: list
    :param ipv: IP version
    :type ipv: tuple

    """
    iptables = 'iptables'
    ip6tables = 'ip6tables'

    if (IPV4 in ipv) and (IPv6 in ipv):
        ip_tables = (iptables, ip6tables)
    elif IPV4 in ipv:
        ip_tables = (iptables,)
    elif IPv6 in ipv:
        ip_tables = (ip6tables,)
    else:
        raise ValueError('Unknown IP address version "%s"', ipv)

    for iptables_cmd in ip_tables:
        base_iptables_cmd = [iptables_cmd, '-t', 'raw']
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
        - RSP(s) already exists when an ACL referencing it is obtained

        How it works:
        1. sfc_agent receives an ACL and passes it for processing
        2. the RSP (its SFF locator) referenced by ACL is requested from ODL
        3. if the RSP exists in the ODL iptables rules for it are applied

        After this process is over, every packet successfully matched to an
        iptables rule (i.e. successfully classified) will be NSH encapsulated
        and forwarded to a related SFF, which knows how to traverse the RSP.

        Rules are created using appropriate iptables command. If the ACE rule
        is MAC address related both iptables and ip6tabeles rules re issued.
        If ACE rule is IPv4 address related, only iptables rules are issued,
        same for IPv6.

        ACL            RULES FOR
        ----------------------------------
        MAC            iptables, ip6tables
        IPv4           iptables
        IPv6           ip6tables

        Information regarding already registered RSP(s) are stored in an
        internal data-store, which is represented as a dictionary:

        {rsp_id: {'name': <rsp_name>,
                  'chains': {'chain_name': (<ipv>,),
                             ...
                             },
                  'sff': {'ip': <ip>,
                          'port': <port>,
                          'starting-index': <starting-index>,
                          'transport-type': <transport-type>
                          },
                  },
        ...
        }

        Where:
            - name: RSP name
            - chains: dict of iptables rules/chains related to the RSP
            - SFF:
                - ip: SFF IP
                - port: SFF port
                - starting-index: index given to packet at first RSP hop
                - transport-type:

        """
        # internal data-store
        self.rsp_2_sff = {}

        # set to `True` by collect_packets()
        self.nfq_running = False

        # NFQ for classified packets
        self.nfq = NetfilterQueue()

        # socket used to forward NSH encapsulated packets
        self.fwd_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

        # identifiers of the currently processed RSP, set by process_acl()
        # these will be different for each processed ACL/ACE
        self.rsp_id = None
        self.rsp_acl = None
        self.rsp_ace = None
        self.rsp_chain = None

        # IP version of the currently processed RSP, set by parse_ace()
        # this attribute serves as run_iptables_cmd() 'ipv' argument
        self.rsp_ipv = None

    def _get_current_ip_version(self, ip):
        """
        Get current IP address version

        :param ip: IP address
        :type ip: str

        :return int

        """
        if '/' in ip:
            ip_parts = ip.split('/')
            ip = ip_parts[0]

        ip = ipaddress.ip_address(ip)

        return ip.version

    def _get_rsp_by_name(self, rsp_name):
        """
        Retrieve RSP data from the data-store based on its name

        :param rsp_name: RSP name
        :type rsp_name: str

        :return tuple (rsp_id, rsp_data) or None

        """
        for rsp_id, rsp_data in self.rsp_2_sff.items():
            if rsp_name == rsp_data['name']:
                return rsp_id, rsp_data
        else:
            return None

    def _fetch_sff_from_odl(self, rsp_name):
        """
        Fetch RSPs' forwarding parameters (SFF locator) from ODL

        :param rsp_name: RSP name
        :type rsp_name: str

        :return dict or None

        """
        url = ('http://{odl}/restconf/operations/rendered-service-path:'
               'read-rendered-service-path-first-hop'.format(odl=ODLIP))

        data = {'input':
                    {'name': rsp_name}
                }

        headers = {'content-type': 'application/json'}

        try:
            logger.info('Requesting SFF for RSP "%s" from ODL', rsp_name)
            rsp_data = requests.post(url=url,
                                     timeout=5,
                                     headers=headers,
                                     data=json.dumps(data),
                                     auth=(USERNAME, PASSWORD))
        except requests.exceptions.Timeout:
            logger.exception('Failed to get RSP "%s" from ODL: timeout',
                             rsp_name)
            return

        if not rsp_data.content:
            logger.warning('RSP "%s" not found in ODL', rsp_name)
            return

        rsp_json = rsp_data.json()
        return rsp_json['output']['rendered-service-path-first-hop']

    def forward_packet(self, packet, rsp_id):
        """
        Encapsulate given packet with NSH and forward it to SFF related with
        currently matched RSP

        :param packet: packet to process
        :type packet: `:class:netfilterqueue.Packet`
        :param rsp_id: RSP identifier
        :type rsp_id: int

        """
        fwd_to = self.rsp_2_sff[rsp_id]['sff']

        # NOTES:
        # so far metadata are not supported -> just sending an empty ctx_header
        # tunnel_id (0x0500) is hard-coded, will it be always the same?
        ctx_header = CONTEXTHEADER(0, 0, 0, 0)
        vxlan_header = VXLANGPE(int('00000100', 2), 0, 0x894F, 0x0500, 64)
        base_header = BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1,
                                 rsp_id, fwd_to['starting-index'])

        nsh_encapsulation = build_packet(vxlan_header, base_header, ctx_header)
        nsh_packet = nsh_encapsulation + packet.get_payload()

        self.fwd_socket.sendto(nsh_packet, (fwd_to['ip'], fwd_to['port']))

    def process_packet(self, packet):
        """
        Main NFQ callback for each classified packet.
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

    def packet_collector(self):
        """
        Main NFQ related method. Configure the queue and wait for packets.

        NOTE: NetfilterQueue.run() blocs!

        """
        try:
            logger.info('Binding to NFQ queue number "%s"', NFQ_NUMBER)
            self.nfq.bind(NFQ_NUMBER, self.process_packet)
        except:
            msg = ('Failed to bind to the NFQ queue number "%s". '
                   'HINT: try to run command `sudo iptables -L` to check if '
                   'the required queue is available.' % NFQ_NUMBER)

            logger.exception(msg)
            raise

        try:
            logger.info('Starting NFQ - waiting for packets ...')
            self.nfq.run()
        except:
            logger.exception('Failed to start NFQ')
            raise

    def collect_packets(self):
        """
        Start a thread for classified packets collection
        """
        nfq_thread = threading.Thread(target=self.packet_collector)
        nfq_thread.daemon = True
        nfq_thread.start()

        self.nfq_running = True

    def parse_ace(self, ace_matches):
        """
        Parse given Access List Entries (ACE) matches and put together an
        iptables command representing the rule that should be applied.

        ACE matches is parsed item by item and the `ace_rule_cmd` list is
        extended in each step. Setting packets marking is the last step before
        returning.

        :param ace_matches: Access List Entries
        :type ace_matches: dict

        :return list

        """
        ace_rule_cmd = ['-I', self.rsp_chain]

        if 'ip-protocol' in ace_matches:
            protocols = ace_2_iptables['protocols']['type']
            protocol_flag = ace_2_iptables['protocols']['flag']

            for protocol in protocols:
                try:
                    protocol = protocols[ace_matches.pop('ip-protocol')]
                    ace_rule_cmd.extend([protocol_flag, protocol])
                    break

                except KeyError:
                    logger.warning('Unknown ip-protocol "%s"', protocol)

        src_ips = ace_2_iptables['source-ips']['type']
        for src_ip in src_ips:
            if src_ip in ace_matches:
                src_ip_flag = ace_2_iptables['source-ips']['flag']
                src_ip = ace_matches.pop(src_ip)
                self.rsp_ipv = (self._get_current_ip_version(src_ip),)

                ace_rule_cmd.extend([src_ip_flag, src_ip])
                break

        dst_ips = ace_2_iptables['destination-ips']['type']
        for dst_ip in dst_ips:
            if dst_ip in ace_matches:
                dst_ip_flag = ace_2_iptables['destination-ips']['flag']
                dst_ip = ace_matches.pop(dst_ip)
                self.rsp_ipv = (self._get_current_ip_version(dst_ip),)

                ace_rule_cmd.extend([dst_ip_flag, dst_ip])
                break

        ports = ace_2_iptables['ports']
        for port_range in ports:
            if port_range in ace_matches:
                port_flag = ports[port_range]

                port_range = ace_matches.pop(port_range)
                upper = str(port_range['upper-port'])
                lower = str(port_range['lower-port'])

                if upper == lower:
                    port = upper
                else:
                    port = '%s:%s' % (lower, upper)

                ace_rule_cmd.extend([port_flag, port])

        source_mac = 'source-mac-address'
        if source_mac in ace_matches:
            ace_rule_cmd.extend(['-m', 'mac', '--mac-source'])
            ace_rule_cmd.append(ace_matches[source_mac])
            self.rsp_ipv = (IPV4, IPv6)

        ace_rule_cmd.extend(['-j', 'MARK', '--set-mark', self.rsp_id])
        return ace_rule_cmd

    def process_acl(self, acl_data):
        """
        Parse ACL data and create/remove ip(6)tables rules accordingly.

        To be able to create/remove an ip(6)tables rule/chain these attributes
        must be set (i.e. not None):
        self.rsp_chain, self.rsp_ipv + self.rsp_id for creating a rule/chain

        :param acl_data: ACL
        :type acl_data: dict

        """
        for acl in acl_data['access-list']:
            self.rsp_acl = acl['acl-name'].upper()

            for ace in acl['access-list-entries']:
                if 'delete' in ace:
                    self.remove_acl_rsps()
                    return

                rsp_name = (ace['actions']
                               ['service-function-acl:rendered-service-path'])

                sff_data = self._fetch_sff_from_odl(rsp_name)
                if sff_data is None:
                    continue

                # NOTE: assuming that RSP IDs are unique
                rsp_id = sff_data['path-id']
                if rsp_id in self.rsp_2_sff:
                    logger.warning('RSP "%s" already exists', rsp_id)
                    continue

                self.rsp_id = rsp_id
                self.rsp_ace = ace['rule-name'].upper()
                self.rsp_chain = '-'.join((self.rsp_acl,
                                           self.rsp_ace,
                                           'RSP',
                                           str(rsp_id)))

                # `self.rsp_ipv` is set by this
                ace_rule_cmd = self.parse_ace(ace['matches'])

                self.create_rsp(rsp_name, sff_data)
                run_iptables_cmd(ace_rule_cmd, self.rsp_ipv)

    def register_rsp(self):
        """
        Create iptables rules for the current ACL -> ACE -> RSP

        In other words: create an iptables chain for the given RSP, direct all
        incoming packets through this chain and send matched packets (matching
        is mark based) to the NetfilterQueue.
        """
        logger.debug('Creating iptables rule for ACL "%s", ACE "%s", RSP "%s"',
                     self.rsp_acl, self.rsp_ace, self.rsp_id)

        # create [-N] new chain for the RSP
        run_iptables_cmd(['-N', self.rsp_chain],
                         self.rsp_ipv)

        # insert [-I] a jump to the created chain
        run_iptables_cmd(['-I', 'PREROUTING',
                          '-j', self.rsp_chain],
                         self.rsp_ipv)

        # append [-A] a redirection of matched packets to the NetfilterQueue
        run_iptables_cmd(['-A', self.rsp_chain,
                          '-m', 'mark', '--mark', self.rsp_id,
                          '-j', 'NFQUEUE', '--queue-num', NFQ_NUMBER],
                         self.rsp_ipv)

    def create_rsp(self, rsp_name, sff_data):
        """
        Create iptables rules for the current RSP and add it to the data-store

        :param rsp_name: RSP name
        :type rsp_name: str
        :param sff_data: RSPs' SFF description
        :type sff_data: dict

        """
        self.register_rsp()

        if self.rsp_id not in self.rsp_2_sff:
            self.rsp_2_sff[self.rsp_id] = {'name': rsp_name}

        if 'sff' not in self.rsp_2_sff[self.rsp_id]:
            sff_data.pop('path-id')
            self.rsp_2_sff[self.rsp_id]['sff'] = sff_data

        if 'chains' not in self.rsp_2_sff[self.rsp_id]:
            self.rsp_2_sff[self.rsp_id]['chains'] = {}

        self.rsp_2_sff[self.rsp_id]['chains'][self.rsp_chain] = self.rsp_ipv

    def unregister_rsp(self):
        """
        Remove iptables rules for the current RSP
        """
        # delete [-D] the jump to the chain
        run_iptables_cmd(['-D', 'PREROUTING',
                          '-j', self.rsp_chain],
                         self.rsp_ipv)

        # flush [-F] the chain
        run_iptables_cmd(['-F', self.rsp_chain],
                         self.rsp_ipv)

        # delete [-X] chain
        run_iptables_cmd(['-X', self.rsp_chain],
                         self.rsp_ipv)

    def remove_rsp(self, rsp_name):
        """
        Remove ip(6)tables rules/chains for a given RSP and remove it from the
        data-store as well; state (return) if the removal was succesfull.

        :param rsp_name: RSP name
        :type rsp_name: str

        :return bool

        """
        try:
            rsp_id, rsp_data = self._get_rsp_by_name(rsp_name)
        except TypeError:
            return False

        logger.debug('Removing iptables rules for RSP "%s"', rsp_id)

        for chain_name, ipv in rsp_data['chains'].items():
            self.rsp_chain = chain_name
            self.rsp_ipv = ipv

            self.unregister_rsp()

        del self.rsp_2_sff[rsp_id]
        return True

    def remove_acl_rsps(self):
        """
        Remove ip(6)tables rules/chains related to the current ACL
        """
        rsps_to_remove = []

        for rsp_id in self.rsp_2_sff:
            rsp = self.rsp_2_sff[rsp_id]
            rsp_chains = rsp['chains']

            chains_to_remove = [chain_name for chain_name in rsp_chains.keys()
                                if self.rsp_acl in chain_name]

            if not chains_to_remove:
                continue

            logger.debug('Removing iptables rule for ACL "%s"', self.rsp_acl)

            for chain_name in chains_to_remove:
                self.rsp_chain = chain_name
                self.rsp_ipv = rsp_chains.pop(chain_name)

                self.unregister_rsp()

            if not rsp_chains:
                rsps_to_remove.append(rsp_id)

        for rsp_id in rsps_to_remove:
            del self.rsp_2_sff[rsp_id]

    def remove_all_rsps(self):
        """
        Remove iptables rules for ALL registered RSPs and clear the data-store
        """
        if not self.rsp_2_sff:
            return

        logger.debug('Removing iptables rule(s) for ALL RSPs')

        for rsp_id in self.rsp_2_sff:
            rsp = self.rsp_2_sff[rsp_id]

            for chain, ipv in rsp['chains'].items():
                self.rsp_ipv = ipv
                self.rsp_chain = chain

                self.unregister_rsp()

        self.rsp_2_sff = {}


def start_classifier():
    """
    Start NFQ classifier
    """
    nfq_classifier = NfqClassifier()

    nfq_classifier.collect_packets()


def clear_classifier():
    """
    Clear all created ip(6)tables rules (if any), unbind from NFQ
    """
    nfq_classifier = NfqClassifier()

    nfq_classifier.remove_all_rsps()
    nfq_classifier.nfq.unbind()
