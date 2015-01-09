define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('rootSfcCtrl', function ($rootScope, SfcRestangularSvc, $sessionStorage, $location, yangUtils) {

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

    $rootScope['section_logo'] = 'logo_sfc';

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
        type: ["napt44", "dpiss", "firewall", "qos", "ids"],
        failmode: ["open", "close"]
      };
    }

    if (angular.isUndefined($rootScope.serviceLocatorConstants)) {
      $rootScope.serviceLocatorConstants =
      {
        transport: ["vxlan-gpe", "gre", "other"],
        type: ["ip", "mac", "lisp", "function"]
      };
    }

    $rootScope.aclConstants =
    {
      "ace-type": ["ip", "eth"],
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
          "sff-sf-data-plane-locator": {"ovs-bridge": {}},
          "sff-interfaces": [],
          "type": "dpi"
        }],
        "service-node": null, "name": "SFF1", "rest-uri": "http://www.example.com/sffs/sff-bootstrap"
      },
      classifierDefault: {
        "service-function-forwarder": [{}],
        "name": "Classifier1"
      },
      aclDefault: {
        "access-list-entries": [
          {
            "matches": {
              "absolute": {"active": "true"},
              "source-ipv4-address": "0.0.0.0/0",
              "destination-ipv4-address": "0.0.0.0/0",
              "source-port-range": {"lower-port": "80", "upper-port": "80"},
              "ip-protocol": "7"
            },
            "actions": {}, "rule-name": "ACE1"
          }
        ],
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
      }

    };

    $sessionStorage.$default({
      restangularBaseUrl: $location.protocol() + "://" + $location.host() + ":" + $location.port() + "/restconf"
    });
    SfcRestangularSvc.changeBaseUrl($sessionStorage.restangularBaseUrl);
  });

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

      if (!_.isEqual(newVal, oldVal)) {
        $scope.ngChangeFunction({selected: $scope.bindingProperty});
      }
    });

    $scope.select2Options = {
      allowClear: true,
      query: function (query) {
        var data = {results: []};
        var exact = false;
        var blank = _.str.isBlank(query.term);

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