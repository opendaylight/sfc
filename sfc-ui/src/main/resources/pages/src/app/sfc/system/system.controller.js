define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('sfcSystemCtrl', function ($scope, SfcSystemSvc, ngTableParams, $filter) {
    var thisCtrl = this;

    $scope.features = [];

    var NgTableParams = ngTableParams;
    $scope.tableParams = new NgTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
          name: 'asc'     // initial sorting
        }
      },
      {
        total: $scope.features.length,
        getData: function ($defer, params) {
          // use build-in angular filter
          var filteredData = params.filter() ?
            $filter('filter')($scope.features, params.filter()) :
            $scope.features;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        }
      });

    this.fetchData = function () {
      SfcSystemSvc.getFeatures(function (data) {
        $scope.features = SfcSystemSvc.getSfcFeatures(data);
        $scope.tableParams.reload();
      });
    };

    this.reloadData = function (data) {
      $scope.features = SfcSystemSvc.getSfcFeatures(data);
      $scope.tableParams.reload();
    };

    this.fetchData();

    $scope.isFeatureInstalled = function (feature) {
      return feature['state'] === "Installed";
    };

    $scope.isFeatureReloading = function (feature) {
      return feature['reloading'];
    };

    $scope.refreshRepository = function (feature) {
      feature['reloading'] = true;
      SfcSystemSvc.refreshRepository(feature['repository-url'], function (data) {
        thisCtrl.reloadData(data);
        feature['reloading'] = false;
      });
    };

    $scope.uninstallFeature = function (feature) {
      feature['reloading'] = true;
      SfcSystemSvc.uninstallFeature(feature['name'], feature['version'], function (data) {
        thisCtrl.reloadData(data);
        feature['reloading'] = false;
      });
    };

    $scope.installFeature = function (feature) {
      feature['reloading'] = true;
      SfcSystemSvc.installFeature(feature['name'], feature['version'], function (data) {
        thisCtrl.reloadData(data);
        feature['reloading'] = false;
      });
    };

    $scope.clearSorting = function () {
      $scope.$broadcast('clearTableSorting');
    };

    $scope.$on('clearTableSorting', function () {
      $scope.tableParams.sorting({});
    });
  });

  sfc.register.controller('sfcSystemLogCtrl', function ($scope, SfcSystemSvc, ngTableParams, $filter, $q, SfcSystemModalException) {
    var thisCtrl = this;

    $scope.logs = [];

    var NgTableParams = ngTableParams;
    $scope.tableParams = new NgTableParams({
        page: 1,            // show first page
        count: 25,          // count per page
        sorting: {
          received: 'desc'     // initial sorting
        }
      },
      {
        total: $scope.logs.length,
        getData: function ($defer, params) {
          // use build-in angular filter
          var filteredData = params.filter() ?
            $filter('filter')($scope.logs, params.filter()) :
            $scope.logs;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        }
      });

    this.fetchData = function () {
      SfcSystemSvc.getLogs(function (data) {
        $scope.logs = data;
        $scope.tableParams.reload();
      });
    };

    this.reloadData = function () {
      $scope.tableParams.reload();
    };

    this.fetchData();

    $scope.logLevels = function () {
      var def = $q.defer();
      var data = [];
      _.each($scope.logConstants.level, function (level) {
        data.push({id: level, title: level});
      });
      def.resolve(data);
      return def;
    };

    $scope.$on('clearTableSorting', function () {
      $scope.tableParams.sorting({});
    });

    $scope.showException = function (log) {
      SfcSystemModalException.open(log);
    };
  });

  sfc.register.controller('sfcSystemModalExceptionCtrl', function ($modalInstance, $scope, log) {
    $scope.log = log;

    $scope.dismiss = function () {
      $modalInstance.dismiss('close');
    };
  });
});