# Copyright (c) 2015 Qosmos and others. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
import logging

__author__ = 'Christophe Fontaine'
__email__ = "christophe.fontaine@qosmos.com"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "0.1"
__status__ = "alpha"


logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


class Singleton(type):
    instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls.instances:
            singleton_cls = super(Singleton, cls).__call__(*args, **kwargs)
            cls.instances[cls] = singleton_cls

        return cls.instances[cls]


class Classifier():
    def __init__(self):
        return

    def process_acl(self, acl_data):
        raise Exception('Not Supported')

    def remove_rsp(self, rsp_name):
        raise Exception('Not Supported')

    def start_classifier(self):
        raise Exception('Not Supported')

    def clear_classifier(self):
        return

    def is_running(self):
        return False
