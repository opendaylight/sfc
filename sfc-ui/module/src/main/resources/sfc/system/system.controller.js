define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('sfcSystemCtrl', function ($scope, SfcSystemSvc, SfcTableParamsSvc, ngTableParams, $filter) {
    var thisCtrl = this;

    $scope.features = [];

    var NgTableParams = ngTableParams;
    SfcTableParamsSvc.initializeSvcForTable('systemTable');

    $scope.tableParams = new NgTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
          moduleName: 'asc'     // initial sorting
        }
      },
      {
        total: $scope.features.length,
        getData: function ($defer, params) {
          SfcTableParamsSvc.setFilterTableParams('systemTable', params.filter());

          // use build-in angular filter
          var filteredData = SfcTableParamsSvc.checkAndSetFilterTableParams('systemTable', $scope.tableParams) ?
            $filter('filter')($scope.features, params.filter()) :
            $scope.features;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        },
        counts: []
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
      $scope.tableParams.sorting({});
    };

    $scope.fetchTableData = function () {
      $scope.$broadcast('fetchTableData');
      thisCtrl.fetchData();
    };
  });

  sfc.register.controller('sfcMdsalCtrl', function ($scope, SfcMdsalCheckSvc, SfcTableParamsSvc, ngTableParams, $filter) {
    var thisCtrl = this;

    $scope.mdsalStatusArray = [];

    var NgTableParams = ngTableParams;
    SfcTableParamsSvc.initializeSvcForTable('mdsalTable');

    $scope.tableParams = new NgTableParams({
        page: 1,            // show first page
        count: 15,          // count per page
        sorting: {
          module: 'asc'     // initial sorting
        }
      },
      {
        counts: [15, 25, 50, 100],
        total: $scope.mdsalStatusArray.length,
        getData: function ($defer, params) {
          SfcTableParamsSvc.setFilterTableParams('mdsalTable', params.filter());

          // use build-in angular filter
          var filteredData = SfcTableParamsSvc.checkAndSetFilterTableParams('mdsalTable', $scope.tableParams) ?
            $filter('filter')($scope.mdsalStatusArray, params.filter()) :
            $scope.mdsalStatusArray;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        },
        counts: []
      });

    this.fetchData = function () {
      SfcMdsalCheckSvc.checkMdsalStatus(function (data) {
        $scope.mdsalStatusArray = data;
        $scope.tableParams.reload();
      });
    };

    this.fetchData();

    $scope.$on('clearTableSorting', function () {
      $scope.tableParams.sorting({});
    });

    $scope.$on('fetchTableData', function () {
      thisCtrl.fetchData();
    });

    $scope.getStatusColor = function (status) {
      switch (status) {
        case "200":
          return 'green';
        case "404":
          return 'orange';
        default:
          return 'red';
      }
    };

  });

  sfc.register.controller('sfcSystemLogCtrl', function ($scope, SfcSystemSvc, SfcTableParamsSvc, ngTableParams, $filter, $q, SfcSystemModalException) {
    var thisCtrl = this;

    $scope.logs = [];

    var NgTableParams = ngTableParams;
    SfcTableParamsSvc.initializeSvcForTable('systemLogTable');

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
          SfcTableParamsSvc.setFilterTableParams('systemLogTable', params.filter());

          // use build-in angular filter
          var filteredData = SfcTableParamsSvc.checkAndSetFilterTableParams('systemLogTable', $scope.tableParams) ?
            $filter('filter')($scope.logs, params.filter()) :
            $scope.logs;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        }
      });

    $scope.fetchData = function () {
      SfcSystemSvc.getLogs(function (data) {
        $scope.logs = data;
        $scope.tableParams.reload();
      });
    };

    $scope.fetchData();

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

    $scope.$on('SYS_LOG_FETCH_DATA', function(){
      $scope.fetchData();
    });
  });

  sfc.register.controller('sfcSystemModalExceptionCtrl', function ($modalInstance, $scope, log) {
    $scope.log = log;

    $scope.dismiss = function () {
      $modalInstance.dismiss('close');
    };
  });
});