define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('servicePathCtrl', function ($scope, $rootScope, ServiceFunctionSvc, ServiceForwarderSvc, ServicePathSvc, ServicePathHelper, ServicePathModalSffSelect, ModalDeleteSvc, ngTableParams, $filter) {
    var NgTableParams = ngTableParams; // checkstyle 'hack'

    $scope.tableParams = new NgTableParams(
      {
        page: 1,            // show first page
        count: 10           // count per page
      },
      {
        total: 0, // wait for 'sfps'
        getData: function ($defer, params) {
          if (params.total() > 0) {
            // use build-in angular filter
            var orderedData = params.sorting() ?
              $filter('orderBy')($scope.sfps, params.orderBy()) :
              $scope.sfps;

            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          } else {
            $defer.resolve([]);
          }
        }
      }
    );

    var sfpsWatcherRegistered = false;

    var registerSfpsWatcher = function () { // wait for combining unpersisted with persisted in getArray callback

      if (!sfpsWatcherRegistered) {
        $scope.$watchCollection('sfps', function (newVal) {
          if (angular.isUndefined(newVal)) {
            return;
          }
          $scope.tableParams.total($scope.sfps.length);
          $scope.tableParams.reload();
        });

        sfpsWatcherRegistered = true;
      }
    };

    $scope.getSFPstate = function getSFPstate(sfp) {
      return sfp.state || $rootScope.sfpState.PERSISTED;
    };

    $scope.setSFPstate = function setSFPstate(sfp, newState) {
      if (angular.isDefined(sfp.state) && sfp.state === $rootScope.sfpState.NEW) {
        sfp.state = $rootScope.sfpState.NEW;
      }
      else {
        sfp.state = newState;
      }
    };

    $scope.unsetSFPstate = function unsetSFPstate(sfp) {
      delete sfp.state;
    };

    $scope.isSFPstate = function isSFPstate(sfp, state) {
      return $scope.getSFPstate(sfp) === state ? true : false;
    };

    ServiceFunctionSvc.getArray(function (data) {
      $scope.sfs = data;

      $scope.tableParamsSfName = new NgTableParams({
          page: 1,            // show first page
          count: 5           // count per page
        },
        {
          counts: [5, 10, 15], // hide page counts control
          total: $scope.sfs.length,  // value less than count hide pagination
          getData: function ($defer, params) {
            // use build-in angular filter
            var orderedData = params.sorting() ?
              $filter('orderBy')($scope.sfs, params.orderBy()) :
              $scope.sfs;

            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
        });
    });

    ServiceForwarderSvc.getArray(function (data) {
      $scope.sffs = data;

      $scope.tableParamsSffName = new NgTableParams({
          page: 1,            // show first page
          count: 5           // count per page
        },
        {
          counts: [5, 10, 15], // hide page counts control
          total: $scope.sffs.length,  // value less than count hide pagination
          getData: function ($defer, params) {
            // use build-in angular filter
            var orderedData = params.sorting() ?
              $filter('orderBy')($scope.sffs, params.orderBy()) :
              $scope.sffs;

            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
        });
    });

    ServicePathSvc.getArray(function (data) {

      //temporary SFPs are kept in rootScope, persisted SFPs should be removed from it
      var tempSfps = [];
      _.each($rootScope.sfps, function (sfp) {
        if ($scope.getSFPstate(sfp) !== $rootScope.sfpState.PERSISTED) {
          tempSfps.push(sfp);
        }
      });

      //concat temporary with loaded data (dont add edited sfcs which are already in tempSfps)
      if (angular.isDefined(data)) {
        _.each(data, function (sfp) {
          //if it is not in tempSfps add it
          if (angular.isUndefined(_.findWhere(tempSfps, {name: sfp.name}))) {
            $scope.setSFPstate(sfp, $rootScope.sfpState.PERSISTED);
            ServicePathHelper.orderHopsInSFP(sfp);
            tempSfps.push(sfp);
          }
        });
      }

      $rootScope.sfps = tempSfps;

      registerSfpsWatcher();
    });

    $scope.undoSFPchanges = function undoSFPchanges(sfp) {
      ServicePathSvc.getItem(sfp.name, function (oldSfp) {
        var index = _.indexOf($rootScope.sfps, sfp);
        $rootScope.sfps.splice(index, 1);
        ServicePathHelper.orderHopsInSFP(oldSfp);
        $rootScope.sfps.splice(index, 0, oldSfp);
      });
    };

    $scope.sortableOptions = {
      connectWith: ".connected-apps-container",
      cursor: 'move',
      placeholder: 'place',
      tolerance: 'pointer',
      start: function (e, ui) {
        $(e.target).data("ui-sortable").floating = true;
      },
      update: function (e, ui) {
        //sfc[0]: name, sfc[1]: index in sfcs
        var sfp = $(e.target).closest('tr').attr('id').split("~!~");
        $scope.setSFPstate(_.findWhere($rootScope.sfps, {"name": sfp[0]}), $rootScope.sfpState.EDITED);
      },
      helper: function (e, ui) {
        var elms = ui.clone();
        elms.find(".arrow-right-part").addClass("arrow-empty");
        elms.find(".arrow-left-part").addClass("arrow-empty");
        return elms;
      }
    };

    var collectDistinctSffsNamesFromSf = function (sf) {
      if (!sf) {
        return [];
      }

      var tmp = {};
      _.each(sf['sf-data-plane-locator'], function (dpl) {
        var sffName;
        if (dpl && (sffName = dpl['service-function-forwarder'])) {
          tmp[sffName] = true; // dummy value
        }
      });

      return _.keys(tmp);
    };

    $scope.onSFPdrop = function (hop, sfp) {
      var hopType, hopName;

      if(angular.isDefined(hop)){
        var str = hop.split('_');
        hopType = str[0];
        hopName = str[1];
      }

      if (hopType == 'sf') {
        var addSfHopToSfp = function (sffName) {
          if (sfp['service-path-hop'] === undefined) {
            sfp['service-path-hop'] = [];
          }

          var hopItem = {"service-function-name": hopName};

          if (sffName) {
            hopItem["service-function-forwarder"] = sffName;
          }

          sfp['service-path-hop'].push(hopItem);
          $scope.setSFPstate(sfp, $rootScope.sfpState.EDITED);
        };

        // retrieve SFF
        var droppedSf = _.findWhere($scope.sfs, {name: hopName});
        var sffNames = collectDistinctSffsNamesFromSf(droppedSf);

        if (sffNames.length > 1) {
          ServicePathModalSffSelect.open(hopName, sffNames, function (sff) {
            if (angular.isDefined(sff.name)) {
              addSfHopToSfp(sff.name);
            }
          });
        } else {
          addSfHopToSfp(sffNames[0]);
        }
      }
      else if (hopType == 'sff') {
        if (sfp['service-path-hop'] === undefined) {
          sfp['service-path-hop'] = [];
        }

        var hopItem = {"service-function-forwarder": hopName};
        sfp['service-path-hop'].push(hopItem);
        $scope.setSFPstate(sfp, $rootScope.sfpState.EDITED);
      }

    };

    $scope.deleteSFP = function deleteSFP(sfp) {
      ModalDeleteSvc.open(sfp.name, function (result) {
        if (result == 'delete') {
          //delete the row
          ServicePathSvc.deleteItem(sfp, function () {
            $rootScope.sfps.splice(_.indexOf($rootScope, sfp), 1);
          });
        }
      });
    };

    $scope.removeSFfromSFP = function removeSFfromSFP(sfp, index) {
      sfp['service-path-hop'].splice(index, 1);
      $scope.setSFPstate(sfp, $rootScope.sfpState.EDITED);
    };

    $scope.persistSFP = function persistSFP(sfp) {
      $scope.unsetSFPstate(sfp);
      ServicePathHelper.updateHopsOrderInSFP(sfp);
      ServicePathSvc.putItem(sfp, function () {
      });
    };

    $scope.getSFindexInSFS = function getSFindexInSFS(sfName) {
      var sfObject = _.findWhere($scope.sfs, {name: sfName});
      return _.indexOf($scope.sfs, sfObject);
    };

    $scope.getHopClass = function getHopClass(hop) {
      if(angular.isDefined(hop)){
        if(angular.isDefined(hop['service-function-name'])){
          return "sf";
        }
        else{
          return "sff";
        }
      }
    };
  });

  sfc.register.controller('servicePathModalSffSelectCtrl', function ($scope, $modalInstance, sfName, sffNameList) {

    $scope.sfName = sfName;
    $scope.sffNameList = sffNameList;

    $scope.save = function () {
      $modalInstance.close({name: this.sffName});
    };

    $scope.dismiss = function () {
      $modalInstance.dismiss('cancel');
    };
  });

})
;