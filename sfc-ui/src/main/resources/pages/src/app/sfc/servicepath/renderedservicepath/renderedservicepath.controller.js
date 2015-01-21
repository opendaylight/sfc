define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('renderedServicePathCtrl', function ($scope, $rootScope, ServiceFunctionSvc, ServiceForwarderSvc, RenderedServicePathSvc, SfcContextMetadataSvc, SfcVariableMetadataSvc, ngTableParams, $filter) {
    var thisCtrl = this;
    var NgTableParams = ngTableParams; // checkstyle 'hack'
    $scope.rsps = [];

    $scope.tableParams = new NgTableParams({
        page: 1,            // show first page
        count: 10           // count per page
      },
      {
        total: $scope.rsps.length,
        getData: function ($defer, params) {
          // use build-in angular filter
          var filteredData = params.filter() ?
            $filter('filter')($scope.rsps, params.filter()) :
            $scope.rsps;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        }
      });

    $scope.fetchData = function () {
      RenderedServicePathSvc.getOperationalArray(function (data) {
        $scope.rsps = data || [];
        $scope.tableParams.reload();
      });
    };

    $scope.fetchData();

    $scope.getSFindexInSFS = function getSFindexInSFS(sfName) {
      var sfObject = _.findWhere($scope.sfs, {name: sfName});
      return _.indexOf($scope.sfs, sfObject);
    };

    $scope.getHopClass = function getHopClass(hop) {
      if (angular.isDefined(hop) && (angular.isDefined(hop['service-function-name']) || angular.isDefined(hop['service-function-forwarder']))) {
        if (angular.isDefined(hop['service-function-name'])) {
          return "sf";
        }
        else {
          return "sff";
        }
      }
    };
  });

});