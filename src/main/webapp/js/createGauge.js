
var gauges = [];


function createGauge(name, label, min, max, sizebias, gx, gy)
{
	
	var config = 
	{
		size: 120 + sizebias,
		label: label,
		min: undefined !== min ? min : 0,
		max: undefined !== max ? max : 100,
    coorx: gx,
    coory: gy
      };

      var range = config.max - config.min;

      gauges[name] = new Gauge(name + "GaugeContainer", config);
      gauges[name].render();

  }

 // function createPaths()
 // {
  //  createGauge("t1","T1",5,100,50,350,200);
  //  createGauge("t2","T2",30,100,0,100,50);
 // }

  function updateGauges(val)
  {
    for (var key in gauges)
    {
     // var value = getRandomValue(gauges[key]);
      gauges[key].redraw(val);
    }
  }


  function getRandomValue(gauge)
  {
    var overflow = 0; //10;
    return Math.floor(gauge.config.min - overflow + (gauge.config.max - gauge.config.min + overflow*2) *  Math.random());
}

function Gauge(placeholderName, configuration)
{

  this.placeholderName = placeholderName;
  var self = this;

  var pi = 2 * Math.PI;

  var degreeString = "00B0";
  var degreeSign =  String.fromCharCode(parseInt(degreeString, 16));

  var defaultColor = "#ff5f24";
  var newColor,
      hold;

  this.configure = function(configuration)
  {
    this.config = configuration;

    this.config.size = this.config.size;
    this.config.min = undefined !== configuration.min ? configuration.min : 0; 
    this.config.max = undefined !== configuration.max ? configuration.max : 100; 

    this.config.transitionDuration = configuration.transitionDuration || 500;

    this.config.gaugetype = configuration.gaugetype;
    this.config.coorx = configuration.coorx;
    this.config.coory = configuration.coory;
    this.config.cx = this.config.size / 2;
    this.config.cy = this.config.size / 2;
    this.config.inner = this.config.size / (25/6);
    this.config.outer = this.config.size / (25/9.5);
  };

  this.render = function()
  {
    var arc = d3.svg.arc()
          .innerRadius(this.config.inner)
          .outerRadius(this.config.outer)
          .startAngle(0);
    
    this.container = d3.select("#gauge")
          .append("div")
          .attr("id", this.config.label + "Temp")
          .attr("class", "tempContainer")
          .style("left", this.config.coorx + "px")
          .style("top", this.config.coory + "px")
          .style("position", "absolute");
    
    this.body = this.container
          .append("svg")
          .attr("width", this.config.size)
          .attr("height", this.config.size)
    
    var defs = this.body.append("defs");
    
    var filter = defs.append("filter")
        .attr("id", "drop-shadow")
        .attr("height", "130%");

    filter.append("feGaussianBlur")
        .attr("in", "SourceAlpha")
        .attr("stdDeviation", 2)
        .attr("result", "blur");

    filter.append("feOffset")
        .attr("in", "blur")
        .attr("dx", 0)
        .attr("dy", 0)
        .attr("result", "offsetBlur");
    var feMerge = filter.append("feMerge");

    feMerge.append("feMergeNode")
        .attr("in", "offsetBlur")
    feMerge.append("feMergeNode")
        .attr("in", "SourceGraphic");
    
    var pointerContainer = this.body.append("g")
                .attr("class", "pointerContainer")
                .attr("transform", "translate(" + this.config.size / 2 + "," + this.config.size / 2 + ")")
                .style("position", "absolute");
    
    pointerContainer
                .append("path")
                .datum({endAngle: pi})
                .style("fill", "rgba(245,245,245,0.95)")
                .attr("d", arc);

    pointerContainer
                .append("path")
                .datum({endAngle: this.valueToRadians(this.config.min)})
                .style("fill", "#ff5f24")
                .attr("class", "foreground")
                .attr("d", arc);

    pointerContainer
                .append("circle")
                .style("fill", "rgba(255, 255, 255, 0.1)")
                .style("class", "gaugebody")
                .style("filter", "url(#drop-shadow)")
                .attr("r", this.config.inner);

    var midValue = (this.config.min + this.config.max) / 2;

     var fontSize = Math.round(this.config.size / 15);
    pointerContainer
            .append("text")
            .attr("x", this.config.cx - (this.config.cx * 1.0))
            .attr("y", this.config.cy - (this.config.cy * 1.20))
                .attr("text-anchor", "middle")
                .attr("fill", "#58585b")
                .style("font-size", fontSize + "px");
               // .text('Current Temp.');

    fontSize = Math.round(this.config.size / 3);
    var current = pointerContainer
                  .append("text")
                  .attr("x", this.config.cx - (this.config.cx * 0.99))
                  .attr("y", this.config.cy - (this.config.cy * 0.75))
                  .attr("class", "current")
                  .attr("text-anchor", "middle")
                  .attr("fill", "rgba(255, 255, 255, 0.9)")
                  .style("font-size", fontSize + "px")
                  .text(this.config.min);

    //console.log(this.pointerContainer.select("d"));
    this.redraw(this.config.min, 0);
    };

    


    this.redraw = function(value, transitionDuration)
    {
        var degreeValue = this.valueToDegrees(value);
        var pointerValue = this.valueToRadians(value);
        redrawSize = this.config.size;
        var arc = d3.svg.arc()
          .innerRadius(this.config.inner)
          .outerRadius(this.config.outer)
          .startAngle(0);


        switch(true)
              {
                case (degreeValue >= 270):
                //Dark Red
                  newColor = 'rgba(129,20,41,0.9) ';
                  break;
                case (degreeValue >= 180):
                //Light Blue
                  newColor = 'rgba(255,95,36,0.9)';
                  break;
                case (degreeValue >= 90):
                //Light Red
                  newColor = 'rgba(0,158,255,0.9) ';
                  break;
                default:
                //Dark Blue
                  newColor = 'rgba(6,90,169,0.9) ';

                  break;
              }

          var current = this.body.select(".pointerContainer");

          current.select(".current")
            .transition()
            .text(value);

          foreground = this.body.select(".pointerContainer");

          foreground.select(".foreground")
            .transition()
            .duration(750)
            .styleTween("fill", function()
            {
              return d3.interpolate(newColor, defaultColor); 
            })
            .call(arcTween, pointerValue);
        //record color
        hold = defaultColor; 
        defaultColor = newColor; 
        newColor = hold;

    function arcTween(transition, newAngle) 
    {
      transition.attrTween("d", function(d) 
      {
        var interpolate = d3.interpolate(d.endAngle, newAngle);
        return function(t) 
        {
          d.endAngle = interpolate(t);
          return arc(d);
        };
      });
    }
    };

    this.valueToDegrees = function(value)
    {
      return Math.floor(value / this.config.max * 360);
    };
  
    this.valueToRadians = function(value)
    {
      return this.valueToDegrees(value) * Math.PI / 180;
    };


  this.configure(configuration);  
}