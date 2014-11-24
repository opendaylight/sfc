define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('rootSfcCtrl', function ($rootScope, SfcRestangularSvc, $localStorage) {

//    // register watch for debugging - works only in firefox
//    if (angular.isDefined($rootScope.watch)) {
//      $rootScope.watch('sfcs', function(id, oldval, newval) {
//        console.log('change!!!');
//
//        var innerfn = function(a, b, c) {
//          console.log('inner change!!!');
//          return c;
//        };
//
//        newval.watch('0', innerfn);
//        newval.watch('1', innerfn);
//        newval.watch('2', innerfn);
//
//        return newval;
//      });
//    }

    $rootScope['section_logo'] = 'logo_sfc';

    $rootScope.sfcState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};
    if (angular.isDefined(Object.freeze)) {
      Object.freeze($rootScope.sfcState);
    }

    $rootScope.sfpState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};
    if (angular.isDefined(Object.freeze)) {
      Object.freeze($rootScope.sfpState);
    }

    $rootScope.sfcs = [];
    $rootScope.sfps = [];
    $rootScope.sfpEffectMe = {};
    $rootScope.serviceFunctionConstants =
    {
      type: ["napt44", "dpi", "firewall"],
      failmode: ["open", "close"]
    };
    $rootScope.serviceLocatorConstants =
    {
      transport: ["vxlan-gpe", "gre", "other"],
      type: ["ip", "mac", "lisp"]
    };
    $rootScope.aclConstants =
    {
      "ace-type": ["ip", "eth"],
      "ace-ip": ["IPv4", "IPv6"]
    };
    $rootScope.classifierConstants =
    {
      "attachment-point-type": ["bridge", "interface"]
    };

    $rootScope.logConstants =
    {
      "level": ['INFO', 'DEBUG', 'WARN', 'ERROR']
    };

    $rootScope.experimental = true;

    $localStorage.$default({
      restangularBaseUrl: SfcRestangularSvc.getCurrentBaseUrl()
    });
    SfcRestangularSvc.changeBaseUrl($localStorage.restangularBaseUrl);

  });

  sfc.register.controller('sfcForwarderSelect2Ctrl', function ($scope) {
    var thisCtrl = this;

    //wait for data load, then prefill select2Model
    this.unregisterSffPropWatch = $scope.$watch('sffProp', function (newVal){
      if(angular.isUndefined(newVal) || newVal === null){
        return;
      }

      $scope.tmpSffForSelect2 = {
        id: newVal,
        text: newVal
      };

      thisCtrl.unregisterSffPropWatch();
    });

    // sync/copy 'id' to model
    $scope.$watch(function () {
      if ($scope.tmpSffForSelect2) {
        $scope.sffProp = $scope.tmpSffForSelect2.id;
      }
    });

    $scope.select2Options = {
      allowClear: true,
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
  });
});