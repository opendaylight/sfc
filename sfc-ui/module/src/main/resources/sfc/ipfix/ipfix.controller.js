define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('sfcIpfixCtrl', function ($scope, $state, SfcIpfixClassIdSvc, SfcIpfixAppIdSvc, ModalDeleteSvc, SfcTableParamsSvc, ngTableParams, $filter) {

    var thisCtrl = this;
    var NgTableParams = ngTableParams; // checkstyle

    this.getNgTableParams = function (dataKey) {
      SfcTableParamsSvc.initializeSvcForTable(dataKey + 'Table');
      var s = {};
      if(dataKey == 'classId') {
        s['id'] = 'asc';
      } else {
        s['selector-id'] = 'asc';
      }
      return new NgTableParams({
          page: 1,          // show first page
          count: 10,        // count per page
          sorting: s
        },
        {
          total: 0,
          getData: function ($defer, params) {
            SfcTableParamsSvc.setFilterTableParams(dataKey + 'Table', params.filter());

            // use build-in angular filter
            var filteredData = SfcTableParamsSvc.checkAndSetFilterTableParams(dataKey + 'Table', $scope[dataKey + 'TableParams']) ?
              $filter('filter')($scope[dataKey], params.filter()) :
              $scope[dataKey];

            var orderedData = params.sorting() ?
              $filter('orderBy')(filteredData, params.orderBy()) :
              filteredData;

            params.total(orderedData.length);
            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          },
          counts: []
        });
    };

    this.fetchClassId = function () {
      $scope.classId = [];

      SfcIpfixClassIdSvc.getArray(function (data) {
        $scope.classId = data.sort(function(a, b) { return a.id - b.id; }); // sort by id
        $scope.classIdTableParams.reload();
      });
    };

    this.fetchAppId = function () {
      $scope.appId = [];

      SfcIpfixAppIdSvc.getArray(function (data) {
        $scope.appId = data.sort(function(a, b) {   // group by classid and sort by selector-id
            var r = a['class-id'] - b['class-id'];
            if (r === 0) {
              return a['selector-id'] - b['selector-id'];
            } else {
              return r;
            }
        });
        $scope.appIdTableParams.reload();
      });
    };

    this.fetchData = function () {
      this.fetchClassId();
      this.fetchAppId();
    };

    this.init = function () {
      $scope.classId = [];
      $scope.appId = [];
      $scope.classIdTableParams = this.getNgTableParams('classId');
      $scope.appIdTableParams = this.getNgTableParams('appId');
      thisCtrl.fetchData();
    };

    //init controller
    this.init();

    $scope.deleteClassIdItem = function deleteClassIdItem(classIdItem) {
      ModalDeleteSvc.open(classIdItem['name'], function (result) {
        if (result === 'delete') {
            //delete the row
            SfcIpfixClassIdSvc.deleteItem(classIdItem, function () {
            thisCtrl.fetchClassId();
          });
        }
      });
    };

    $scope.deleteAppIdItem = function deleteAppIdItem(appIdItem) {
      ModalDeleteSvc.open(appIdItem['applicationName'], function (result) {
        if (result === 'delete') {
            //delete the row
            SfcIpfixAppIdSvc.deleteItem(appIdItem, function () {
            thisCtrl.fetchAppId();
          });
        }
      });
    };

    $scope.deleteAllClassId = function deleteAllClassId() {
      ModalDeleteSvc.open($scope.$eval('"SFC_IPFIX_CLASSID" | translate'), function (result) {
        if (result === 'delete') {
            SfcIpfixClassIdSvc.deleteAll(function () {
            thisCtrl.fetchClassId();
          });
        }
      });
    };

    $scope.deleteAllAppId = function deleteAllAppId() {
      ModalDeleteSvc.open($scope.$eval('"SFC_IPFIX_APPID" | translate'), function (result) {
        if (result === 'delete') {
            SfcIpfixAppIdSvc.deleteAll(function () {
            thisCtrl.fetchAppId();
          });
        }
      });
    };

    $scope.editClassIdItem = function editClassIdItem(classIdItem) {
      $state.transitionTo('main.sfc.ipfix-classid-edit', {itemKey: classIdItem['name']}, { location: true, inherit: true, relative: $state.$current, notify: true });
    };

    $scope.editAppIdItem = function editAppIdItem(appIdItem) {
      $state.transitionTo('main.sfc.ipfix-appid-edit', {itemKey: appIdItem['applicationName']}, { location: true, inherit: true, relative: $state.$current, notify: true });
    };
  });

  sfc.register.controller('sfcIpfixClassIdCreateCtrl', function ($scope, $rootScope, $state, $stateParams, SfcIpfixClassIdSvc) {

    $scope.data = {}; // initial empty data

    if ($stateParams.itemKey) {
        SfcIpfixClassIdSvc.getItem($stateParams.itemKey, function (item) {
        $scope.data = item;
      });
    } else {
      $scope.data = {};
    }

    $scope.submit = function () {
      SfcIpfixClassIdSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.ipfix', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };

  });

  sfc.register.controller('sfcIpfixAppIdCreateCtrl', function ($scope, $rootScope, $state, $stateParams, SfcIpfixClassIdSvc, SfcIpfixAppIdSvc) {

    $scope.data = {}; // initial empty data
    $scope.classId = [];

    SfcIpfixClassIdSvc.getArray(function (data) {
        $scope.classId = data.sort(function(a, b) { return a.id - b.id; }); // sort by id
    });

    if ($stateParams.itemKey) {
        SfcIpfixAppIdSvc.getItem($stateParams.itemKey, function (item) {
        $scope.data = item;
      });
    } else {
      $scope.data = {};
    }

    $scope.submit = function () {
      SfcIpfixAppIdSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.ipfix', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };

  });

}); // end define