define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('serviceFunctionCtrl', function ($scope, $state, ServiceFunctionSvc, ServiceFunctionHelper, ModalDeleteSvc, ngTableParams, $filter, $q) {
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

    $scope.sfTypes = function () {
      var def = $q.defer();
      var data = [];
      _.each($scope.servicefunction.type, function (type) {
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

  sfc.register.controller('serviceFunctionCreateCtrl', function ($scope, $state, ServiceFunctionSvc, ServiceFunctionHelper, ServiceForwarderSvc) {

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

    $scope.select2Options = ServiceFunctionHelper.select2Options($scope);

    $scope.select2ModelSff = [];

    $scope.$watchCollection(
      function () {
        if ($scope.select2ModelSff) {
          var idArray = [];
          _.each($scope.select2ModelSff, function (sff) {
            if (angular.isDefined(sff) && !_.isNull(sff) && angular.isDefined(sff.id)) {
              idArray.push(sff.id); // watch id in select2 view model
            }
          });
          return idArray;
        } else {
          return undefined;
        }
      },
      function (newVal) {
        if (angular.isDefined(newVal) && !_.isEmpty(newVal)) {
          _.each($scope.data['sf-data-plane-locator'], function (locator, index) {
            // copy id to YANG model
            locator['service-function-forwarder'] = $scope.select2ModelSff[index].id;
          });
        }
      }
    );


    $scope.submit = function () {
      ServiceFunctionSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.servicefunction', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };
  });

  sfc.register.controller('serviceFunctionEditCtrl', function ($scope, $state, $stateParams, ServiceFunctionSvc, ServiceFunctionHelper, ServiceForwarderSvc) {
    $scope.data = {"sf-data-plane-locator": [{}]};

    $scope.select2Options = ServiceFunctionHelper.select2Options($scope);

    $scope.select2ModelSff = [];

    ServiceForwarderSvc.getArray(function (data) {
      $scope.sffs = data;

      ServiceFunctionSvc.getItem($stateParams.sfName, function (item) {
        $scope.data = item;

        $scope.select2ModelSff = [];
        _.each($scope.data['sf-data-plane-locator'], function (locator) {
          $scope.select2ModelSff.push({"id": locator['service-function-forwarder'], "text": locator['service-function-forwarder']});
        });

        $scope.$watchCollection(
          function () {
            if ($scope.select2ModelSff) {
              var idArray = [];
              _.each($scope.select2ModelSff, function (sff) {
                if (angular.isDefined(sff) && !_.isNull(sff) && angular.isDefined(sff.id)) {
                  idArray.push(sff.id); // watch id in select2 view model
                }
              });
              return idArray;
            } else {
              return undefined;
            }
          },
          function (newVal) {
            if (angular.isDefined(newVal) && !_.isEmpty(newVal)) {
              _.each($scope.data['sf-data-plane-locator'], function (locator, index) {
                // copy id to YANG model
                locator['service-function-forwarder'] = $scope.select2ModelSff[index].id;
              });
            }
          }
        );
      });
    });

    $scope.addLocator = function () {
      ServiceFunctionHelper.addLocator($scope);
    };

    $scope.removeLocator = function (index) {
      ServiceFunctionHelper.removeLocator(index, $scope);
    };

    $scope.submit = function () {
      ServiceFunctionSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.servicefunction', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };
  });

});