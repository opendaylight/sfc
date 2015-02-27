import logging
import threading
import subprocess

from queue import Queue
from netfilterqueue import NetfilterQueue

from common.sfc_globals import sfc_globals


logger = logging.getLogger('classifier')
logger.setLevel(logging.INFO)


#: constnats
NFQ = 'NFQUEUE'
NFQ_QUEUE_NUMBER = 2
NFQ_CHAIN_PREFIX = 'nfq-rsp-'
#: we are using the 'raw' table as it's used mainly for configuring exemptions
RAW_TABLE = 'raw'


# NOTE: naming conventions
# fwd -> forwarder/forwarding
# tun -> tunnel
# sp -> Service Path
# spi -> Service Path Id


def run_cmd(cmd):
    """
    Execute a BASH command

    :param cmd: command to be executed
    :type cmd: list

    """
    logger.debug('Executing command: %s', cmd)
    subprocess.check_call(cmd)


def run_cmd_as_root(cmd):
    """
    Execute a BASH command with root privileges

    :param cmd: command to be executed
    :type cmd: list

    """
    cmd.insert(0, 'sudo')
    run_cmd(cmd)


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

        NOTE:
        - ACLs and NFQ packets are managed in separate threads

        How it works [WORK STILL IN PROGESS]:
        1. sfc_agent passes ACLs
        2. collected ACLs are crunched and then forwarded for main processing
        3. iptables rules are created based on the ACL data
        4. ... to be continued

        """
        # a simple RSP data-store
        # {rsp_id: {'ip': <ip>,
        #           'port': <port>}
        # ...
        # }
        self.rsp_2_sff = {}

        self.acl_queue = Queue()
        self.nfq_queue = NetfilterQueue()

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

            # TODO: remove me
            logger.debug('NFQ received a %s, mark=%d', packet, rsp_id)

            if rsp_id in self.rsp_2_sff:
                # TODO: process packet as it is already know where to send it
                pass
            else:
                logger.warning('Dropping packet as it did\'t match any rule')
                packet.drop()

        except:
            logger.exception('NFQ failed to receive a packet')

    def preprocess_acl(self, acl_data):
        """
        Preprocess ACL data - append RSP, tunnel and forwarding information.
        Feed the acl_queue with completed data.

        :param acl_data: ACL
        :type acl_data: dict

        """
        acl_data = acl_data['access-list'][0]
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
        acl_data['tunnel-params'] = {'nsp': path_id,
                                     'starting-index': starting_index}
        acl_data['forwarding-params'] = {'ip': fwd_params['ip'],
                                         'port': fwd_params['port']}

        self.acl_queue.put(acl_data)

    def process_acl(self, acl_data):
        """
        Parse ACL data and create iptables rules accordingly

        :param acl_data:
        :type acl_data: dict

        """
        self._describe_rsp(acl_data['path-id'])

        if 'delete' in acl_data:
            self.remove_rsp()
            return

        ip = acl_data['forwarding-params']['ip']
        port = acl_data['forwarding-params']['port']
        self.create_rsp(ip, port)

        # TODO: apply rules from ACL

    def register_rsp(self):
        """
        Create iptables rules for the current RSP

        In other words: create an iptables chain for the given RSP, direct all
        incoming packets through this chain and send matched packets (matching
        is mark based) to the NetfilterQueue.
        """
        logger.debug('Creating iptables rule for RSP: %s', self.current_rsp_id)

        # create [-N] new chain for the RSP
        run_cmd_as_root(['iptables', '-t', RAW_TABLE,
                         '-N', self.current_iptables_chain])

        # insert [-I] a jump to the created sub-chain
        run_cmd_as_root(['iptables', '-t', RAW_TABLE,
                         '-I', 'PREROUTING', self.current_iptables_chain])

        # append [-A] a redirection of matched packets to the NetfilterQueue
        run_cmd_as_root(['iptables', '-t', RAW_TABLE,
                         '-A', self.current_iptables_chain,
                         '-m', 'mark', '--mark', self.current_rsp_id,
                         '-j', NFQ, '--queue-num', NFQ_QUEUE_NUMBER])

    def unregister_rsp(self):
        """
        Remove iptables rules for the current RSP
        """
        logger.debug('Removing iptables rule for RSP: %s', self.current_rsp_id)

        # delete [-D] the jump to the chain
        run_cmd_as_root(['iptables', '-t', RAW_TABLE,
                         '-D', 'PREROUTING',
                         '-j', self.current_iptables_chain])

        # flush [-F] the chain
        run_cmd_as_root(['iptables', '-t', RAW_TABLE,
                         '-F', self.current_iptables_chain])

        # delete [-X] chain
        run_cmd_as_root(['iptables', '-t', RAW_TABLE,
                         '-X', self.current_iptables_chain])

    def unregister_all_rsps(self):
        """
        Remove iptables rules for ALL RSPs
        """
        logger.debug('Removing iptables rule(s) for ALL RSPs')

        # flush [-F] the 'raw' table
        run_cmd_as_root(['iptables', '-t', RAW_TABLE, '-F'])

        # delete [-X] ALL chains
        run_cmd_as_root(['iptables', '-t', RAW_TABLE, '-X'])

    def create_rsp(self, ip, port):
        """
        Create iptables rules for the current RSP and add it to the data-store

        :param ip: current RSPs' first SF IP address
        :type ip: str
        :param port: current RSPs' first SF port
        :type port: int

        """
        self.register_rsp()

        # TODO: careful - currently the map contains data about SF, not SFF
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
    Start both NFQ and ACL related threads
    """
    nfq_classifier = NfqClassifier()

    try:
        nfq_classifier.collect_acl()
        nfq_classifier.collect_packets()
    except KeyboardInterrupt:
        pass
    finally:
        nfq_classifier.remove_all_rsps()

if __name__ == '__main__':
    start_classifier()
