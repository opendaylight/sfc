# hint: to see the rules execute: 'sudo iptables -S -t raw'

from netfilterqueue import NetfilterQueue
import logging
import socket
import threading
import subprocess
import json
from sff_globals import *
from sfc_agent import find_sff_locator
from nfq_class_server import nfq_class_server_start

logger = logging.getLogger('sfc.nfq_class_thread')

nfq_class_manager = None

def get_nfq_class_manager_ref():
    global nfq_class_manager
    return nfq_class_manager

# To DO - implement class like this like global helper class managing path infos
class NfqPathInfoSupplier:
    def __init__(self):
        self.path_name_2_id_map = {}
        self.path_id_2_info_map = {}

    def get_path_id(self, path_name):
        if path_name in self.path_name_2_id_map:
            return self.path_name_2_id_map[path_name]

        if self.__add_path_info(path_name):
            return self.get_path_id(path_name)  # call this one once more
        else:
            logger.warn('get_path_id: path not found (path_name=%s)', path_name)
            return None

    def delete_path_info(self, path_id):
        # remove data from maps for given path
        if path_id in self.path_id_2_info_map:
            path_item = self.path_id_2_info_map.pop(path_id)
            path_name = path_item['name']
            if path_name in self.path_name_2_id_map:
                self.path_name_2_id_map.pop(path_name)
                return True
        else:
            logger.debug('delete_path_info: path not found (path_id=%d)', path_id)

        return False

    # private  - returns True if path_item was found in global path data
    def __add_path_info(self, path_name):
        if not get_path():
            logger.warn('__add_path_info: No path data')
            return False

        if path_name in get_path():
            path_item = get_path()[path_name]
            path_id = path_item['path-id']
            self.path_name_2_id_map[path_name] = path_id
            self.path_id_2_info_map[path_id] = path_item
            return True

        return False

    # assuming info already added by requesting path_id before
    def get_forwarding_params(self, path_id):
        if path_id not in self.path_id_2_info_map:
            logger.warn('get_forwarding_params: path data not found for path_id=%d', path_id)
            return None

        path_item = self.path_id_2_info_map[path_id]

        # string ref for sff for first hop
        sff_name = path_item['rendered-service-path-hop'][0]['service-function-forwarder']

        sff_locator = find_sff_locator(sff_name)
        if not sff_locator:
            logger.warn('get_forwarding_params: sff data not found for sff_name=%s', sff_name)

        return sff_locator

    # assuming info already added by requesting path_id before
    def get_tunnel_params(self, path_id):
        if path_id not in self.path_id_2_info_map:
            logger.warn('get_tunnel_params: path data not found for path_id=%d', path_id)
            return None

        path_item = self.path_id_2_info_map[path_id]

        result = {}

        result['nsp'] = path_id
        result['starting-index'] = path_item['starting-index']

        if 'context-metadata' in path_item:
            result['context-metadata'] = path_item['context-metadata']

        return result


class NfqClassifierManager:
    # we use packet-mark that will be equal to path_id
    def __init__(self):
        self.path_info_supp = NfqPathInfoSupplier()
        return

    # compile_one_acl
    # !assumed! all aces in alc_item are for one and only path
    def compile_one_acl(self, acl_item):
        logger.debug("compile_one_acl: acl_item=%s", acl_item)

        collected_results = {} # add error info to this dictionary

        first_ace = acl_item['access-list-entries'][0]
        path_name = first_ace['actions']['service-function-acl:rendered-service-path']
        path_id = self.path_info_supp.get_path_id(path_name)

        if not path_id:
            logger.error("compile_one_acl: path_id not found for path_name=%s", path_name)
            collected_results[path_name] = 'Path data not found'
            return collected_results

        logger.debug("compile_one_acl: found path_id=%d", path_id)

        data = {
            'path-id': path_id,
            'acl': acl_item, # only entries for this path
            'forwarding-params': self.path_info_supp.get_forwarding_params(path_id), # ip, port
            'tunnel-params': self.path_info_supp.get_tunnel_params(path_id) # nsp, starting-index, context-metadata
        }

        jsonified_data = json.dumps(data)

        try:
            socka = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            socka.connect("/tmp/nfq-class.sock")
            logger.debug("compile_one_acl: sending to socket: %s", jsonified_data)
            socka.send(jsonified_data.encode())
            socka.close()
        except:
            collected_results[path_name] = 'Error sending data to nfq classifier server'
            logger.exception('data was not sent: exception:')

        return collected_results  # return info about unsuccess



def __start_nfq_classifier_separate_script():
    cli = "sudo python3 nfq_class_server.py"
    logger.info("start_nfq_classifier_separate_script cli: %s", cli)
    subprocess.call([cli], shell=True)
    return

# globals
def start_nfq_classifier(start_server_as_separate_script):

    logger.info('starting nfq classifier server')
    thread = None
    if not start_server_as_separate_script:
        thread = threading.Thread(target=nfq_class_server_start, args=())
    else:
        thread = threading.Thread(target=__start_nfq_classifier_separate_script, args=())
    thread.daemon = True
    thread.start()

    global nfq_class_manager

    if nfq_class_manager:
        logger.error('Nfq classifier already started!')
        return

    nfq_class_manager = NfqClassifierManager()
    return

