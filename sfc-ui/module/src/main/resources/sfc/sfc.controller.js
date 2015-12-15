var controllers = [
  'app/sfc/sfc.controller',
  'app/sfc/servicenode/servicenode.controller',
  'app/sfc/serviceforwarder/serviceforwarder.controller',
  'app/sfc/servicefunction/servicefunction.controller',
  'app/sfc/servicechain/servicechain.controller',
  'app/sfc/servicepath/servicepath.controller',
  'app/sfc/config/config.controller',
  'app/sfc/utils/modal.controller',
  'app/sfc/acl/acl.controller',
  'app/sfc/metadata/metadata.controller',
  'app/sfc/ipfix/ipfix.controller',
  'app/sfc/servicelocator/servicelocator.controller',
  'app/sfc/system/system.controller',
  'app/sfc/servicepath/renderedservicepath/renderedservicepath.controller'];
var services = [
  'app/core/core.services',
  'app/sfc/sfc.services',
  'app/sfc/utils/modal.services',
  'app/sfc/utils/datatemplate.services',
  'app/sfc/servicechain/servicechain.services',
  'app/sfc/servicenode/servicenode.services',
  'app/sfc/config/config.services',
  'app/sfc/config/schemas.services',
  'app/sfc/serviceforwarder/serviceforwarder.services',
  'app/sfc/servicefunction/servicefunction.services',
  'app/sfc/servicepath/servicepath.services',
  'app/sfc/servicelocator/servicelocator.services',
  'app/sfc/acl/acl.services',
  'app/sfc/system/system.services'];
var directives = [
  'app/sfc/sfc.directives',
  'app/sfc/servicenode/servicenode.directives',
  'app/sfc/config/config.directives',
  'app/sfc/servicelocator/servicelocator.directives',
  'app/sfc/serviceforwarder/serviceforwarder.directives',
  'app/sfc/acl/acl.directives',
  'app/sfc/metadata/metadata.directives',
  'app/sfc/ipfix/ipfix.directives',
  'app/sfc/servicepath/servicepath.directives',
  'app/sfc/utils/datatemplate.directives'
];
var modules = ['app/sfc/sfc.module'];

define([].concat(modules).concat(services).concat(directives).concat(controllers), function (sfc) {

  sfc.register.controller('rootSfcCtrl', ['$rootScope', '$scope', 'SfcRestangularSvc', '$sessionStorage', '$location', 'yangUtilsSfc', 'ENV', 'sfcYangParseSvc',
    function ($rootScope, $scope, SfcRestangularSvc, $sessionStorage, $location, yangUtils, ENV, sfcYangParseSvc) {

//    // register watch for debugging - works only in firefox
//    if (angular.isDefined($rootScope.watch)) {
//      $rootScope.watch('sfcs', function(id, oldval, newval) {
//        console.log('change!!!');
//
//        var innerfn = function(a, b, c) {
//          console.log('inner change!!!');
//          return c;
//        };
//
//        newval.watch('0', innerfn);
//        newval.watch('1', innerfn);
//        newval.watch('2', innerfn);
//
//        return newval;
//      });
//    }
    $rootScope['section_logo'] = '/src/app/sfc/assets/images/logo_sfc.gif';

    $rootScope.sfcState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};
    if (angular.isDefined(Object.freeze)) {
      Object.freeze($rootScope.sfcState);
    }

    $rootScope.sfpState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};
    if (angular.isDefined(Object.freeze)) {
      Object.freeze($rootScope.sfpState);
    }

    $rootScope.sfcs = [];
    $rootScope.sfps = [];
    $rootScope.sfpEffectMe = {};

    if (angular.isUndefined($rootScope.serviceFunctionConstants)){
      $rootScope.serviceFunctionConstants =
      {
        type: ["napt44", "dpi", "firewall", "qos", "ids"],
        failmode: ["open", "close"]
      };
    }

    if (angular.isUndefined($rootScope.serviceLocatorConstants)) {
      $rootScope.serviceLocatorConstants =
      {
        transport: ["vxlan-gpe", "gre", "other"],
        type: ["ip", "mac", "lisp", "function"],
        typeFormFields: {}
      };
    }

    $rootScope.aclConstants =
    {
      "ace-type": ["ip", "eth", "ipfix"],
      "ace-ip": ["IPv4", "IPv6"]
    };

    $rootScope.classifierConstants =
    {
      "attachment-point-type": ["bridge", "interface"]
    };

    $rootScope.logConstants =
    {
      "level": ['INFO', 'DEBUG', 'WARN', 'ERROR']
    };

    $rootScope.experimental = true;

    $rootScope.defaultTemplates =
    {
      sfDefault: {
        "sf-data-plane-locator": [{
          "service-function-forwarder": null,
          "name": "dp1",
          "ip": "10.0.0.1",
          "transport": "vxlan-gpe"
        }],
        "name": "SF1", "type": "firewall", "nsh-aware": "true"
      },
      sffDefault: {
        "sff-data-plane-locator": [{
          "data-plane-locator": {"ip": "10.0.0.1", "transport": "vxlan-gpe"},
          "ovs-bridge": {},
          "name": "eth0"
        }],
        "service-function-dictionary": [{
          "nonExistent": false,
          "sff-sf-data-plane-locator": {},
          "sff-interfaces": []
        }],
        "service-node": null, "name": "SFF1", "rest-uri": "http://www.example.com/sffs/sff-bootstrap"
      },
      classifierDefault: {
        "scl-service-function-forwarder": [{}],
        "name": "Classifier1"
      },
      aclDefault: {
        "access-list-entries": {
          "ace": [
            {
              "rule-name": "ACE1",
              "matches": {
                "absolute-time": {
                  "active": true
                },
                "protocol": 7,
                "source-ipv4-network": "11.11.11.0/24",
                "destination-ipv4-network": "22.22.22.0/24"
              }
            }
          ]
        },
        "acl-name": "ACL1"
      },
      contextMetadataDefault: [
        {
          "name": "ContextMetadata1",
          "context-header1": "0x1234",
          "context-header2": "0x1234",
          "context-header3": "0x1234",
          "context-header4": "0x1234"
        }
      ],
      variableMetadataDefault: {
        "tlv-metadata": [
          {"flags": {}, "tlv-class": "0x123", "tlv-type": "0x12", "tlv-data": "data", "length": "4"}
        ],
        "name": "VariableMetadata1"
      },
      classIdDefault: {
        "id": "255",
        "name": "SubscriberClass",
        "description": "Classification from a mapping IP Address to a class of user. This mapping is provided to the classifier independently."
      },
      appIdDefault : {
        "class-id": "255",
        "selector-id": "1",
        "pen": "0",
        "applicationName": "Gold",
        "applicationDescription": "Gold Subscriber Class with privileged access to the network."
      }
    };

    sfcYangParseSvc.init().then(function () {
      // loaded.resolve(true);
      console.info("sfcLoaderSvc:  completed");
    });

    $sessionStorage.$default({
      restangularBaseUrl:  ENV.getBaseURL("MD_SAL") + "/restconf"
    });
    SfcRestangularSvc.changeBaseUrl($sessionStorage.restangularBaseUrl);


    $scope.callBroadcastFromRoot = function(name, obj){
      $scope.$broadcast(name, obj);
    };
  }]);

  sfc.register.controller('sfcSelect2CreateSearchChoiceCtrl', function ($scope) {
    var thisCtrl = this;

    //wait for data load, then prefill select2Model($scope.tmpForSelect2) - do it only once
    this.unregisterBindingPropertyWatch = $scope.$watch('bindingProperty', function (newVal) {
      if (angular.isUndefined(newVal) || newVal === null) {
        return;
      }

      $scope.tmpForSelect2 = {
        id: newVal,
        text: newVal
      };

      thisCtrl.unregisterBindingPropertyWatch();
    });

    // sync/copy 'id' (id = selected value) to bindingProperty
    $scope.$watch('tmpForSelect2', function (newVal, oldVal) {
      if (newVal && newVal['id']) {
        $scope.bindingProperty = newVal['id'];
      }
      else {
        $scope.bindingProperty = null;
      }

      if (!_.isEqual(newVal, oldVal) && angular.isFunction($scope.ngChangeFunction)) {
        $scope.ngChangeFunction({selected: $scope.bindingProperty});
      }
    });

    $scope.select2Options = {
      allowClear: true,
      query: function (query) {
        var data = {results: []};
        var exact = false;
        var blank = _.isEmpty(query.term);

        _.each($scope.availableOptionsArray, function (option) {
          var name = option.name;
          var addThis = false;

          if (!blank) {
            if (query.term == name) {
              exact = true;
              addThis = true;
            } else if (name.toUpperCase().indexOf(query.term.toUpperCase()) >= 0) {
              addThis = true;
            }
          } else {
            addThis = true;
          }

          if (addThis === true) {
            data.results.push({id: name, text: name});
          }
        });

        if (!exact && !blank) {
          data.results.unshift({id: query.term, text: query.term, ne: true});
        }

        query.callback(data);
      },
      formatSelection: function (object) {
        if (object.ne) {
          return object.text + " <span><i style=\"color: greenyellow;\">(to be created)</i></span>";
        } else {
          return object.text;
        }
      }
    };
  });
});