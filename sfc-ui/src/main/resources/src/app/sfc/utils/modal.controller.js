define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('ModalInfoCtrl', function ($modalInstance, $scope, info) {
    $scope.info = info;

    $scope.dismiss = function () {
      $modalInstance.dismiss('ok');
    };
  });

  sfc.register.controller('ModalErrorCtrl', function ($modalInstance, $scope, error) {
    $scope.error = error;

    $scope.isRpcError = function () {
      return (!!error.rpcError);
    };

    $scope.dismiss = function () {
      $modalInstance.dismiss('close');
    };
  });

  sfc.register.controller('ModalDeleteCtrl', function ($modalInstance, $scope, name) {
    $scope.name = name;

    $scope.delete = function () {
      $modalInstance.close('delete');
    };

    $scope.dismiss = function () {
      $modalInstance.dismiss('cancel');
    };
  });

});