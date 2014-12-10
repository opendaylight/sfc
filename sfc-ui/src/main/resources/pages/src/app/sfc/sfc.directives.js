define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('showValidationError', function ($timeout) {
    return {
      restrict: 'A',
      require: '^form',
      scope: {},  // for watch need isolated scope - specialy for destroying the watch
      link: function (scope, el, attrs, formCtrl) {
        // find the text box element, which has the 'name' attribute
        var inputEl = el[0].querySelector("[name]");
        // convert the native text box element to an angular element
        var inputNgEl = angular.element(inputEl);
        // get the name on the text box so we know the property to check
        // on the form controller
        var inputName = inputNgEl.attr('name');

        // wait for initial model update
        $timeout(function () {
          // el.toggleClass('has-error', formCtrl[inputName].$invalid);

          scope.$watch(function () {
            return formCtrl[inputName].$invalid;
          }, function (newVal) {
            el.toggleClass('has-error', newVal);
          });

        }, 500);

        // only apply the has-error class after the user leaves the text box
//        inputNgEl.bind('change', function () {
//          el.toggleClass('has-error', formCtrl[inputName].$invalid);
//        });

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
                $('#s2id_' + iAttrs['uiSelect2Label']).find("input").focus();
              });
            }
          }
        );
      }
    };
  });

  sfc.register.directive('sfcSelect2CreateSearchChoice', function () {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        'inputId': '@inputId',
        'placeHolder': '@placeHolder',
        'availableOptionsArray': '=',
        'bindingProperty': '='
      },
      template: '<input type="hidden" id="{{inputId}}" class="form-control input-sm" ui-select2="select2Options" ng-model="tmpForSelect2" ng-required="false" data-placeholder="{{placeHolder | translate}}">',
      controller: 'sfcSelect2CreateSearchChoiceCtrl'
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

  sfc.register.directive('numberRange', function ($parse) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {

        var params = $parse(attrs['numberRange'])();

        ctrl.$parsers.unshift(function (viewValue) {
          if (viewValue === null || viewValue === "") {
            ctrl.$setValidity('numberRange', true);
            return null;
          }
          else if (viewValue >= params.from && viewValue <= params.to) {
            ctrl.$setValidity('numberRange', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('numberRange', false);
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
          else if (isHexadecimal(viewValue)){
            var decimal = parseInt(viewValue, 16);
            if(decimal >= 0 && decimal <= 255){
              ctrl.$setValidity('uint8', true);
              return viewValue;
            }
            else {
              ctrl.$setValidity('uint8', false);
              return undefined;
            }
          }
          else if (viewValue >= 0 && viewValue <= 255) {
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

  sfc.register.directive('uint16', function () {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(function (viewValue) {
          if (viewValue === null || viewValue === "") {
            ctrl.$setValidity('uint16', true);
            return null;
          }
          else if (isHexadecimal(viewValue)){
            var decimal = parseInt(viewValue, 16);
            if(decimal >= 0 && decimal <= 65535){
              ctrl.$setValidity('uint16', true);
              return viewValue;
            }
            else {
              ctrl.$setValidity('uint16', false);
              return undefined;
            }
          }
          else if (viewValue >= 0 && viewValue <= 65535) {
            ctrl.$setValidity('uint16', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('uint16', false);
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
          else if (isHexadecimal(viewValue)){
            var decimal = parseInt(viewValue, 16);
            if(decimal >= 0 && decimal <= 4294967295){
              ctrl.$setValidity('uint32', true);
              return viewValue;
            }
            else {
              ctrl.$setValidity('uint32', false);
              return undefined;
            }
          }
          else if (viewValue >= 0 && viewValue <= 4294967295) {
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

  sfc.register.directive('ipAddress', function ($parse) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {

        var params = $parse(attrs['ipAddress'])();

        ctrl.$parsers.unshift(function (viewValue) {
          if (viewValue === null || viewValue === "") {
            ctrl.$setValidity('ipAddress', true);
            return null;
          }
          else if (inet_pton(viewValue, params)) {
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

  function isHexadecimal(string) {
    return string.match(/^0x[0-9A-Fa-f]+$/) ? true : false;
  }

  function inet_pton(a, params) {
    //  discuss at: http://phpjs.org/functions/inet_pton/
    // original by: Theriault
    //   example 1: inet_pton('::');
    //   returns 1: '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0'
    //   example 2: inet_pton('127.0.0.1');
    //   returns 2: '\x7F\x00\x00\x01'

    // enhanced by: Andrej Kincel (akincel@cisco.com)
    //    features: IPv4 regex checks for valid range

    var r, m, x, i, j, f = String.fromCharCode, prefix;

    if (params && params.prefix === true) {
      m = a.match(/(\/)([0-9]+)$/);
      if (m) {
        prefix = parseInt(m[2]);
        a = a.replace(/\/[0-9]+$/, ""); // trim prefix
      } else {
        return false;
      }
    }

    // IPv4
    if (!params || params.version != 6) {
      m = a.match(/^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/);
      if (m) {

        if (prefix && prefix > 32) {
          return false;
        }

        m = m[0].split('.');
        m = f(m[0]) + f(m[1]) + f(m[2]) + f(m[3]);
        // Return if 4 bytes, otherwise false.
        return m.length === 4 ? m : false;
      }
    }

    if (!params || params.version != 4) {
      // IPv6
      r = /^((?:[\da-fA-F]{1,4}(?::|)){0,8})(::)?((?:[\da-fA-F]{1,4}(?::|)){0,8})$/;
      m = a.match(r);
      if (m) {

        if (prefix && prefix > 128) {
          return false;
        }

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