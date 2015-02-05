define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('serviceNodesTopology', function (ServiceNodeTopologyBackend, $timeout) {

    return {
      restrict: 'E',
      scope: {
        treeViewData: '='
      },
      link: function ($scope, iElm, iAttrs) {

        var defaultImage = {
          url: "assets/images/Device_switch_3062_unknown_64.png",
          wh: "40",
          xy: "-20"
        };

        var defaultText = {x: -50, y: -80, align: "bottom"};

        var margin = {top: 90, right: 0, bottom: 0, left: 0 };

        var i = 0,
            duration = 600,
            root, width, height, tree, svg, diagonal;

        $scope.$watch('treeViewData', function (newVal) {
          $timeout(function () {
            width = $(iElm[0].parentNode).width();
            height = 500 - margin.top;

            svg = d3.select(iElm[0]).append('svg')
              .attr("width", width)
              .attr("height", height)
              .attr("class", "overlay")
              .append("g")
              .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            tree = d3.layout.tree()
              .size([width, height])
              .separation(function separation(a, b) {
                return (a.parent == b.parent ? 1 : 2);
              });

            diagonal = d3.svg.diagonal()
              .projection(function (d) {
                return [d.x, d.y];
              });

            if (angular.isUndefined(newVal)) {
              return;
            }

            root = newVal;
            root.x0 = width / 2;
            root.y0 = 0;

            //root.children.forEach(ServiceNodeTopologyBackend._collapse);
            update(root);
          });
        });

        function update(source) {
          //some childrens are not rendered correctly after reload, deleting generated id fixed this
          ServiceNodeTopologyBackend._removeIds(root);

          // Compute the new tree layout.
          var nodes = tree.nodes(root),
              links = tree.links(nodes);

          // Normalize for fixed-depth.
          nodes.forEach(function (d) {
            d.y = d.depth * 110;
          });

          // Update the nodes…
          var node = svg.selectAll("g.node")
            .data(nodes, function (d) {
              return d.id || (d.id = ++i);
            });

          var div = d3.select("body").append("div")
            .attr("class", "tooltip")
            .style("opacity", 0);

          // Enter any new nodes at the parent's previous position.
          var nodeEnter = node.enter().append("g")
            .attr("class", "node")
            .attr("transform", function (d) {
              return "translate(" + source.x0 + "," + source.y0 + ")";
            })
            .on("click", click)
            .on("mouseover", function (d) {
              div.transition()
                .duration(200)
                .style("opacity", 0.9)
                .style("z-index", "10");

              div.html(d.tooltipHtml);

              div.style("left", (d3.event.pageX + 15) + "px")
                .style("top", (d3.event.pageY - 28) + "px");
            })
            .on("mouseout", function (d) {
              div.transition()
                .duration(600)
                .style("opacity", 0)
                .style("z-index", "-1");
            });

          nodeEnter.append("image")
            .attr("xlink:href", function (d) {
              return (d.image) ? d.image.url : defaultImage.url;
            })
            .attr("x", function (d) {
              return (d.image) ? d.image.xy : defaultImage.xy;
            })
            .attr("y", function (d) {
              return (d.image) ? d.image.xy : defaultImage.xy;
            })
            .attr("width", function (d) {
              return (d.image) ? d.image.wh : defaultImage.wh;
            })
            .attr("height", function (d) {
              return (d.image) ? d.image.wh : defaultImage.wh;
            });

          //IE does not support foreignObject
          if (ServiceNodeTopologyBackend.msieversion()) {
            nodeEnter.append("text")
              .attr("y", function (d) {
                return (d.text) ? d.text.y + 20 : defaultText.y + 20;
              })
              .attr("x", function (d) {
                return (d.text) ? d.text.x + 50 : defaultText.x + 50;
              })
              .attr("dy", ".35em")
              .attr("text-anchor", 'middle')
              .text(function (d) {
                return d.name;
              })
              .style("fill-opacity", 1);
          }
          else {
            nodeEnter.append("foreignObject")
              .attr("width", "100px")
              .attr("height", "50px")
              .style("text-align", "center")
              .attr("y", function (d) {
                return (d.text) ? d.text.y : defaultText.y;
              })
              .attr("x", function (d) {
                return (d.text) ? d.text.x : defaultText.x;
              })
              .append("xhtml:div")
              .attr("class", "nodeLabelOutContainer")
              .append("xhtml:p")
              .attr("class", "nodeLabelInContainer")
              .style("vertical-align", function (d) {
                return (d.text) ? d.text.align : defaultText.align;
              })
              .append("xhtml:p")
              .attr("class", "nodeLabel")
              .html(function (d) {
                return d.name;
              });
          }

          // Transition nodes to their new position.
          var nodeUpdate = node.transition()
            .duration(duration)
            .attr("transform", function (d) {
              return "translate(" + d.x + "," + d.y + ")";
            });

          nodeUpdate.select("text")
            .style("fill-opacity", 1);

          // Transition exiting nodes to the parent's new position.
          var nodeExit = node.exit().transition()
            .duration(duration)
            .attr("transform", function (d) {
              return "translate(" + source.x + "," + source.y + ")";
            })
            .remove();


          nodeExit.select("text")
            .style("fill-opacity", 1e-6);

          // Update the links…
          var link = svg.selectAll("path.link")
            .data(links, function (d) {
              return d.target.id;
            });

          // Enter any new links at the parent's previous position.
          link.enter().insert("path", "g")
            .attr("class", "link")
            .attr("d", function (d) {
              var o = {x: source.x0, y: source.y0};
              return diagonal({source: o, target: o});
            });

          // Transition links to their new position.
          link.transition()
            .duration(duration)
            .attr("d", diagonal);

          // Transition exiting nodes to the parent's new position.
          link.exit().transition()
            .duration(duration)
            .attr("d", function (d) {
              var o = {x: source.x, y: source.y};
              return diagonal({source: o, target: o});
            })
            .remove();

          // Stash the old positions for transition.
          nodes.forEach(function (d) {
            d.x0 = d.x;
            d.y0 = d.y;
          });
        }

        // Toggle children on click.
        function click (d) {
          ServiceNodeTopologyBackend._click(d);
          update(d);
        }
      }
    };
  });
});