define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('serviceFunctionCtrl', function ($scope, $state, ServiceFunctionSvc, ServiceFunctionHelper, ServiceLocatorHelper, ModalDeleteSvc, ngTableParams, $filter, $q) {
    var NgTableParams = ngTableParams;

    ServiceFunctionSvc.getArray(function (data) {
      $scope.sfs = data;
      _.each($scope.sfs, function (sf) {
        sf['sf-data-plane-locator-string'] = ServiceFunctionHelper.sfDpLocatorToString(sf['sf-data-plane-locator']);
      });

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
            // use build-in angular filter
            var filteredData = params.filter() ?
              $filter('filter')($scope.sfs, params.filter()) :
              $scope.sfs;

            var orderedData = params.sorting() ?
              $filter('orderBy')(filteredData, params.orderBy()) :
              filteredData;

            params.total(orderedData.length);
            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
        });
    });

    $scope.getSfLocatorTooltipText = function(locator) {
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

    $scope.editSF = function editSF(sfName) {
      $state.transitionTo('main.sfc.servicefunction-edit', {sfName: sfName}, { location: true, inherit: true, relative: $state.$current, notify: true });
    };
  });

  sfc.register.controller('serviceFunctionCreateCtrl', function ($scope, $state, ServiceFunctionSvc, ServiceFunctionHelper, ServiceForwarderSvc, ServiceLocatorHelper) {

    ServiceForwarderSvc.getArray(function (data) {
      $scope.sffs = data;
    });

    $scope.data = {"sf-data-plane-locator": [
      {}
    ]};

    $scope.addLocator = function () {
      ServiceFunctionHelper.addLocator($scope);
    };

    $scope.removeLocator = function (index) {
      ServiceFunctionHelper.removeLocator(index, $scope);
    };

    $scope.submit = function () {
      ServiceFunctionHelper.removeTemporaryPropertiesFromSf($scope.data);
      ServiceFunctionSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.servicefunction', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };
  });

  sfc.register.controller('serviceFunctionEditCtrl', function ($scope, $state, $stateParams, ServiceFunctionSvc, ServiceFunctionHelper, ServiceForwarderSvc, ServiceLocatorHelper) {

    function nshAwareToString(sf) {
      if (angular.isDefined(sf['nsh-aware'])) {
        sf['nsh-aware'] = sf['nsh-aware'] === true ? "true" : "false";
      }
    }

    $scope.data = {"sf-data-plane-locator": []}; // empty array on init of edit

    ServiceForwarderSvc.getArray(function (data) {
      $scope.sffs = data;

      ServiceFunctionSvc.getItem($stateParams.sfName, function (item) {
        $scope.data = item;
        nshAwareToString($scope.data);

        _.each($scope.data['sf-data-plane-locator'], function (locator) {
        });

      });
    });

    $scope.addLocator = function () {
      ServiceFunctionHelper.addLocator($scope);
    };

    $scope.removeLocator = function (index) {
      ServiceFunctionHelper.removeLocator(index, $scope);
    };

    $scope.submit = function () {
      ServiceFunctionHelper.removeTemporaryPropertiesFromSf($scope.data);
      ServiceFunctionSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.servicefunction', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };
  });

});