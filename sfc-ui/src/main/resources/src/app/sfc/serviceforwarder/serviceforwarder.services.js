define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceForwarderHelper', function (){
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

    svc.sffInterfaceToObjectArray = function (functionDictionaryList) {
      _.each(functionDictionaryList, function (sf) {
        var sffInterfaces = [];
        _.each(sf['sff-interfaces'], function (interf) {
          sffInterfaces.push({'sff-interface': interf});
        });
        sf['sff-interfaces'] = sffInterfaces;
      });
    };

    svc.removeTemporaryPropertiesFromSf = function (functionDictionaryList) {
      _.each(functionDictionaryList, function (sf) {
        if (angular.isDefined(sf.nonExistent)){
          delete sf.nonExistent;
        }
      });
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

    svc.getSfDpLocators = function (choosenSf, sfModelData, $scope) {
      if (angular.isDefined(sfModelData['sf-data-plane-locator'])) {
        $scope.DpLocators[choosenSf.name] = sfModelData['sf-data-plane-locator'];
      }
      else {
        $scope.DpLocators[choosenSf.name] = [];
      }
    };

    svc.dpChangeListener = function (sf, $scope) {
      var dpLocator = _.findWhere($scope.DpLocators[sf.name], {name: $scope.selectedDpLocator[sf.name]});
      delete dpLocator.name;
      delete dpLocator['service-function-forwarder'];
      sf['sff-sf-data-plane-locator'] = dpLocator;
    };

    svc.sfChangeListener = function (choosenSf, $scope) {
      if (angular.isDefined(choosenSf)) {
        var sfModelData = _.findWhere($scope.sfs, {name: choosenSf.name});

        if (angular.isDefined(sfModelData)) {
          svc.addSfTypeToChoosenSf(choosenSf, sfModelData);
          svc.getSfDpLocators(choosenSf, sfModelData, $scope);
        }
      }
    };

    return svc;
  });
});