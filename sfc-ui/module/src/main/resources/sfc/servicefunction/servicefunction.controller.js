define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('serviceFunctionCtrl', function ($scope, $window, $state, ServiceFunctionSvc, ServiceFunctionHelper, ServiceLocatorHelper, ModalDeleteSvc, SfcTableParamsSvc, ngTableParams, $filter, $q) {
    var thisCtrl = this;
    var NgTableParams = ngTableParams;

    $scope.sfs = [];

    SfcTableParamsSvc.initializeSvcForTable('serviceFunctionTable');
    $scope.tableParams = new NgTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
          name: 'asc'     // initial sorting
        }
      },
      {
        total: $scope.sfs.length,
        getData: function ($defer, params) {
          SfcTableParamsSvc.setFilterTableParams('serviceFunctionTable', params.filter());

          // use build-in angular filter
          var filteredData = SfcTableParamsSvc.checkAndSetFilterTableParams('serviceFunctionTable', $scope.tableParams) ?
            $filter('filter')($scope.sfs, params.filter()) :
            $scope.sfs;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        },
        counts: []
      });

    $scope.fetchData = function () {
      ServiceFunctionSvc.getArray(function (data) {
        $scope.sfs = data;
        _.each($scope.sfs, function (sf) {
          sf['sf-data-plane-locator-string'] = ServiceFunctionHelper.sfDpLocatorToString(sf['sf-data-plane-locator']);
        });

        $scope.tableParams.reload();
      });
    };

    $scope.fetchData();

    $scope.getSfLocatorTooltipText = function (locator) {
      return ServiceLocatorHelper.getSfLocatorTooltipText(locator, $scope);
    };

    $scope.sfTypes = function () {
      var def = $q.defer();
      var data = [];
      _.each($scope.serviceFunctionConstants.type, function (type) {
        data.push({id: type, title: type});
      });
      def.resolve(data);
      return def;
    };

    $scope.sfNshAware = function () {
      var def = $q.defer();
      var data = [];
      data.push({id: 'true', title: 'true'});
      data.push({id: 'false', title: 'false'});
      def.resolve(data);
      return def;
    };

    $scope.deleteSF = function deleteSF(sf) {
      ModalDeleteSvc.open(sf.name, function (result) {
        if (result == 'delete') {
          //delete the row
          ServiceFunctionSvc.deleteItem(sf, function () {
            $scope.sfs.splice(_.indexOf($scope.sfs, sf), 1);
            $scope.tableParams.reload();
          });
        }
      });
    };

    $scope.cloneSF = function cloneSF(sf) {
      delete sf['sf-data-plane-locator-string'];
      sf['name'] = sf['name'] + "_copy";
      $state.transitionTo('main.sfc.servicefunction-clone', {"sf": JSON.stringify(sf)}, {
        location: true,
        inherit: true,
        relative: $state.$current,
        notify: true
      });
    };

    $scope.editSF = function editSF(sfName) {
      $state.transitionTo('main.sfc.servicefunction-edit', {"sfName": sfName}, {
        location: true,
        inherit: true,
        relative: $state.$current,
        notify: true
      });
    };

    $scope.statsSF = function statsSF(sf) {
      if (sf['type'] == 'Cisco-vASA' || sf['type'] == 'Cisco-vNBAR') {
        $state.transitionTo('main.sfc.servicefunction-stats', {"sfName": sf.name}, {
          location: true,
          inherit: true,
          relative: $state.$current,
          notify: true
        });
     }
     else {}
    };

    $scope.deleteAll = function deleteAll() {
      ModalDeleteSvc.open($scope.$eval('"SFC_SERVICE_FUNCTIONS" | translate'), function (result) {
        if (result == 'delete') {
          ServiceFunctionSvc.deleteAll(function () {
            $scope.fetchData();
          });
        }
      });
    };
  });

  sfc.register.controller('serviceFunctionCreateCtrl', function ($scope, $state, $stateParams, $sce, SfcDataTemplateSvc, ServiceFunctionSvc, ServiceFunctionHelper, ServiceForwarderSvc) {

    $scope.data = {"sf-data-plane-locator": [{}]};

    ServiceForwarderSvc.getArray(function (data) {
      $scope.sffs = data;

      if (angular.isDefined($stateParams.sf)) {
        $scope.data = JSON.parse($stateParams.sf);
        ServiceFunctionHelper.nshAwareToString($scope.data);
      }
      else if (angular.isDefined($stateParams.sfName)) {
        ServiceFunctionSvc.getItem($stateParams.sfName, function (item) {
          $scope.data = item;
          ServiceFunctionHelper.nshAwareToString($scope.data);
        });
      }
    });

    $scope.addLocator = function () {
      ServiceFunctionHelper.addLocator($scope);
    };

    $scope.removeLocator = function (index) {
      ServiceFunctionHelper.removeLocator(index, $scope);
    };

    $scope.htmlSafe = function (data) {
        return $sce.trustAsHtml(data);
    };

    $scope.renderIframe = function () {
      var ifrm = "",
          ifrmSafe = "";
      if ($scope.data['type'] == 'Cisco-vASA') {
        ifrm = '<iframe id=ifrm src="https://' + $scope.data['ip-mgmt-address'] + '/admin/exec/show+nsh" width="1100" height="850" style="background-color: Snow;"></iframe>';
        ifrmSafe = $scope.htmlSafe(ifrm);
        return ifrmSafe; 
      } 
      else if ($scope.data['type'] == 'Cisco-vNBAR') {
        ifrm = '<iframe id=ifrm src="http://' + $scope.data['ip-mgmt-address'] + '/appBars.html" width="1100" height="850"></iframe>';
        ifrmSafe = $scope.htmlSafe(ifrm);
        return ifrmSafe; 
      } 
      else {}
    };

    $scope.submit = function () {
      ServiceFunctionSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.servicefunction', null, {
          location: true,
          inherit: true,
          relative: $state.$current,
          notify: true
        });
      });
    };
  });

  sfc.register.controller('serviceFunctionTypeCtrl', function ($scope, ServiceFunctionTypeSvc, SfcTableParamsSvc, ngTableParams, $filter) {

    $scope.sfts = [];

    var NgTableParams = ngTableParams;
    SfcTableParamsSvc.initializeSvcForTable('serviceFunctionTypeTable');

    $scope.tableParams = new NgTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
          'type': 'asc'     // initial sorting
        }
      },
      {
        total: $scope.sfts.length,
        getData: function ($defer, params) {
          SfcTableParamsSvc.setFilterTableParams('serviceFunctionTypeTable', params.filter());

          // use build-in angular filter
          var filteredData = SfcTableParamsSvc.checkAndSetFilterTableParams('serviceFunctionTypeTable', $scope.tableParams) ?
            $filter('filter')($scope.sfts, params.filter()) :
            $scope.sfts;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        }
      });

    $scope.fetchData = function () {
      $scope.sfts = [];
      ServiceFunctionTypeSvc.getArray(function (data) {
        //expand sf type into rows
        _.each(data, function (sft) {
          if (!_.isEmpty(sft['sft-service-function-name'])) {
            _.each(sft['sft-service-function-name'], function (entry) {
              entry['type'] = sft['type'];
              $scope.sfts.push(entry);
            });
          }
        });

        $scope.tableParams.reload();
      });
    };

    $scope.fetchData();

    $scope.$on('FETCH_DATA_TYPES', function(){
      $scope.fetchData();
    });
  });

});
