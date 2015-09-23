	$(function () {
    $('#heartbeat').highcharts({
    chart: {
		type: "line",
		//backgroundColor: "rgba(255, 255, 255, 0.65)",
		backgroundColor: "rgba(24, 11, 81, 0.85)",
		borderRadius: 5,
		borderColor: "#ffffff",
		borderWidth: 3
	},
	title: {
		//text: "Live Monitoring"
		text: '',
    style: {
        display: 'none'
    }
	},	
	credits: {
      enabled: false
	},
	yAxis: {
		title: {
		text: null
	},
		//type: "logarithmic",
		minorTickInterval: "auto",
		 gridLineWidth: 0,
          minorGridLineWidth: 0
	},
	colors: [
		"#67a8e6",
		"#434348",
		"#4ac831",
		"#f7a35c",
		"#8085e9",
		"#f15c80",
		"#e4d354",
		"#2b908f",
		"#f45b5b",
		"#91e8e1"
	],
	xAxis: {
		type: "datetime"
	},
	series: [
		{
			index: 0,
			 id: 'asr',
			 dashStyle: "Solid",
			marker: {
				"enabled": false
			},
			name: "ASR",
			data: [
				[
					(new Date()).getTime(),
					0
				]
			]
		},
		{
			index: 1,
			id:'acd',
			dashStyle: "Solid",
			marker: {
				"enabled": false
			},
			name: "ACD",
			data: [
				[
					(new Date()).getTime(),
					0
				]
			]
		},
		{
			index: 2,
			id:'rtp',
			dashStyle: "Solid",
			marker: {
				"enabled": false
			},
			name: "RTP Quality",
			data: [
				[
					(new Date()).getTime(),
					0
				]
			]
		}
	]
    });
	
	$('#basicstats').highcharts({
    chart: {
		type: "area",
		//backgroundColor: "rgba(255, 255, 255, 0.65)",
		//backgroundColor: "rgba(0, 0, 52, 0.85)",
		backgroundColor: "rgba(24, 11, 81, 0.85)",
		borderRadius: 5,
		borderColor: "#ffffff",
		borderWidth: 3
	},
	title: {
		//text: "Live Monitoring"
		text: '',
    style: {
        display: 'none'
    }
	},
	credits: {
      enabled: false
	},
	yAxis: {
		title: {
		text: null
	},
		//type: "logarithmic",
		minorTickInterval: "auto",
		 gridLineWidth: 0,
          minorGridLineWidth: 0
	},
	colors: [
		"#67a8e6",
		"#434348",
		"#4ac831",
		"#f7a35c",
		"#8085e9",
		"#f15c80",
		"#e4d354",
		"#2b908f",
		"#f45b5b",
		"#91e8e1"
	],
	xAxis: {
		type: "datetime"
	},
	series: [
		{
			index: 0,
			id: 'concurrentCalls',
			 dashStyle: "Solid",
			marker: {
				"enabled": false
			},
			name: "Concurrent Calls",
			data: [
				[
					(new Date()).getTime(),
					0
				]
			]
		},
		{
			index: 1,
			id: 'failedCalls',
			dashStyle: "Solid",
			marker: {
				"enabled": false
			},
			name: "Failed Calls",
			data: [
				[
					(new Date()).getTime(),
					0
				]
			]
		}
	]
    });
	
	$('#cpu').highcharts({
    chart: {
		type: "area",
		//backgroundColor: "rgba(255, 255, 255, 0.65)",
		//backgroundColor: "rgba(0, 0, 52, 0.85)",
		backgroundColor: "rgba(24, 11, 81, 0.85)",
		borderRadius: 5,
		borderColor: "#ffffff",
		borderWidth: 3
	},
	title: {
		//text: "Live Monitoring"
		text: '',
    style: {
        display: 'none'
    }
	},
	credits: {
      enabled: false
	},	
	yAxis: {
		title: {
		text: null
	},
		//type: "logarithmic",
		minorTickInterval: "auto",
		 gridLineWidth: 0,
          minorGridLineWidth: 0
	},
	colors: [
		"#67a8e6",
		"#434348",
		"#4ac831",
		"#f7a35c",
		"#8085e9",
		"#f15c80",
		"#e4d354",
		"#2b908f",
		"#f45b5b",
		"#91e8e1"
	],
	xAxis: {
		type: "datetime"
	},
	series: [
		{
			index: 0,
			id: 'cpuUsage',
			 dashStyle: "Solid",
			marker: {
				"enabled": false
			},
			name: "CPU Usage (%)",
			data: [
				[
					(new Date()).getTime(),
					0
				]
			]
		}
	]
    });
});
