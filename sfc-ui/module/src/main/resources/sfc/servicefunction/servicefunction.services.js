define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceFunctionHelper', function () {
    var svc = {};

    svc.addLocator = function ($scope) {
      if (angular.isUndefined($scope.data['sf-data-plane-locator'])) {
        $scope.data['sf-data-plane-locator'] = [];
      }
      $scope.data['sf-data-plane-locator'].push({});
    };

    svc.removeLocator = function (index, $scope) {
      $scope.data['sf-data-plane-locator'].splice(index, 1);
    };

    svc.sfDpLocatorToString = function (sfDpLocators) {
      var string = "";
      _.each(sfDpLocators, function (locator) {
        string = string.concat(locator.name + " ");
      });
      return string;
    };

    svc.nshAwareToString = function (sf) {
      if (angular.isDefined(sf['nsh-aware'])) {
        sf['nsh-aware'] = sf['nsh-aware'] === true ? "true" : "false";
      }
    };

    return svc;
  });
});