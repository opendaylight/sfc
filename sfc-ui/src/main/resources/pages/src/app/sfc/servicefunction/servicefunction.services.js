define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceFunctionHelper', function () {
    var svc = {};

    svc.removeTemporaryPropertiesFromSf = function(sf) {
      if (angular.isDefined(sf['sf-data-plane-locator'])){
        _.each(sf['sf-data-plane-locator'], function (locator){
          if (angular.isDefined(locator['locator-type'])){
            delete locator['locator-type'];
          }
        });
      }
    };

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

    return svc;
  });
});