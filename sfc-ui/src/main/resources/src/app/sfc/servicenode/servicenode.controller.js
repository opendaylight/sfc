define(['app/sfc/sfc.module'], function (sfc) {

  function removeNonExistentSfs(sn, sfs) {
    _.each(sn['service-function'], function (sf, index) {
      var sfExists = getSFfromSFS(sf, sfs);
      if (angular.isUndefined(sfExists)) {
        sn['service-function'].splice(index, 1);
      }
    });
  }

  function addNecessarySff(sn, sfs) {
    _.each(sn['service-function'], function (sf) {
      var sff = getSFfromSFS(sf, sfs)['sf-data-plane-locator']['service-function-forwarder'];

      if (angular.isUndefined(sn['service-function-forwarder'])) {
        sn['service-function-forwarder'] = [];
      }

      if (!_.contains(sn['service-function-forwarder'], sff)) {
        sn['service-function-forwarder'].push(sff);
      }
    });
  }

  function getSFfromSFS(sfName, sfs) {
    return sfObject = _.findWhere(sfs, {name: sfName});
  }

  sfc.register.controller('serviceNodeCtrl', function ($scope, $state, ServiceFunctionSvc, ServiceForwarderSvc, ServiceNodeSvc, ServiceNodeTopologyBackend, ModalDeleteSvc) {
    $scope.deleteServiceNode = function deleteServiceNode(snName) {
      ModalDeleteSvc.open(snName, function (result) {
        if (result == 'delete') {
          ServiceNodeSvc.deleteItem({name: snName}, function () {
            //after delete refresh local service node array
            ServiceNodeSvc.getArray(function (data) {
              $scope.sns = data;
              $scope.snsGraph = ServiceNodeTopologyBackend.createGraphData($scope.sns, $scope.sffs, $scope.sfs);
            });
          });
        }
      });
    };

    $scope.editServiceNode = function editServiceNode(snName) {
      $state.transitionTo('main.sfc.servicenode-edit', {snName: snName}, { location: true, inherit: true, relative: $state.$current, notify: true });
    };

    $scope.getSnsGraphClass = function getSnsGraphClass(snList) {
      //determine the maximum number of SFs attached to SN
      var maxSf = 0;
      _.each(snList, function (sn) {
        if (angular.isDefined(sn['service-function']) && maxSf < sn['service-function'].length) {
          maxSf = sn['service-function'].length;
        }
      });
      //if it is lq than 10 allow 3 SNs to be placed side by side, else only 2 SNs
      return maxSf <= 10 ? "col-xs-12 col-md-6 col-lg-4" : "col-xs-12 col-md-12 col-lg-6";
    };

    $scope.experimental = false;

    $scope.toggleGraphData = function () {
      if ($scope.experimental) {
        $scope.snsGraph = ServiceNodeTopologyBackend.createGraphDataExperimentalSFF($scope.sns, $scope.sffs, $scope.sfs);
      }
      else {
        $scope.snsGraph = ServiceNodeTopologyBackend.createGraphData($scope.sns, $scope.sffs, $scope.sfs);
      }
    };

    ServiceFunctionSvc.getArray(function (data) {
      $scope.sfs = data;

      ServiceForwarderSvc.getArray(function (data) {
        $scope.sffs = data;

        ServiceNodeSvc.getArray(function (data) {
          $scope.sns = data;
          $scope.snsGraph = ServiceNodeTopologyBackend.createGraphData($scope.sns, $scope.sffs, $scope.sfs);
        });
      });
    });
  });

  sfc.register.controller('serviceNodeEditCtrl', function ($scope, $state, $stateParams, ServiceFunctionSvc, ServiceNodeSvc) {
    $scope.data = {};

    ServiceFunctionSvc.getArray(function (data) {
      $scope.sfs = data;

      ServiceNodeSvc.getItem($stateParams.snName, function (item) {
        $scope.data = item;
        removeNonExistentSfs($scope.data, $scope.sfs);
      });
    });

    $scope.submit = function () {
      //addNecessarySff($scope.data, $scope.sfs);
      ServiceNodeSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.servicenode', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };
  });

  sfc.register.controller('serviceNodeCreateCtrl', function ($scope, $state, ServiceFunctionSvc, ServiceNodeSvc) {
    $scope.data = {};

    ServiceFunctionSvc.getArray(function (data) {
      $scope.sfs = data;
    });

    $scope.submit = function () {
      addNecessarySff($scope.data, $scope.sfs);
      ServiceNodeSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.servicenode', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };
  });

});
