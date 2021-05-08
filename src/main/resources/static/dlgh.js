///WMS村庄Layer图层
var wmscun = new ol.layer.Tile({


    title: '高速公路',
    source: new ol.source.TileWMS({
        ratio: 1,
        params: { 'LAYERS': 'show:0' },
        url: "http://47.95.218.128:8090/iserver/services/map-XASP/wms130",


        params: {
            LAYERS: "gsgl",
            TILED: true
        },
        // serverType: "iserver",
        crossOrigin: "anonymous"
    })
});

///WMS村庄Layer图层
var asfasf= new ol.layer.Tile({


    title: '底图',
    source: new ol.source.TileWMS({
        ratio: 1,
        params: { 'LAYERS': 'show:0' },
        url: "http://47.95.218.128:8090/iserver/services/map-XASP/wms130",


        params: {
            LAYERS: "liulandi",
            TILED: true
        },
        // serverType: "iserver",
        crossOrigin: "anonymous"
    })
});


///WMS村庄Layer图层
///WMS村庄Layer图层
var wmsqq = new ol.layer.Tile({


    title: '现有村镇',
    source: new ol.source.TileWMS({
        ratio: 1,
        params: { 'LAYERS': 'show:0' },
        url: "http://47.95.218.128:8090/iserver/services/map-XASP/wms130",


        params: {
            LAYERS: "css",
            TILED: true
        },
        // serverType: "iserver",
        crossOrigin: "anonymous"
    })
});



///WMS村庄Layer图层
var wmsc = new ol.layer.Tile({


    title: '主干路',
    source: new ol.source.TileWMS({
        ratio: 1,
        params: { 'LAYERS': 'show:0' },
        url: "http://47.95.218.128:8090/iserver/services/map-XASP/wms130",

        visible: false,
        params: {
            LAYERS: "gl",
            TILED: true
        },
        // serverType: "iserver",
        crossOrigin: "anonymous"
    })
});


///规划道路
var daolu = new ol.layer.Tile({


    title: '道路',
    source: new ol.source.TileWMS({
        ratio: 1,
        params: { 'LAYERS': 'show:0' },
        url: "http://47.95.218.128:8090/iserver/services/map-XASP/wms130",

        visible: false,
        params: {
            LAYERS: "dl",
            TILED: true
        },
        // serverType: "iserver",
        crossOrigin: "anonymous"
    })
});



///规划组团
var zu = new ol.layer.Tile({


    title: '环淀路',
    source: new ol.source.TileWMS({
        ratio: 1,
        params: { 'LAYERS': 'show:0' },
        url: "http://47.95.218.128:8090/iserver/services/map-XASP/wms130",

        visible: false,
        params: {
            LAYERS: "hl",
            TILED: true
        },
        // serverType: "iserver",
        crossOrigin: "anonymous"
    })
});

///规划组团
var zu1 = new ol.layer.Tile({


    title: 'R1快线',
    source: new ol.source.TileWMS({
        ratio: 1,
        params: { 'LAYERS': 'show:0' },
        url: "http://47.95.218.128:8090/iserver/services/map-XASP/wms130",

        visible: false,
        params: {
            LAYERS: "rr1",
            TILED: true
        },
        // serverType: "iserver",
        crossOrigin: "anonymous"
    })
});

//天地图注记
var tian_di_tu_annotation = new ol.layer.Tile({
    title: "地名标注",
    source: new ol.source.XYZ({
        url: 'http://t3.tianditu.com/DataServer?T=cva_w&x={x}&y={y}&l={z}&tk=c8f40527485d981d34c5369cb830104a',
        crossOrigin: "anonymous"
    })
});

//地图窗口图层组

(function() {
    var $vector = new ol.layer.Vector({
        source: new ol.source.Vector({ wrapX: false })
    });
    var map = new ol.Map({

        target: 'map',
        layers: [
            new ol.layer.Group({
                'title': 'Base maps',
                layers: [


                    new ol.layer.Tile({
                        title: 'Water color',
                        type: 'base',
                        visible: false,
                        source: new ol.source.Stamen({
                            layer: 'watercolor',
                            crossOrigin: 'anonymous'

                        })
                    }),

                    new ol.layer.Tile({

                        title: '遥感影像',
                        type: 'base',
                        visible: true,
                        source: new ol.source.TileWMS({
                            ratio: 1,
                            params: { 'LAYERS': 'show:0' },
                            url: "http://47.95.218.128:8090/iserver/services/map-XASP/wms130",
                            params: {
                                LAYERS: "rsmap",
                                TILED: true
                            },
                            // serverType: "iserver",
                            crossOrigin: "anonymous"
                        }),

                    }),


                    new ol.layer.Tile({
                        title: "天星影像",
                        type: 'base',
                        visible: true,
                        source: new ol.source.XYZ({
                            url: 'http://t3.tianditu.com/DataServer?T=img_w&x={x}&y={y}&l={z}&tk=c8f40527485d981d34c5369cb830104a',
                            crossOrigin: "anonymous"
                        })
                    }),

                    new ol.layer.Tile({
                        title: "路网",
                        type: 'base',
                        visible: true,
                        source: new ol.source.XYZ({
                            url: "http://t4.tianditu.com/DataServer?T=vec_w&x={x}&y={y}&l={z}&tk=c8f40527485d981d34c5369cb830104a",
                            crossOrigin: "anonymous"
                        })
                    })


                ]
            }),

            new ol.layer.Group({
                title: 'Overlays',
                layers: [
                    asfasf,zu, daolu, wmsc, wmscun, zu1, wmsqq,tian_di_tu_annotation
                ]
            }),
            $vector
        ],

        //加载瓦片时开启动画效果
        loadTilesWhileAnimating: true,


        //view范围
        view: new ol.View({
            projection: 'EPSG:3857',
            center: ol.proj.fromLonLat([116.019402, 38.980929]), //坐标转换
            zoom: 11
        })

    });
    var layerSwitcher = new ol.control.LayerSwitcher({
        tipLabel: 'Légende' // Optional label for button
    });


    map.addControl(layerSwitcher);

    ///比例尺
    var scaleControl = new ol.control.ScaleLine();
    map.addControl(scaleControl);
    //zoom控件
    zoomControl = new ol.control.Zoom();
    map.addControl(zoomControl);

    //zoomBar控件
    map.addControl(new ol.control.ZoomSlider({}));

    //清除
    var draw = null;

    function clear() {
        if (draw) map.removeInteraction(draw);
    }
    //创建一个当前要绘制的对象
    var sketch = new ol.Feature();
    //创建一个测量提示框对象
    var measureTooltipElement;
    //创建一个测量提示信息对象
    var measureTooltip;
    var currentFeature;


    //添加交互式绘图对象的函数
    function addInteraction(type) {
        //创建一个交互式绘图对象
        draw = new ol.interaction.Draw({
            //绘制的数据源
            source: $vector.getSource(),
            //绘制类型
            type: type,
            //样式
            style: new ol.style.Style({
                fill: new ol.style.Fill({
                    color: 'rgba(255,255,255,0.2)'
                }),
                stroke: new ol.style.Stroke({
                    color: 'rgba(0,0,0,0.5)',
                    lineDash: [10, 10],
                    width: 2
                }),
                image: new ol.style.Circle({
                    radius: 5,
                    stroke: new ol.style.Stroke({
                        color: 'rgba(0,0,0,0.7)'
                    }),
                    fill: new ol.style.Fill({
                        color: 'rgba(255,255,255,0.2)'
                    })
                })
            })
        });
        //将交互绘图对象添加到地图中
        map.addInteraction(draw);

        //创建测量提示框
        createMeasureTooltip();

        //定义一个事件监听
        var listener;
        //定义一个控制鼠标点击次数的变量
        var count = 0;
        //绘制开始事件
        draw.on('drawstart', function(evt) {
            //The feature being drawn.
            sketch = evt.feature;
            //提示框的坐标
            var tooltipCoord = evt.coordinate;
            //监听几何要素的change事件
            //Increases the revision counter and dispatches a 'change' event.

            listener = sketch.getGeometry().on('change', function(evt) {
                //The event target.
                //获取绘制的几何对象
                var geom = evt.target;
                //定义一个输出对象，用于记录面积和长度
                var output;
                if (geom instanceof ol.geom.Polygon) {
                    map.removeEventListener('singleclick');
                    map.removeEventListener('dblclick');
                    //输出多边形的面积
                    output = formatArea(geom);
                    //Return an interior point of the polygon.
                    //获取多变形内部点的坐标
                    tooltipCoord = geom.getInteriorPoint().getCoordinates();
                } else if (geom instanceof ol.geom.LineString) {
                    //输出多线段的长度
                    output = formatLength(geom);
                    //Return the last coordinate of the geometry.
                    //获取多线段的最后一个点的坐标
                    tooltipCoord = geom.getLastCoordinate();
                    tooltipCoord[0] = tooltipCoord[0] - 1500;
                    tooltipCoord[1] = tooltipCoord[1] - 1500;
                }

                //设置测量提示框的内标签为最终输出结果
                measureTooltipElement.innerHTML = output;
                //设置测量提示信息的位置坐标
                measureTooltip.setPosition(tooltipCoord);
            });

            // //地图单击事件
            map.on('singleclick', function(evt) {
                //设置测量提示信息的位置坐标，用来确定鼠标点击后测量提示框的位置
                measureTooltip.setPosition(evt.coordinate);
                //如果是第一次点击，则设置测量提示框的文本内容为起点
                if (count == 0) {
                    measureTooltipElement.innerHTML = "起点";
                }
                //根据鼠标点击位置生成一个点
                var point = new ol.geom.Point(evt.coordinate);
                //将该点要素添加到矢量数据源中
                $vector.getSource().addFeature(new ol.Feature(point));
                //更改测量提示框的样式，使测量提示框可见
                measureTooltipElement.className = 'tooltip tooltip-static';
                //创建测量提示框
                createMeasureTooltip();
                //点击次数增加
                count++;
            });

            //地图双击事件
            map.on('dblclick', function(evt) {
                //根据
                var point = new ol.geom.Point(evt.coordinate);
                $vector.getSource().addFeature(new ol.Feature(point));
            });
        }, this);
        //绘制结束事件
        draw.on('drawend', function(evt) {
            count = 0;
            //设置测量提示框的样式
            measureTooltipElement.className = 'tooltip tooltip-static';
            //Set the offset for this overlay.
            //设置偏移量
            measureTooltip.setOffset([0, -7]);
            sketch.ele = measureTooltipElement;
            //清空绘制要素
            sketch = null;
            //清空测量提示要素
            measureTooltipElement = null;
            //创建测量提示框
            createMeasureTooltip();
            //Removes an event listener using the key returned by on() or once().
            //移除事件监听
            ol.Observable.unByKey(listener);
            //移除地图单击事件
            map.removeEventListener('singleclick');
        }, this);
    }

    //创建测量提示框
    function createMeasureTooltip() {
        //创建测量提示框的div
        measureTooltipElement = document.createElement('div');
        measureTooltipElement.setAttribute('id', 'lengthLabel');
        measureTooltipElement.style.opacity = 1;
        //设置测量提示要素的样式
        measureTooltipElement.className = 'tooltip tooltip-measure';
        //创建一个测量提示的覆盖标注
        measureTooltip = new ol.Overlay({
            element: measureTooltipElement,
            offset: [0, -15],
            positioning: 'bottom-center'
        });
        //将测量提示的覆盖标注添加到地图中
        map.addOverlay(measureTooltip);
    }

    //格式化测量长度
    var formatLength = function(line) {
        //定义长度变量
        var length;
        //如果大地测量复选框被勾选，则计算球面距离
        //Return the length of the linestring on projected plane.
        //计算平面距离
        length = Math.round(line.getLength() * 100) / 100;
        //定义输出变量
        var output;
        //如果长度大于1000，则使用km单位，否则使用m单位
        if (length > 1000) {
            output = (Math.round(length / 1000 * 100) / 100) + ' ' + 'km'; //换算成KM单位
        } else {
            output = (Math.round(length * 100) / 100) + ' ' + 'm'; //m为单位
        }
        return output;
    };
    //格式化测量面积
    var formatArea = function(polygon) {
        //定义面积变量
        var area;
        //如果大地测量复选框被勾选，则计算
        area = polygon.getArea();
        //定义输出变量
        var output;
        //当面积大于10000时，转换为平方千米，否则为平方米
        if (area > 10000) {
            output = (Math.round(area / 1000000 * 100) / 100) + ' ' + 'km<sup>2</sup>';
        } else {
            output = (Math.round(area * 100) / 100) + ' ' + 'm<sup>2</sup>';
        }
        return output;
    };

    function polygon(on) {
        draw = new ol.interaction.Draw({
            source: $vector.getSource(),
            type: 'Polygon',
            style: new ol.style.Style({
                fill: new ol.style.Fill({
                    color: 'blue'
                }),
                stroke: new ol.style.Stroke({
                    color: '#ffcc33',
                    width: 2
                }),
                image: new ol.style.Circle({
                    radius: 7,
                    fill: new ol.style.Fill({
                        color: '#ffcc33'
                    })
                })
            })
        });
        draw.on('drawend', function(evt) {
            currentFeature = evt.feature;
        }, this);
        map.addInteraction(draw);
    }
    // 2.绘制线
    function linedraw() {
        draw = new ol.interaction.Draw({
            source: $vector.getSource(),
            type: 'LineString',
            style: new ol.style.Style({
                fill: new ol.style.Fill({
                    color: 'rgba(255, 255, 255, 0.2)'
                }),
                stroke: new ol.style.Stroke({
                    color: '#ffcc33',
                    width: 2
                }),
            })
        })
        draw.on('drawend', function(evt) {
            currentFeature = evt.feature;
        }, this);
        map.addInteraction(draw);
    }
    // 3.绘制点
    function point() {
        draw = new ol.interaction.Draw({
            source: $vector.getSource(),
            type: 'Point',
            zIndex: -99,
            style: new ol.style.Style({
                image: new ol.style.Circle({
                    radius: 5,
                    stroke: new ol.style.Stroke({
                        color: 'rgba(0,0,0,0.7)'
                    }),
                    fill: new ol.style.Fill({
                        color: 'rgba(255,255,255,0.2)'
                    })
                })
            })
        })
        draw.on('drawend', function(evt) {
            currentFeature = evt.feature;
        }, this);
        map.addInteraction(draw);
    }
    //圆
    function circle() {
        draw = new ol.interaction.Draw({
            source: $vector.getSource(),
            type: 'Circle'
        })
        draw.on('drawend', function(evt) {
            currentFeature = evt.feature;
        }, this);
        map.addInteraction(draw);
    }



    //面积
    $("#measure_area").click(function() {
        clear();
        addInteraction('Polygon');
    });
    //距离
    $("#measure_distance").click(function() {
        clear();
        addInteraction('LineString');
    });
    //结束测量
    $("#measure_down").click(function() {
        clear();
    });
    //点
    $("#drawPoint").click(function() {
        clear();
        point();
    });
    // 线
    $("#drawLine").click(function() {
        clear();
        linedraw();
    });
    //面
    $("#drawPolygon").click(function() {
        clear();
        polygon();
    });
    //圆
    $("#drawCircle").click(function() {
        clear();
        circle();
    });
    //取消
    $("#none").click(function() {
        if (currentFeature) $vector.getSource().removeFeature(currentFeature);
    });
    //清除
    $("#clear").click(function() {
        $vector.getSource().clear();
    });

    //百度搜索
    $("#s_submit").click(function() {
        $.ajax({
            //url中的参数含义参见百度地图官网webAPI文档
            url: 'http://api.map.baidu.com/place/v2/search?query=' + $("input[type=search]").val() + '&tag=&region=河北&output=json&ak=NpRgVsjFQU8cwHM8gXuRbxtpQpYkCZTb',
            type: "GET",
            async: false,
            dataType: "jsonp",
            jsonp: "callback",
            jsonpCallback: "callback",
            contentType: "application/json;charset=utf-8",
            success: function(data) {
                if (data && data.status == 0 && data.results && data.results.length > 0) {
                    map.getView().setCenter(ol.proj.transform([data.results[0].location.lng, data.results[0].location.lat], 'EPSG:4326', 'EPSG:3857'));
                    map.getView().setZoom(13)
                }
            },
            error: function(data) {
                console.log(data)
            }
        })
    });


    $.ajax({
        //url中的参数含义参见百度地图官网webAPI文档
        url: 'http://api.map.baidu.com/place/v2/suggestion?query=天安门&region=北京&city_limit=true&output=json&ak=NpRgVsjFQU8cwHM8gXuRbxtpQpYkCZTb',
        type: "GET",
        async: false,
        dataType: "jsonp",
        jsonp: "callback",
        jsonpCallback: "callback",
        contentType: "application/json;charset=utf-8",
        success: function(data) {
            console.log(data);
        },
        error: function(data) {
            console.log(data)
        }
    })



    //打印
    $("#export-png").click(function() {
        map.once('postcompose', function(event) {
            var canvas = event.context.canvas;
            canvas.toBlob(function(blob) {
                saveAs(blob, 'map.png');
            });
        });
        map.renderSync();
    });

    // document.getElementById('export-png').addEventListener('click', function() {

    //     map.once("postcompose", function(event) {
    //         var canvas = event.context.canvas;
    //         if (navigator.msSaveBlob) {
    //             navigator.msSaveBlob(canvas.msToBlob(), "map.png");
    //         } else {
    //             canvas.toBlob(function(blob) {
    //                 // 报错删除FileSaverJS
    //                FileSaverJS.saveAs(blob, "map.png");
    //             });
    //         }
    //     });
    //     map.renderSync();

    // });



    //openlayers 3例子   在4中animation改为 view.animate
    // //各定位点(中国省会城市)
    // var shenyang = ol.proj.fromLonLat([123.24, 41.50]);
    // var beijing = ol.proj.fromLonLat([116.28, 39.54]);
    // var shanghai = ol.proj.fromLonLat([121.29, 31.14]);
    // var wuhan = ol.proj.fromLonLat([114.21, 30.37]);
    // var guangzhou = ol.proj.fromLonLat([113.15, 23.08]);
    // var haikou = ol.proj.fromLonLat([110.20, 20.02]);
    // var xian = ol.proj.fromLonLat([108.93, 34.28]);

    // //旋转动画
    // document.getElementById('fly').onclick = function() {
    //     //旋转动画
    //     var rotate = ol.animation.rotate({
    //         //持续时间
    //         duration: 2000,
    //         //旋转角度
    //         rotation: -4 * Math.PI
    //     });
    //     //地图渲染前设置旋转动画效果(rotate)
    //     map.beforeRender(rotate);
    //     //定位
    //     view.setCenter(shenyang);
    // };


    //导览回雄安新区 坐标    view.animate  
    var location1 = ol.proj.fromLonLat([115.919402, 39.053929]);
    //飞行   其他 Pan to London Elastic to Moscow Bounce to Istanbul Spin to Rome Fly to Bern Rotate around Rome Take a tour
    function flyTo(location, done) {
        //持续时间
        var duration = 2000;
        var view = map.getView();
        var zoom = view.getZoom();
        var parts = 2;
        var called = false;

        function callback(complete) {
            --parts;
            if (called) {
                return;
            }
            if (parts === 0 || !complete) {
                called = true;
                done(complete);
            }
        }
        view.animate({
            center: location,
            duration: duration
        }, callback);
        view.animate({
            //缩放动画 变化范围
            zoom: zoom - 1,
            duration: duration / 2
        }, {
            //最后缩放级别
            zoom: 11,
            duration: duration / 2
        }, callback);
    }

    //////////////////////////////////////////////////////   方法3 button

    //      1
    // document.getElementById('fly').onclick = function() {

    // };
    //  2
    // function onClick(id, callback) {
    //     document.getElementById(id).addEventListener('click', callback);
    // }
    // onClick('fly', function() {

    // });


    //飞行旋转动画
    $("#fly").click(function() {
        flyTo(location1, function() {});
    });



})();