define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServicePathHelper', function ($filter) {
    var svc = {};

    svc.orderHopsInSFP = function (sfp) {
      var orderedHops = $filter('orderBy')(sfp['service-path-hop'], "+'hop-number'");
      sfp['service-path-hop'] = orderedHops;
    };

    svc.getCountOfSfsInSFP = function (sfp) {
      var count = 0;
      _.each(sfp['service-path-hop'], function(hop){
        if(angular.isDefined(hop['service-function-name'])){
          count++;
        }
      });
      return count;
    };

    svc.updateHopsOrderInSFP = function (sfp) {
      var serviceIndex = svc.getCountOfSfsInSFP(sfp);
      sfp['starting-index'] = serviceIndex;

      _.each(sfp['service-path-hop'], function (hop, index) {
        //first hop should have the biggest index = total number of SFs
        hop['service_index'] = serviceIndex;
        hop['hop-number'] = index;

        //if hop is SF it consumes service_index thus decrement it
        if(angular.isDefined(hop['service-function-name'])){
          //until it equals 1
          if (serviceIndex != 1){
            serviceIndex--;
          }
        }
      });
    };

    var sfpState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};
    if (angular.isDefined(Object.freeze)) {
      Object.freeze(sfpState);
    }

    svc.setSFPstate = function (sfp, newState) {
      if (angular.isDefined(sfp.state) && sfp.state === sfpState.NEW) {
        sfp.state = sfpState.NEW;
      }
      else {
        sfp.state = newState;
      }
    };

    return svc;
  });

  sfc.register.factory('ServicePathModalSffSelect', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (sfName, sffNameList, callback) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/servicepath/servicepath.modal.sff.select.tpl.html',
        controller: 'servicePathModalSffSelectCtrl',
        resolve: {
          sfName: function () {
            return sfName;
          },
          sffNameList: function () {
            return sffNameList;
          }
        }
      });

      modalInstance.result.then(function (result) {
        callback(result);
      }, function (reason) {
        callback(reason);
      });

      return modalInstance;
    };
    return svc;
  });

});