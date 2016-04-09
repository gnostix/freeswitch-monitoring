(function ($) {
	$.fn.arcGauge = function (config) {
		this.each(function () {
			var gauge = {};
			// set container
			gauge.container = $(this);
			// get size
			var size = Math.max(gauge.container.outerWidth(), gauge.container.outerHeight())
			// settings
			gauge.settings = $.extend({}, {
				class:      'arc-gauge',
				width:      size,
				height:     size,
				startAngle: -120,
				endAngle:   +120,
				thickness:  8,
				value:      0,
				minValue:   0,
				maxValue:   100,
				transition: 1000,
				colors:     '#08c',
				bgColor:    '#eee',
				onchange:   function(value){}
			}, gauge.container.data(), config);
			// convert degrees to radians
			gauge.degToRad = function (degree) {
				return degree * Math.PI / 180;
			};
			// setup radius
			gauge.settings.radius = Math.min(gauge.settings.width, gauge.settings.height) / 2;
			// convenience method to map data to start/end angles
			gauge.pie = d3.layout.pie()
				.startAngle(gauge.degToRad(gauge.settings.startAngle))
				.endAngle(gauge.degToRad(gauge.settings.endAngle))
			// setup the arc
			gauge.arc = d3.svg.arc()
				.innerRadius(gauge.settings.radius - gauge.settings.thickness)
				.outerRadius(gauge.settings.radius)
				.cornerRadius(gauge.settings.thickness);
			// get data to draw in the right format
			gauge.getData = function (data) {
				var value = Math.max(Math.min(data, gauge.settings.maxValue), gauge.settings.minValue);
				// compute the start angle
				var start = gauge.degToRad(gauge.settings.startAngle);
				// compute the end angle
				var end = start + value * (gauge.degToRad(gauge.settings.endAngle) - start) / gauge.settings.maxValue;
				// return data
				return [{
					startAngle: start,
					endAngle: end,
					value: value
				}];
			};
			// get the value of the gauge
			gauge.get = function () {
				return gauge.svg.selectAll('path.arc-value').data()[0].value;
			};
			// set a new value of the gauge
			gauge.set = function (data) {
				// animate chart
				gauge.svg.selectAll('path.arc-value')
					.data(gauge.getData(data))
					.transition()
					.duration(gauge.settings.transition)
					.attrTween('d', gauge.arcTween)
					.style('fill', gauge.getColor);
				// trigger change
				if (gauge.settings.onchange)
					gauge.settings.onchange.call(this, data);
			};
			// draw
			gauge.arcTween = function (d, i) {
				var isOuter = this == gauge.svg.select('path.arc-background')[0][0];
				// compute the start angle
				var start = gauge.degToRad(gauge.settings.startAngle);
				// compute the end angle
				var end = gauge.degToRad(gauge.settings.endAngle);
				// interpolation
				var interpolate = d3.interpolate(this.previous || gauge.getData(gauge.settings[isOuter ? 'maxValue' : 'minValue'])[0], d);
				// cache value
				if (!isOuter) this.previous = d;
				// return
				return function (t) {
					return gauge.arc(interpolate(t));
				};
			};
			// gradient color based
			gauge.getColor = function (d, i) {
				if (!$.isPlainObject(gauge.settings.colors))
					return gauge.settings.colors;
				var color = gauge.settings.bgColor, percent = d.value / gauge.settings.maxValue;
				// looking for the color with the highest value in range
				for (var i in gauge.settings.colors) {
					if (percent < parseFloat(i)) break;
					color = gauge.settings.colors[i];
				}
				return color;
			};
			// initialization
			gauge.init = function () {
				// create the svg
				gauge.svg = d3.select(gauge.container[0])
					.append('svg')
					.attr('class', gauge.settings.class)
					.attr('width', gauge.settings.width)
					.attr('height', gauge.settings.height)
					.append('g')
					.attr('transform', 'translate(' + gauge.settings.width / 2 + ',' + gauge.settings.height / 2 + ')');
				// append the outer arc
				gauge.svg.append('path')
					.data(gauge.getData(gauge.settings.maxValue))
					.attr('class', 'arc-background')
					.attr('fill', gauge.settings.bgColor)
					.transition()
					.attrTween('d', gauge.arcTween);
				// append the inner arc
				gauge.svg.append('path')
					.data(gauge.getData(gauge.settings.minValue))
					.attr('class', 'arc-value')
					.attr('fill', gauge.settings.bgColor);
				// set first value
				gauge.set(gauge.settings.value);
				// export get function
				gauge.container[0].get = gauge.get;
				// export set function
				gauge.container[0].set = gauge.set;
			};
			// start
			gauge.init();
		});
		// return
		return this;
	};
}(jQuery));