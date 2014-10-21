define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('tlvFlags', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: 'src/app/sfc/metadata/metadata.variable.tlvflags.tpl.html',
      scope: {
        idSuffix: '@idSuffix',
        flags: "=flags"
      },
      controller: function ($scope) {

        this.initRmodel = function (flags) {
          $scope.r1 = "";
          $scope.r2 = "";
          $scope.r3 = "";

          if (!_.isEmpty(flags)) {
            var stringArray = flags.split(' ');
            _.each(stringArray, function (bit) {
              switch (bit) {
                case 'r1':
                  $scope.r1 = 'r1';
                  break;
                case 'r2':
                  $scope.r2 = 'r2';
                  break;
                case 'r3':
                  $scope.r3 = 'r3';
                  break;
              }
            });
          }
        };

        this.initRmodel($scope.flags);

        $scope.onFlagChange = function () {
          $scope.flags = $scope.r1 + " " + $scope.r2 + " " + $scope.r3;
        };

      }
    };
  });

});