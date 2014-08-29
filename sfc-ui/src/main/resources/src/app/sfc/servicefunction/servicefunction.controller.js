define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('serviceFunctionCtrl', function ($scope, $state, ServiceFunctionSvc, ModalDeleteSvc, ngTableParams, $filter, $timeout) {
    var NgTableParams = ngTableParams;

    ServiceFunctionSvc.getArray(function (data) {
      $scope.sfs = data;

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
            var orderedData = params.sorting() ?
              $filter('orderBy')($scope.sfs, params.orderBy()) :
              $scope.sfs;

            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
        });
    });

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

    $scope.data = {"sf-data-plane-locator": [{}]};

    $scope.addLocator = function () {
      ServiceFunctionHelper.addLocator($scope);
    };

    $scope.removeLocator = function (index) {
      ServiceFunctionHelper.removeLocator(index, $scope);
    };

//    $scope.select2ModelSff = $scope.data["service-function-forwarder"];  // initial copy to select2 view model (for optional future 'edit sf' feature)
//
//    $scope.$watch(
//      function () {
//        if ($scope.select2ModelSff) {
//          return $scope.select2ModelSff.id; // watch id in select2 view model
//        } else {
//          return undefined;
//        }
//      },
//      function (newVal) {
//        $scope.data["service-function-forwarder"] = newVal; // copy id to our model
//      }
//    );
//
//    $scope.select2Options = {
//      query: function (query) {
//        var data = {results: []};
//        var exact = false;
//        var blank = _.str.isBlank(query.term);
//
//        _.each($scope.sffs, function (sff) {
//          var name = sff.name;
//          var addThis = false;
//
//          if (!blank) {
//            if (query.term == name) {
//              exact = true;
//              addThis = true;
//            } else if (name.toUpperCase().indexOf(query.term.toUpperCase()) >= 0) {
//              addThis = true;
//            }
//          } else {
//            addThis = true;
//          }
//
//          if (addThis === true) {
//            data.results.push({id: name, text: name});
//          }
//        });
//
//        if (!exact && !blank) {
//          data.results.unshift({id: query.term, text: query.term, ne: true});
//        }
//
//        query.callback(data);
//      },
//      formatSelection: function (object, container) {
//        if (object.ne) {
//          return object.text + " <span><i style=\"color: greenyellow;\">(to be created)</i></span>";
//        } else {
//          return object.text;
//        }
//      }
//    };

    $scope.submit = function () {
      ServiceFunctionSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.servicefunction', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };
  });

  sfc.register.controller('serviceFunctionEditCtrl', function ($scope, $state, $stateParams, ServiceFunctionSvc, ServiceFunctionHelper, ServiceForwarderSvc) {
    $scope.data = {"sf-data-plane-locator": [{}]};

    ServiceForwarderSvc.getArray(function (data) {
      $scope.sffs = data;

      ServiceFunctionSvc.getItem($stateParams.sfName, function (item) {
        $scope.data = item;
        removeNonExistentSff($scope.data['sf-data-plane-locator'], $scope.sffs);
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

  function removeNonExistentSff(sfDPlocator, sffs) {
    if (angular.isDefined(sfDPlocator)) {

      var sffExist = _.findWhere(sffs, {name: sfDPlocator['service-function-forwarder']});
      if (angular.isUndefined(sffExist)) {
        delete sfDPlocator['service-function-forwarder'];
      }
    }
  }
});