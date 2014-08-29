define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ModalInfoSvc', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (info) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/utils/modal.info.tpl.html',
        controller: 'ModalInfoCtrl',
        resolve: {
          info: function () {
            return info;
          }
        }
      });
    };

    return svc;
  });

  sfc.register.factory('ModalErrorSvc', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (error) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/utils/modal.error.tpl.html',
        controller: 'ModalErrorCtrl',
        resolve: {
          error: function () {
            return error;
          }
        }
      });
    };

    return svc;
  });

  sfc.register.factory('ModalDeleteSvc', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (name, callback) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/utils/modal.delete.tpl.html',
        controller: 'ModalDeleteCtrl',
        resolve: {
          name: function () {
            return name;
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