define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('sffBridge', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: 'src/app/sfc/serviceforwarder/serviceforwarder.bridge.tpl.html',
      scope: {
        idSuffix: '@idSuffix',
        bridge: '=bridge'
      },
      controller: function ($scope) {

        $scope.addEid = function() {
          if (!$scope.bridge['external-ids']) {
            $scope.bridge['external-ids'] = [];
          }
          $scope.bridge['external-ids'].push({});
        };


        $scope.removeEid = function(idx) {
          $scope.bridge['external-ids'].splice(idx, 1);
          if ($scope.bridge['external-ids'].length === 0) {
            delete($scope.bridge['external-ids']);
          }
        };
      }
    };
  });

});