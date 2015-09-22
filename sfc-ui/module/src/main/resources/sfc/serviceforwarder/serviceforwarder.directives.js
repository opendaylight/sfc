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

  sfc.register.directive('sffOptions', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: 'src/app/sfc/serviceforwarder/serviceforwarder.options.tpl.html',
      scope: {
        idSuffix: '@idSuffix',
        options: '=options'
      },
      controller: function ($scope) {

        $scope.$watch('options', function (newVal) {
          if (angular.isUndefined(newVal)) {
            return;
          }

          var options = newVal;

          _.each(Object.keys(options), function (key) {
            if (options[key] == null) {
              delete options[key];
            }
          });

        }, true);
      }
    };
  });

});