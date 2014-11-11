define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceChainHelper', function ($rootScope) {
    var svc = {};

    svc.setSFCstate = function setSFCstate(sfc, newState) {
      if (angular.isDefined(sfc.state) && sfc.state === $rootScope.sfcState.NEW) {
        sfc.state = $rootScope.sfcState.NEW;
      }
      else {
        sfc.state = newState;
      }
    };

    return svc;
  });

  sfc.register.factory('ModalSfpInstantiateSvc', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (sfc, callback) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/servicechain/servicechain.modal.instantiate.tpl.html',
        controller: 'ModalSfpInstantiateCtrl',
        resolve: {
          sfc: function () {
            return sfc;
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

  sfc.register.factory('ModalSfNameSvc', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (sfc, sf, callback) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/servicechain/servicechain.modal.sfname.tpl.html',
        controller: 'ModalSfNameCtrl',
        resolve: {
          sfc: function () {
            return sfc;
          },
          sf: function () {
            return sf;
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