define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('ipAddress', function () {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(function (viewValue) {
          if (inet_pton(viewValue)) {
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
    r = /^((?:[\da-f]{1,4}(?::|)){0,8})(::)?((?:[\da-f]{1,4}(?::|)){0,8})$/;
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