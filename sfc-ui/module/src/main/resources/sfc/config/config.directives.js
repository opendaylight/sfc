define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('ngFileSelect', function() {
    return {
      link: function ($scope, el) {
        el.bind("change", function (e) {
          $scope.getOnFileSelect((e.srcElement || e.target).files[0]);
        });
      }
    };
  });

});

