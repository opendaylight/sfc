define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceForwarderHelper', function () {
    var svc = {};

    svc.addLocator = function ($scope) {
      if (angular.isUndefined($scope.data['sff-data-plane-locator'])) {
        $scope.data['sff-data-plane-locator'] = [];
      }
      $scope.data['sff-data-plane-locator'].push({"data-plane-locator": {}});
    };

    svc.removeLocator = function (index, $scope) {
      $scope.data['sff-data-plane-locator'].splice(index, 1);
    };

    svc.addFunction = function ($scope) {
      if (angular.isUndefined($scope.data['service-function-dictionary'])) {
        $scope.data['service-function-dictionary'] = [];
      }
      $scope.data['service-function-dictionary'].push(
        {
          "nonExistent": false,
          "sff-sf-data-plane-locator": {},
          "sff-interfaces": []
        });
    };

    svc.removeFunction = function (index, $scope) {
      $scope.data['service-function-dictionary'].splice(index, 1);
    };

    svc.addForwarder = function ($scope) {
      if (angular.isUndefined($scope.data['connected-sff-dictionary'])) {
        $scope.data['connected-sff-dictionary'] = [];
      }
      $scope.data['connected-sff-dictionary'].push(
        {
          "nonExistent": false,
          "sff-sff-data-plane-locator": {},
          "sff-interfaces": []
        });
    };

    svc.removeForwarder = function (index, $scope) {
      $scope.data['connected-sff-dictionary'].splice(index, 1);
    };

    svc.sffInterfaceToString = function (sffInterfaces) {
      var string = "";
      _.each(sffInterfaces, function (interf) {
        string = string.concat(interf['sff-interface'] + ", ");
      });
      return string.slice(0, -2);
    };

    svc.sffInterfaceToStringArray = function (sffInterfaces) {
      var array = [];
      _.each(sffInterfaces, function (interf) {
        array.push(interf['sff-interface']);
      });
      return array;
    };

    svc.sffInterfaceInSffToString = function (sffInterfaces) {
      var string = "";
      _.each(sffInterfaces, function (interf) {
        string = string.concat(interf['sff-interface'] + ", ");
      });
      return string.slice(0, -2);
    };

    svc.sffInterfaceInSffToStringArray = function (sffInterfaces) {
      var array = [];
      _.each(sffInterfaces, function (interf) {
        array.push(interf['sff-interface']);
      });
      return array;
    };

    svc.sffInterfaceToObjectArray = function (sffInterfaces) {
      var objectArray = [];
      _.each(sffInterfaces, function (interf) {
        objectArray.push({'sff-interface': interf});
      });
      return objectArray;
    };

    svc.sffInterfaceInSffToObjectArray = function (sffInterfaces) {
      var objectArray = [];
      _.each(sffInterfaces, function (interf) {
        objectArray.push({'sff-interface': interf});
      });
      return objectArray;
    };

    svc.sffDpLocatorToString = function (sffDpLocators) {
      var string = "";
      _.each(sffDpLocators, function (locator) {
        string = string.concat(locator.name + " ");
      });
      return string;
    };

    svc.sffFunctionDictionaryToString = function (sffFunctionDictionary) {
      var string = "";
      _.each(sffFunctionDictionary, function (sf) {
        string = string.concat(sf.name + " ");
      });
      return string;
    };

    svc.sffForwarderDictionaryToString = function (sffForwarderDictionary) {
      var string = "";
      _.each(sffForwarderDictionary, function (sff) {
        string = string.concat(sff.name + " ");
      });
      return string;
    };

    svc.removeTemporaryPropertiesFromSf = function (sf) {
      if (angular.isDefined(sf.nonExistent)) {
        delete sf.nonExistent;
      }
    };

    svc.removeTemporaryPropertiesFromSff = function (sff) {
      if (angular.isDefined(sff.nonExistent)) {
        delete sff.nonExistent;
      }
    };

    svc.removeNonExistentSn = function (sff, sns) {
      if (angular.isDefined(sff['service-node'])) {
        var snExists = _.findWhere(sns, {'name': sff['service-node']});

        if (angular.isUndefined(snExists)) {
          delete sff['service-node'];
        }
      }
    };

    svc.addSfTypeToChoosenSf = function (choosenSf, sfModelData) {
      choosenSf.type = sfModelData.type;
    };

    svc.sfChangeListener = function (choosenSf, $scope) {
      if (angular.isDefined(choosenSf)) {
        var sfModelData = _.findWhere($scope.sfs, {name: choosenSf.name});

        if (angular.isDefined(sfModelData)) {
          svc.addSfTypeToChoosenSf(choosenSf, sfModelData);
        }
      }
    };

    /*
    svc.addMgtIpToChoosenSff = function (choosenSff, sffModelData) {
      choosenSff.ip-mgmt-address = sffModelData.ip-mgmt-address;
    };

    svc.sffChangeListener = function (choosenSff, $scope) {
      if (angular.isDefined(choosenSff)) {
        var sffModelData = _.findWhere($scope.sffs, {name: choosenSff.name});

        if (angular.isDefined(sffModelData)) {
          svc.addMgtIpToChoosenSff(choosenSff, sffModelData);
        }
      }
    };
    */

    svc.sfUpdate = function (sf, $scope) {
      if (angular.isDefined(sf)) {
        sf['sff-interfaces'] = svc.sffInterfaceToStringArray(sf['sff-interfaces']);
        var sfModelData = _.findWhere($scope.sfs, {name: sf.name});

        sf.nonExistent = angular.isDefined(sfModelData) ? false : true;
      }
    };

    svc.sffUpdate = function (sff, $scope) {
      if (angular.isDefined(sff)) {
        sff['sff-interfaces'] = svc.sffInterfaceInSffToStringArray(sff['sff-interfaces']);
        var sffModelData = _.findWhere($scope.sffs, {name: sff.name});

        sff.nonExistent = angular.isDefined(sffModelData) ? false : true;
      }
    };

    svc.selectOptions = function ($scope) {
      return {
        'multiple': true,
        'simple_tags': true,
        'tags': function () {
          var interfacesArray = [];
          _.each($scope.data['sff-data-plane-locator'], function (locator) {
            if (angular.isDefined(locator['name'])) {
              interfacesArray.push(locator['name']);
            }
          });
          return interfacesArray;
        }
      };
    };

    svc.getOVStooltipText = function (ovs, $scope) {
      if(angular.isUndefined(ovs) || angular.isUndefined(ovs['bridge-name'])){
        return;
      }

      var tootlip = "<br/>" + $scope.$eval('"SFC_FORWARDER_OVS_BRIDGE" | translate') + ": " + "<br/>";

      tootlip += $scope.$eval('"SFC_FORWARDER_SHORT_BRIDGE_NAME" | translate') + ": " + ovs['bridge-name'] + "<br/>";

      if(angular.isDefined(ovs['uuid'])){
        tootlip += $scope.$eval('"SFC_FORWARDER_SHORT_BRIDGE_UUID" | translate') + ": " + ovs['uuid'] + "<br/>";
      }

      _.each(ovs['external-ids'], function (eid){
        tootlip += $scope.$eval('"SFC_FORWARDER_SHORT_BRIDGE_EID" | translate') + ": " + eid['name'] + " : " + eid['value'] + "<br/>";
      });

      return tootlip;
    };

    return svc;
  });
});