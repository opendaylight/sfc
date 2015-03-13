# flake8: noqa
# hint: to see the rules execute: 'sudo iptables -S -t raw'

import os
import sys

# fix PYTHONPATH
parent_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.sep.join(parent_dir.split(os.sep)[:-1])
sys.path.append(parent_dir)

import json
import socket
import logging
import threading
import subprocess

from netfilterqueue import NetfilterQueue

from nsh.encode import build_packet
from nsh.common import VXLANGPE, BASEHEADER, CONTEXTHEADER


if __name__ == '__main__':
    from logging_config import *


logger = logging.getLogger('sfc.nfq_class_server')

NFQ_NUMBER = 1
TUNNEL_ID = 0x0500  # TODO: add tunnel_id to sff yang model
SUDO = True

# global ref to manager
nfq_class_server_manager = None


def execute_cli(cli):
    """
    Common BASH command executor
    """
    if (SUDO):
        cli = "sudo " + cli
    logger.debug("execute_cli: %s", cli)
    subprocess.call([cli], shell=True)
    return


class NfqTunnelParamsTransformer:
    """
    Transforms tunnel params for packet forwarder
    """
    def transform_tunnel_params(self, tun_params):
        if 'context-metadata' in tun_params:
            ctx_metadata = tun_params['context-metadata']
            ctx_values = CONTEXTHEADER(ctx_metadata['context-header1'],
                                       ctx_metadata['context-header2'],
                                       ctx_metadata['context-header3'],
                                       ctx_metadata['context-header4'])
        else:
            ctx_values = CONTEXTHEADER(0, 0, 0, 0)  # empty default ctx

        # set path_id, starting-index to VXLAN+NSH template
        vxlan_values = VXLANGPE(int('00000100', 2), 0, 0x894F, TUNNEL_ID, 64)
        base_values = BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1,
                                 tun_params['nsp'],
                                 tun_params['starting-index'])

        return {"vxlan_values": vxlan_values,
                "base_values": base_values,
                "ctx_values": ctx_values}


class NfqClassifierServerManager:
    def __init__(self, nfq_number):
        """
        We use mark that will be equal to path_id
        """
        self.nfq_number = nfq_number
        self.nfqueue = NetfilterQueue()
        self.tun_params_transformer = NfqTunnelParamsTransformer()
        self.__reset()
        #return

    def __del__(self):
        """
        Wannabe destructor - does not work

        NetfilterQueue should destroy itself properly automatically
        """
        self.__clear_all_rules()
        #return

    def __reset(self):
        """
        Private reset
        """
        self.__clear_all_rules()
        #return

    def __clear_all_rules(self):
        """
        Delete all forwarder and iptables rules
        """
        logger.info("Clear_all_rules: Reset iptables rules.")

        # init map
        self.path_id_2_pfw_map = {}

        # clear all previous rules/sub-chains in 'raw' table
        cli = "iptables -t raw -F"
        execute_cli(cli)

        cli = "iptables -t raw -X"
        execute_cli(cli)
        #return

    # helper
    def get_sub_chain_name(self, path_id):
        return "sfp-nfq-" + str(path_id)

    def __common_process_packet(self, packet):
        """
        Main NFQ callback for received packets
        """
        try:
            logger.debug("common_process_packet: received packet=%s, mark=%d",
                         packet, packet.get_mark())

            mark = packet.get_mark()

            # check
            if mark in self.path_id_2_pfw_map:
                packet_forwarder = self.path_id_2_pfw_map[mark]
                packet_forwarder.process_packet(packet)
            else:
                logger.warn("common_process_packet: no packet forwarder for "
                            "mark=%d, dropping the packet", mark)
                packet.drop()

            #return
        except Exception as exc:
            logger.exception('common_process_packet exception: %s', exc)

    def bind_and_run(self):
        """
        Bind to queue and run listening loop
        """
        self.nfqueue.bind(self.nfq_number, self.__common_process_packet)

        logger.info("NFQ binded to queue number %d", self.nfq_number)

        self.nfqueue.run()
        #return

    def process_input(self, message_dict):
        """
        Apply new configuration
        """
        # input
        path_id = message_dict['path-id']
        acl = message_dict['acl']

        # check if 'delete' operation
        if acl == 'delete':
            self.__destroy_packet_forwarder(path_id)
            return

        # additional input
        fw_params = message_dict['forwarding-params']
        tun_params = message_dict['tunnel-params']

        # delete possible former forwarder
        if path_id in self.path_id_2_pfw_map:
            self.__destroy_packet_forwarder(path_id)
        # init new forwarder
        self.__init_new_packet_forwarder(path_id,
                                         fw_params,
        self.tun_params_transformer.transform_tunnel_params(tun_params))
        # create rules
        self.__compile_acl(acl, path_id)
        return

    def __compile_acl(self, acl_item, path_id):
        logger.debug("__compile_acl: acl_item=%s", acl_item)
        for ace in acl_item['access-list-entries']:
            self.__add_iptables_classification_rule(ace, path_id)
        return

    def __init_new_packet_forwarder(self, path_id, forwarding_params,
                                    tunnel_params):
        sub_chain_name = self.get_sub_chain_name(path_id)

        # create sub-chain for the path, this way we can in future easily
        # remove the rules for particular path
        cli = "iptables -t raw -N " + sub_chain_name
        execute_cli(cli)

        # insert jump to sub-chain
        cli = "iptables -t raw -I PREROUTING -j " + sub_chain_name
        execute_cli(cli)

        # add jump to queue 'nfq_number' in case of match mark (ACL matching
        # rules will have to be inserted before this one)
        cli = ("iptables -t raw -A " + sub_chain_name +
               " -m mark --mark " + str(path_id) +
               " -j NFQUEUE --queue-num " + str(self.nfq_number))
        execute_cli(cli)

        packet_forwarder = PacketForwarder(path_id)

        packet_forwarder.update_forwarding_params(forwarding_params)
        packet_forwarder.update_tunnel_params(tunnel_params)

        self.path_id_2_pfw_map[path_id] = packet_forwarder
        return

    def __destroy_packet_forwarder(self, path_id):
        """
        Destroy PacketForwader with iptables rules and chains
        """
        # check
        assert path_id

        if path_id in self.path_id_2_pfw_map:
            logger.debug("destroy_packet_forwarder: Removing classifier for "
                         "path_id=%d", path_id)

            del self.path_id_2_pfw_map[path_id]

            sub_chain_name = self.get_sub_chain_name(path_id)

            # -D - delete the jump to sub-chain
            cli = "iptables -t raw -D PREROUTING -j " + sub_chain_name
            execute_cli(cli)

            # delete rules in sub-chain
            cli = "iptables -t raw -F  " + sub_chain_name
            execute_cli(cli)

            # delete sub-chain
            cli = "iptables -t raw -X " + sub_chain_name
            execute_cli(cli)

            logger.info("destroy_packet_forwarder: Classifier for path_id=%d "
                        "removed", path_id)
        else:
            logger.debug("destroy_packet_forwarder: Classifier for path_id=%d "
                         "not found", path_id)

    def __add_iptables_classification_rule(self, ace, path_id):
        """
        Create iptables matches for sending packets to NFQ of given number
        """
        assert ace
        assert path_id

        ace_matches = ace['matches']

        # dl_src
        dl_src = ''
        if 'source-mac-address' in ace_matches:
            dl_src = '-m mac --mac-source' + ace_matches['source-mac-address']

            if 'source-mac-address-mask' in ace_matches:
                logger.warn('source-mac-address-mask match not implemented')

        # dl_dst
        dl_dst = ''
        if 'destination-mac-address' in ace_matches:
            logger.warn('destination-mac-address match not implemented')

        # nw_src/ipv6_src
        nw_src = ''
        if 'source-ipv4-address' in ace_matches:
            nw_src = ' -s ' + ace_matches['source-ipv4-address']

        ipv6_src = ''
        if 'source-ipv6-address' in ace_matches:
            # not sure about this
            ipv6_src = ' -s ' + ace_matches['source-ipv6-address']

        #nw_dst/ipv6_dst
        nw_dst = ''
        if 'destination-ipv4-address' in ace_matches:
            nw_dst = ' -d ' + ace_matches['destination-ipv4-address']

        ipv6_dst = ''
        if 'destination-ipv6-address' in ace_matches:
            # not sure about this
            ipv6_dst = ' -d ' + ace_matches['destination-ipv6-address']

        # nw_proto --- TCP/UDP ....
        nw_proto = ''
        if 'ip-protocol' in ace_matches:
            if ace_matches['ip-protocol'] == 7:
                nw_proto = ' -p tcp'
            elif ace_matches['ip-protocol'] == 17:
                nw_proto = ' -p udp'
            else:
                logger.warn('unknown ip-protocol=%d',
                            ace_matches['ip-protocol'])

        # only lower transport port dst/src supported !!!!
        tp_dst = ''
        if 'destination-port-range' in ace_matches:
            if 'lower-port' in ace_matches['destination-port-range']:
                if nw_proto == '':
                    logger.error("add_iptables_classification_rule: "
                                 "processing 'destination-port-range'. "
                                 "ip-protocol must be specified")
                    return

                port = str(ace_matches['destination-port-range']['lower-port'])
                tp_dst = ' --dport ' + port

        tp_src = ''
        if 'source-port-range' in ace_matches:
            if 'lower-port' in ace_matches['source-port-range']:
                if nw_proto == '':
                    logger.error("add_iptables_classification_rule: "
                                 "processing 'source-port-range'. "
                                 "ip-protocol must be specified")
                    return

                port = str(ace_matches['source-port-range']['lower-port'])
                tp_src = ' --sport ' + port

        sub_chain_name = self.get_sub_chain_name(path_id)

        # 'I' - insert this 'set mark' rule before the 'jump to queue' rule
        cli = "iptables -t raw -I " + sub_chain_name
        cli += nw_src + nw_dst + ipv6_src + ipv6_dst
        cli += nw_proto + tp_src + tp_dst
        cli += " -j MARK --set-mark " + str(path_id)

        execute_cli(cli)
        #return


class PacketForwarder:
    def __init__(self, path_id):
        self.path_id = path_id
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self._fw_params_set = False
        self._tun_params_set = False
        return

    def __str__(self):
        return "PacketForwarder: path_id=" + str(self.path_id)

    def update_forwarding_params(self, forwarding_params):
        if not forwarding_params:
            return
        self.sff_ip_addr = forwarding_params['ip']
        self.sff_port = forwarding_params['port']
        self._fw_params_set = True
        return

    def update_tunnel_params(self, tunnel_params):
        if not tunnel_params:
            return
        self.vxlan_values = tunnel_params['vxlan_values']
        self.base_values = tunnel_params['base_values']
        self.ctx_values = tunnel_params['ctx_values']
        self._tun_params_set = True
        return

    def process_packet(self, orig_packet):
        # check
        if not self._fw_params_set:
            logger.error('process_packet: Forwarding params not set for '
                         'path_id=%d', self.path_id)
            return

        if not self._tun_params_set:
            logger.error('process_packet: Tunnel params not set for '
                         'path_id=%d', self.path_id)
            return

        logger.debug('process_packet: Forwarding packet to %s:%d',
                     self.sff_ip_addr, self.sff_port)

        orig_payload = orig_packet.get_payload()
        vxlan_packet = build_packet(self.vxlan_values,
                                    self.base_values,
                                    self.ctx_values) + orig_payload

        self.socket.sendto(vxlan_packet, (self.sff_ip_addr, self.sff_port))
        # ! drop original packet
        orig_packet.drop()
        return


# global procedures
def start_nfq_class_server_manager():
    global nfq_class_server_manager

    if nfq_class_server_manager:
        logger.error('Nfq classifier already started!')
        return

    nfq_class_server_manager = NfqClassifierServerManager(NFQ_NUMBER)
    nfq_class_server_manager.bind_and_run()
    return


# starts nfq thread and listens on socket
def nfq_class_server_start():
    global nfq_class_server_manager

    logger.info('starting thread for NetfilterQueue.run()')
    t = threading.Thread(target=start_nfq_class_server_manager, args=())
    t.daemon = True
    t.start()

    # create and listen on stream socket
    logger.info('creating socket')

    s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    try:
        os.remove("/tmp/nfq-class.sock")
    except OSError:
        pass

    s.bind("/tmp/nfq-class.sock")
    # allow not root access
    execute_cli('chmod 777 /tmp/nfq-class.sock')

    logger.info('listening on socket')
    while True:
        s.listen(1)
        conn, _ = s.accept()

        message = ""
        message_dict = None
        try:
            # collect message
            while True:
                # buffer
                data = conn.recv(1024)
                if not data:
                    # end of stream
                    break

                message = message + data.decode()

            # convert received message
            logger.debug('socket received message: %s', message)
            message_dict = json.loads(message)
        except:
            logger.exception("exception while receiving data, message %s not "
                             "applied", message)
            break

        try:
            # apply message
            nfq_class_server_manager.process_input(message_dict)
        except:
            logger.exception("exception while applying message %s", message)
            break

    conn.close()
    #return


# launch main loop
if __name__ == '__main__':
    nfq_class_server_start()
