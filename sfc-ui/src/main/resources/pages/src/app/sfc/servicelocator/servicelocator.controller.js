define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('serviceLocatorCtrl', function ($scope, ServiceLocatorHelper) {

    $scope.$watch('service_locator', function (newVal) {

      if (angular.isUndefined(newVal)){
        return;
      }

      $scope.locator_type = ServiceLocatorHelper.getLocatorType($scope.service_locator);
    });
  });

  sfc.register.controller('serviceLocatorCtrlIp', function ($scope) {

    $scope.$on($scope.resetOn, function(event, arg){
      if (arg != $scope.notResetCondition){
        delete $scope['service_locator']['ip'];
        delete $scope['service_locator']['port'];
      }
    });

  });

  sfc.register.controller('serviceLocatorCtrlMac', function ($scope) {

    $scope.$on($scope.resetOn, function(event, arg){
      if (arg != $scope.notResetCondition) {
        delete $scope['service_locator']['mac'];
        delete $scope['service_locator']['vlan-id'];
      }
    });

  });

  sfc.register.controller('serviceLocatorCtrlLisp', function ($scope) {

    $scope.$on($scope.resetOn, function(event, arg){
      if (arg != $scope.notResetCondition) {
        delete $scope['service_locator']['eid'];
      }
    });

  });

  sfc.register.controller('serviceLocatorSelectorCtrl', function ($scope, ServiceLocatorHelper, ServiceFunctionSvc) {

    $scope.sfLocators = [];
    $scope.sfLocatorsAvailable = undefined;

    $scope.locatorToString = function (locator) {
      return ServiceLocatorHelper.locatorToString(locator, $scope);
    };

    $scope.setSffSfLocator = function (sf, locatorName) {
      var locator = _.clone(_.findWhere($scope.sfLocators, {name: locatorName}));
      if (locator) {
        delete locator['name'];
        delete locator['service-function-forwarder'];
        sf['sff-sf-data-plane-locator'] = locator;
      } else {
        sf['sff-sf-data-plane-locator'] = {};
      }
    };

    $scope.$watch('service_function', function (newVal, oldVal) {

      if (angular.isUndefined(newVal) || angular.isUndefined(newVal['name'])){
        return;
      }

      if ($scope.sfLocatorsAvailable == newVal['name']) {
        return;
      }

      ServiceFunctionSvc.getItem(newVal['name'], function(item) {
        if (angular.isDefined(item)) {
          $scope.sfLocators = item['sf-data-plane-locator'];
          $scope.sfLocatorsAvailable = newVal['name'];
          $scope.selected_locator = ServiceLocatorHelper.getLocatorName(newVal['sff-sf-data-plane-locator'], $scope.sfLocators);
        }
      });

    }, true);

  });

});