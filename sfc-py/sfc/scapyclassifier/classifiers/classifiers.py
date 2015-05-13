#! /usr/bin/env python
#
# Copyright (c) 2015 Qosmos.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
import logging
import json
from scapy.all import log_scapy

__author__ = "Christophe Fontaine"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "0.1"
__email__ = "christophe.fontaine@qosmos.com"

logger = logging.getLogger('l7classifier')
logger.addHandler(log_scapy)
logger.setLevel(10)


class Classifiers():
    _classifiers = []
    _childclassifiers = dict()

    @staticmethod
    def register(classifier):
        logger.info("Register classifier %s", classifier.name)
        Classifiers._classifiers.append(classifier)

    @staticmethod
    def register_child(classifier, child):
        logger.info("Register classifier %s-%s", classifier.name, child.name)
        if classifier.name not in Classifiers._childclassifiers:
            Classifiers._childclassifiers[classifier.name] = []
        Classifiers._childclassifiers[classifier.name].append(child)

    @staticmethod
    def to_JSON():
        encoder = ClassifierJSONEncoder()
        jsondict = dict()
        jsondict['application-id-dictionary'] = dict()
        jsondict['application-id-dictionary']['application-id'] = []
        for c in Classifiers._classifiers:
            jsondict['application-id-dictionary']['application-id'].append(encoder.default(c))
        for carray in Classifiers._childclassifiers.values():
            for c in carray:
                jsondict['application-id-dictionary']['application-id'].append(encoder.default(c))
        return json.dumps(jsondict)

    @staticmethod
    def GetClassifierFromName(appName):
        for c in Classifiers._classifiers:
            if c.name == appName:
                return c
        for carray in Classifiers._childclassifiers.values():
            for c in carray:
                if  c.name == appName:
                    return c
        return None

    @staticmethod
    def classify(pktlist, classifiers=_classifiers):
        classifier = None
        for c in classifiers:
            if c.classify(pktlist):
                classifier = c
                childClassifier = None
                if c.name in Classifiers._childclassifiers:
                    subclass = Classifiers._childclassifiers[c.name]
                    childClassifier = Classifiers.classify(pktlist, subclass)
                if childClassifier is not None:
                    classifier = childClassifier
        return classifier


class Classifier():
    def __init__(self):
        self.name = 'Template'
        self.group = ''
        self.category = ''
        self.subcategory = ''
        self.classId = 0
        self.selectorId = 0
        self.pen = 0

    def classify(self, packetList):
        return False

    def appId(self):
        return (self.classId << 24) + self.selectorId

    def to_JSON(self):
        return json.dumps(self, cls=ClassifierJSONEncoder)


class Unknown(Classifier):
    def __init__(self):
        super().__init__()
        self.name = 'Unknown'


class ClassifierJSONEncoder(json.JSONEncoder):
    def default(self, o):
        jsondict = dict()
        jsondict['applicationName'] = o.name
        jsondict['class-id'] = o.classId
        jsondict['selector-id'] = o.selectorId
        jsondict['pen'] = o.pen
        jsondict['applicationGroupName'] = o.group
        jsondict['applicationCategoryName'] = o.category
        jsondict['applicationSubCategoryName'] = o.subcategory
        return jsondict


