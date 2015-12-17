define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('renderedServicePathCtrl', function ($scope, $state, $rootScope, ServiceFunctionSvc, ServiceForwarderSvc,
                                                               RenderedServicePathSvc, SfcContextMetadataSvc, SfcVariableMetadataSvc,
                                                               ModalDeleteSvc, ModalErrorSvc, ngTableParams, $filter) {
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
        },
        counts: []
      });

    $scope.fetchData = function () {
      var rspToClassifierMap = {};

        RenderedServicePathSvc.getOperationalArray(function (data) {
          _.each(data, function (rsp) {
            // rsp['classifier'] = rspToClassifierMap[rsp['name']];

            rsp['context-metadata'] = $scope.sfps.length ? $scope.sfps.filter(function(sfp){
                return sfp.name === rsp['parent-service-function-path'];
            })[0]['context-metadata'] : '';
          });

          $scope.rsps = data || [];

          $scope.tableParams.reload();
        });
    };

    $scope.vnbar = function vnbar(sfName) {
        $state.transitionTo('main.sfc.servicepath-vnbar', {"sfName": sfName}, {
          location: true,
          inherit: true,
          relative: $state.$current,
          notify: true
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

    $scope.rpcDeleteRsp = function (rsp) {
      ModalDeleteSvc.open(rsp['name'], function (result) {
        if (result == 'delete') {
          //delete the row
          RenderedServicePathSvc.executeRpcOperation({input: {name: rsp['name']}}, 'delete-rendered-path', null, function (result) {
            if (angular.isDefined(result) && result['result'] === true) {
              $scope.rsps.splice(_.indexOf($scope.rsps, rsp), 1);
              $scope.tableParams.reload();
            }
            else {
              ModalErrorSvc.open({
                head: $scope.$eval('"SFC_RENDERED_PATH_MODAL_DELETE_RPC_FAIL_HEAD" | translate'),
                rpcError: result['response'] || ''
              });
            }
          });
        }
      });
    };

    $scope.$on('REN_PATH_FETCH_DATA', function(){
        $scope.fetchData();
    });

  });
});
