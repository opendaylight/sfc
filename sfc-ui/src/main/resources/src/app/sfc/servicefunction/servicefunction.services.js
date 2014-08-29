define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceFunctionHelper', function (){
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

    return svc;
  });
});