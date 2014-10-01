define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('showValidationError', function ($timeout) {
    return {
      restrict: 'A',
      require: '^form',
      link: function (scope, el, attrs, formCtrl) {
        // find the text box element, which has the 'name' attribute
        var inputEl = el[0].querySelector("[name]");
        // convert the native text box element to an angular element
        var inputNgEl = angular.element(inputEl);
        // get the name on the text box so we know the property to check
        // on the form controller
        var inputName = inputNgEl.attr('name');
        $timeout(function (){
          el.toggleClass('has-error', formCtrl[inputName].$invalid);
        }, 500);

        // only apply the has-error class after the user leaves the text box
        inputNgEl.bind('change', function () {
          el.toggleClass('has-error', formCtrl[inputName].$invalid);
        });
      }
    };
  });

  sfc.register.directive('sfcWatchForReinit', function ($log) {
    // adds wotch in scope for 're'-ngInit
    return {
      restrict: 'A',
      link: function (scope, iElement, iAttrs) {
        var count = 1;

        if (iAttrs && iAttrs['sfcWatchForReinit'] && iAttrs['ngInit']) {

          scope.$watch(iAttrs['sfcWatchForReinit'], function (newVal) {
            if (count === 1) {
              count--;
              return; // skip first time - ngInit will do the work
            }

            if (angular.isUndefined(newVal)) {
              return;
            }

            scope.$eval(iAttrs['ngInit']);
          });
        } else {
          $log.warn('sfcWatchForReinit - illegal arguments');
        }
      }
    };
  });

  sfc.register.directive('uiSelect2Label', function ($timeout) {
    return {
      restrict: 'A',
      link: function (scope, iElement, iAttrs) {
        // fix for focus on label click
        $timeout(function () {
            if (iAttrs && iAttrs['uiSelect2Label']) {
              iElement.bind("click", function () {
                $('#s2id_' + iAttrs['uiSelect2Label'] + " > input").focus();
              });
            }
          }
        );
      }
    };
  });

  sfc.register.directive('sfcForwarderSelect2', function () {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        'inputId': '@inputId',
        'sffs': '=sffs',
        'sffProp': '=sffProp'
      },
      template: '<input type="hidden" id="{{inputId}}" class="form-control input-sm" ui-select2="select2Options" ng-model="tmpSffForSelect2" ng-required="false" data-placeholder="{{\'SFC_FUNCTION_SFF_CREATE_NAME\' | translate}}">',
      controller: ['$scope', function ($scope) {

        // initial
        if ($scope.sffProp) {
          $scope.tmpSffForSelect2 = {
            id: $scope.sffProp,
            text: $scope.sffProp
          };
        }

        // sync/copy 'id' to model
        $scope.$watch(function () {
          if ($scope.tmpSffForSelect2) {
            $scope.sffProp = $scope.tmpSffForSelect2.id;
          }
        });

        $scope.select2Options = {
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
      }]
    };
  });

  sfc.register.directive('dateAndTime', function () {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(function (viewValue) {
          if (viewValue === null || viewValue === "") {
            ctrl.$setValidity('dateAndTime', true);
            return null;
          }
          else if (viewValue.match(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[\+\-]\d{2}:\d{2})/)) {
            ctrl.$setValidity('dateAndTime', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('dateAndTime', false);
            return undefined;
          }
        });
      }
    };
  });

  sfc.register.directive('vlanId', function () {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(function (viewValue) {
          if (viewValue === null || viewValue === "") {
            ctrl.$setValidity('vlanId', true);
            return null;
          }
          else if (viewValue >= 1 && viewValue <= 4094) {
            ctrl.$setValidity('vlanId', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('vlanId', false);
            return undefined;
          }
        });
      }
    };
  });

  sfc.register.directive('macAddress', function () {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(function (viewValue) {
          if (viewValue === null || viewValue === "") {
            ctrl.$setValidity('macAddress', true);
            return null;
          }
          else if (viewValue.match(/^([0-9a-fA-F]{2}[:]){5}([0-9a-fA-F]{2})$/)) {
            ctrl.$setValidity('macAddress', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('macAddress', false);
            return undefined;
          }
        });
      }
    };
  });

  sfc.register.directive('uint8', function () {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(function (viewValue) {
          if (viewValue === null || viewValue === "") {
            ctrl.$setValidity('uint8', true);
            return null;
          }
          else if (viewValue >= 0 && viewValue <= 63) {
            ctrl.$setValidity('uint8', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('uint8', false);
            return undefined;
          }
        });
      }
    };
  });

  sfc.register.directive('uint32', function () {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(function (viewValue) {
          if (viewValue === null || viewValue === "") {
            ctrl.$setValidity('uint32', true);
            return null;
          }
          else if (viewValue >= 0 && viewValue <= 1048575) {
            ctrl.$setValidity('uint32', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('uint32', false);
            return undefined;
          }
        });
      }
    };
  });

  sfc.register.directive('port', function () {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(function (viewValue) {
          if (viewValue === null || viewValue === "") {
            ctrl.$setValidity('port', true);
            return null;
          }
          else if (viewValue >= 0 && viewValue <= 65535) {
            ctrl.$setValidity('port', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('port', false);
            return undefined;
          }
        });
      }
    };
  });

  sfc.register.directive('ipAddress', function () {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(function (viewValue) {
          if (viewValue === null || viewValue === "") {
            ctrl.$setValidity('ipAddress', true);
            return null;
          }
          else if (inet_pton(viewValue)) {
            ctrl.$setValidity('ipAddress', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('ipAddress', false);
            return undefined;
          }
        });
      }
    };
  });

  function inet_pton(a) {
    //  discuss at: http://phpjs.org/functions/inet_pton/
    // original by: Theriault
    //   example 1: inet_pton('::');
    //   returns 1: '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0'
    //   example 2: inet_pton('127.0.0.1');
    //   returns 2: '\x7F\x00\x00\x01'

    // enhanced by: Andrej Kincel (akincel@cisco.com)
    //    features: IPv4 regex checks for valid range

    var r, m, x, i, j, f = String.fromCharCode;
    // IPv4
    m = a.match(/^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/);
    if (m) {
      m = m[0].split('.');
      m = f(m[0]) + f(m[1]) + f(m[2]) + f(m[3]);
      // Return if 4 bytes, otherwise false.
      return m.length === 4 ? m : false;
    }
    r = /^((?:[\da-fA-F]{1,4}(?::|)){0,8})(::)?((?:[\da-fA-F]{1,4}(?::|)){0,8})$/;
    // IPv6
    m = a.match(r);
    if (m) {
      // Translate each hexadecimal value.
      for (j = 1; j < 4; j++) {
        // Indice 2 is :: and if no length, continue.
        if (j === 2 || m[j].length === 0) {
          continue;
        }
        m[j] = m[j].split(':');
        for (i = 0; i < m[j].length; i++) {
          m[j][i] = parseInt(m[j][i], 16);
          // Would be NaN if it was blank, return false.
          if (isNaN(m[j][i])) {
            // Invalid IP.
            return false;
          }
          m[j][i] = f(m[j][i] >> 8) + f(m[j][i] & 0xFF);
        }
        m[j] = m[j].join('');
      }
      x = m[1].length + m[3].length;
      if (x === 16) {
        return m[1] + m[3];
      }
      else if (m[2] !== undefined) {
        if (x < 16 && m[2].length > 0) {
          return m[1] + (new Array(16 - x + 1))
            .join('\x00') + m[3];
        }
      }
    }
    // Invalid IP.
    return false;
  }

  //textarea from xeditable enhanced with save on blur event
  sfc.register.directive('easyEditableTextarea', ['editableDirectiveFactory',
    function (editableDirectiveFactory) {
      return editableDirectiveFactory({
        directiveName: 'easyEditableTextarea',
        inputTpl: '<textarea></textarea>',
        addListeners: function () {
          var self = this;
          self.parent.addListeners.call(self);
          // submit textarea by ctrl+enter even with buttons
          if (self.single && self.buttons !== 'no') {
            self.autosubmit();
          }
        },
        autosubmit: function () {
          var self = this;
          self.inputEl.bind('keydown', function (e) {
            if ((e.ctrlKey || e.metaKey) && (e.keyCode === 13)) {
              self.scope.$apply(function () {
                self.scope.$form.$submit();
              });
            }
          });
          self.inputEl.bind('blur', function (e) {
            self.scope.$apply(function () {
              self.scope.$form.$submit();
            });
          });
        }
      });
    }]);

  sfc.register.directive('effectMe', function ($timeout, $parse, $rootScope) {
    return {

      link: function (scope, element, attrs) {

        var effectMeGetter = $parse(attrs.effectMe); // will watch this expression
        var effectMeSetter = effectMeGetter.assign;

        scope.$watch(
          function () { //
            return effectMeGetter(scope);
          },
          function (newValue, oldValue) {

            if (newValue === undefined) {
              return;
            }

            effectMeSetter(scope, undefined); // reset watched property

            $timeout(function () {
//              $(element[0]).effect("bounce", { times: 10 }, "slow");
              $(element[0]).effect("highlight", {color: "#aaaaff"}, 600);
            });
          },
          false);
      }
    };
  });

  sfc.register.directive('focusMe', function ($timeout, $parse) {
    return {
      link: function (scope, element, attrs) {
        var model = $parse(attrs.focusMe);

        var setFocus = function () {
          $timeout(function () {
            element[0].focus();
          });
        };

        if (model.assign) {  // binding to assignable expression
          scope.$watch(model, function (value) {
            if (value === true) {
              setFocus();
            }
          });

          element.bind('blur', function () {
            scope.$apply(model.assign(scope, false));
          });
        } else {
          setFocus();
        }
      }
    };
  });
});