define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceFunctionHelper', function () {
    var svc = {};

    svc.addLocator = function ($scope) {
      if (angular.isUndefined($scope.data['sf-data-plane-locator'])) {
        $scope.data['sf-data-plane-locator'] = [];
      }
      $scope.data['sf-data-plane-locator'].push({});
    };

    svc.removeLocator = function (index, $scope) {
      $scope.data['sf-data-plane-locator'].splice(index, 1);
    };

    svc.sfDpLocatorToString = function (sfDpLocators) {
      var string = "";
      _.each(sfDpLocators, function (locator) {
        string = string.concat(locator.name + " ");
      });
      return string;
    };

    svc.select2Options = function ($scope) {
      return {
        query: function (query) {
          var data = {results: []};
          var exact = false;
          var blank = _.str.isBlank(query.term);

          _.each($scope.sffs, function (sff) {
            var name = sff.name;
            var addThis = false;

            if (!blank) {
              if (query.term == name) {
                exact = true;
                addThis = true;
              } else if (name.toUpperCase().indexOf(query.term.toUpperCase()) >= 0) {
                addThis = true;
              }
            } else {
              addThis = true;
            }

            if (addThis === true) {
              data.results.push({id: name, text: name});
            }
          });

          if (!exact && !blank) {
            data.results.unshift({id: query.term, text: query.term, ne: true});
          }

          query.callback(data);
        },
        formatSelection: function (object) {
          if (object.ne) {
            return object.text + " <span><i style=\"color: greenyellow;\">(to be created)</i></span>";
          } else {
            return object.text;
          }
        }
      };
    };

    return svc;
  });
});