#! /usr/bin/env python
#
# Copyright (c) 2015 Qosmos.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import logging
from . import Classifiers, Classifier

__author__ = "Christophe Fontaine"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "0.1"
__email__ = "christophe.fontaine@qosmos.com"

logger = logging.getLogger('l7classifier')


class FTPClassifier(Classifier):
    def __init__(self):
        super().__init__()
        self.name = 'FTP'
        self.selectorId = 2
        self.protocolPath = 1

    def classify(self, packetList):

        return False

Classifiers.register(FTPClassifier())
