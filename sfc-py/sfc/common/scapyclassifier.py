# Copyright (c) 2015 Qosmos and others. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
import json
import logging
import requests
import threading
from ..common.sfc_globals import sfc_globals

from .classifier import Classifier
from scapyclassifier.scapyClassifierL7 import startClassifier
from scapyclassifier.actions import ActionManager, Matcher, Actions
from scapyclassifier.classifiers import classifiers


__author__ = 'Christophe Fontaine'
__email__ = "christophe.fontaine@qosmos.com"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "0.1"
__status__ = "alpha"

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


class ScapyClassifier(Classifier):
    def __init__(self):
        self._classThread = None
        self._am = ActionManager()
        self._running = False
        self.aces = dict()

    def _process_ace(self, ace_data):
        if 'delete' in ace_data:
            logger.info("DELETE")
            return
        logger.info("")
        logger.info("rule-name: " + ace_data['rule-name'])
        rsp_name = ace_data['actions']['service-function-acl:rendered-service-path']
        matcher = Matcher.Build(ace_data)
        logger.info("----------")
        return (matcher, rsp_name)

    def process_acl(self, acl_data):
        logger.debug(acl_data)
        for acl in acl_data['access-list']:
            logger.debug(acl['acl-name'])
            for ace in acl['access-list-entries']:
                (matcher, rsp_name) = self._process_ace(ace)
                action = None
                reversepathaction = None
                try:
                    rsp = self._fetch_rsp_first_hop_from_odl(rsp_name)
                    logger.rsp(rsp)
                    action = Actions()
                    action.nsp = rsp['path-id']
                    action.nsi = rsp['starting-index']
                except:
                    logger.warn("RSP %s does not exists", rsp_name)
                try:
                    rsp = self._fetch_rsp_first_hop_from_odl(rsp_name + '-Reverse')
                    logger.debug(rsp)
                    reversepathaction = Actions()
                    reversepathaction.nsp = rsp['path-id']
                    reversepathaction.nsi = rsp['starting-index']
                except:
                    logger.debug("RSP %s does not exists ", rsp_name + '-Reverse')

                if action is not None:
                    self._am.addMatcher(matcher, action, reversepathaction)

    def remove_rsp(self, rsp_name):
        logger.info('remove_rsp' + rsp_name)

    def start_classifier(self):
        self._running = True
        self._classifThread = threading.Thread(target=startClassifier,
                                               name='scapyClassifier',
                                               args=(None, self._am),
                                               daemon = False)
        self._classifThread.start()

    def clear_classifier(self):
        self._running = False

    def is_running(self):
        return self._running

    def _fetch_rsp_first_hop_from_odl(self, rsp_name):
        """
        Fetch RSP forwarding parameters (SFF locator) from ODL

        :param rsp_name: RSP name
        :type rsp_name: str

        :return dict

        """
        odl_locator = sfc_globals.get_odl_locator()
        url = ('http://{odl}/restconf/operations/rendered-service-path:'
               'read-rendered-service-path-first-hop'.format(odl=odl_locator))
        data = {'input': {'name': rsp_name}}

        headers = {'content-type': 'application/json'}

        try:
            logger.info('Requesting SFF for RSP "%s" from ODL', rsp_name)
            rsp_data = requests.post(url=url,
                                     timeout=5,
                                     headers=headers,
                                     data=json.dumps(data),
                                     auth=sfc_globals.get_odl_credentials())
        except requests.exceptions.Timeout:
            logger.error('Failed to get RSP "%s" from ODL: timeout', rsp_name)
            raise

        if not rsp_data.content:
            logger.error('RSP "%s" not found in ODL', rsp_name)
            raise

        rsp_json = rsp_data.json()
        if 'errors' in rsp_json:
            for errors in rsp_json['errors'].values():
                for e in errors:
                    logger.error('%s (%s)', e['error-message'], e['error-tag'])
            raise
        return rsp_json['output']['rendered-service-path-first-hop']

    def send_ipfixconf(self):
        url = ('http://' + sfc_globals.get_odl_locator() + '/restconf/config/'
               'ipfix-application-information:application-id-dictionary/')
        payload = classifiers.Classifiers.to_JSON()
        headers = {'content-type': 'application/json'}
        response = requests.put(url, data=payload, headers=headers,
                                auth=sfc_globals.get_odl_credentials())
        logger.info(response)
