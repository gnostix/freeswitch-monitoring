//
// Pipelining function for DataTables. To be used to the `ajax` option of DataTables
//
$.fn.dataTable.pipeline = function ( opts ) {
    // Configuration options
	console.log("opts:"+JSON.stringify(opts));
    var conf = $.extend( {
      //  pages: 500,     // number of pages to cache
        url: '',  
		//columns: opts.columns,// script url
       // data: opts.data,   // function or object with parameters to send to the server
       // dataSrc:opts.dataSrc,             // matching how `ajax.data` works in DataTables
        method: 'GET' // Ajax HTTP method
    }, opts );
 console.log("opts dataSrc:"+JSON.stringify(opts.dataSrc));
    // Private variables for storing the cache
    var cacheLower = -1;
    var cacheUpper = null;
    var cacheLastRequest = null;
    var cacheLastJson = null;
 
    return function ( request, drawCallback, settings ) {
        var ajax          = false;
        var requestStart  = request.start;
        var drawStart     = request.start;
        var requestLength = request.length;
        var requestEnd    = requestStart + requestLength;
         
        if ( settings.clearCache ) {
            // API requested that the cache be cleared
            ajax = true;
            settings.clearCache = false;
        }
        else if ( cacheLower < 0 || requestStart < cacheLower || requestEnd > cacheUpper ) {
            // outside cached data - need to make a request
			console.log("cache request");
            ajax = true;
        }
        else if ( JSON.stringify( request.order )   !== JSON.stringify( cacheLastRequest.order ) ||
                  JSON.stringify( request.columns ) !== JSON.stringify( cacheLastRequest.columns ) ||
                  JSON.stringify( request.search )  !== JSON.stringify( cacheLastRequest.search )
        ) {
            // properties changed (ordering, columns, searching)
            ajax = true;
        }
         
        // Store the request for checking next time around
        cacheLastRequest = $.extend( true, {}, request );
 
        if ( ajax ) {
            // Need data from the server
            if ( requestStart < cacheLower ) {
                requestStart = requestStart - (requestLength*(conf.pages-1));
 
                if ( requestStart < 0 ) {
                    requestStart = 0;
                }
            }
             
            cacheLower = requestStart;
            cacheUpper = requestStart + (requestLength * conf.pages);
 
            request.start = requestStart;
            request.length = requestLength*conf.pages;
 
            // Provide the same `data` options as DataTables.
            if ( $.isFunction ( conf.data ) ) {
                // As a function it is executed with the data object as an arg
                // for manipulation. If an object is returned, it is used as the
                // data object to submit
                var d = conf.data( request );
                if ( d ) {
                    $.extend( request, d );
                }
            }
            else if ( $.isPlainObject( conf.data ) ) {
                // As an object, the data given extends the default
                $.extend( request, conf.data );
            }
				//console.log("lazyloading conf.dataSrc:"+JSON.stringify(request));
				console.log("request:"+JSON.stringify(conf));
                settings.jqXHR = $.ajax( {
                "type":     conf.method,
                "url":      conf.url,
                "data":     conf.data,
				//"dataSrc":  conf.dataSrc,
                "dataType": "json",
                "cache":    false,
               // "columns": conf.columns,
				"success":  function ( json ) {
                    //cacheLastJson = $.extend(true, {}, json);
					//var arr = $.makeArray( json.payload );
					var arr = [];
					// Now it can be used reliably with $.map()
					$.map( json.payload, function( val, i ) {
					//console.log("val:"+val+" ::i:"+i);
					var m = {};
					var a=[];
					m.country=val;
					m.code=i;
					a.push(val,i);
					arr.push(a);
					});
					
					var j={
						"data":arr
					};
				cacheLastJson = $.extend(true, {}, j);
			//console.log("FFFFFFFFF:"+JSON.stringify(j));
				   
                    if ( cacheLower != drawStart ) {
						console.log("FFFFFFFFF:");
                        j.data.splice( 0, drawStart-cacheLower );
                    }
                   
				  // j.data.splice( requestLength, j.data.size );
				   j.data.splice( requestLength, j.data.length );
                    // console.log("requestLength:"+$.type(j));
				   drawCallback( j );
                }
            } );
        }
        else {
            j = $.extend( true, {}, cacheLastJson );
            j.draw = request.draw; // Update the echo for each response
            j.data.splice( 0, requestStart-cacheLower );
            j.data.splice( requestLength, j.data.length );
 
            drawCallback(j);
			
			//json = $.extend( true, {}, cacheLastJson );
           // json.draw = request.draw; // Update the echo for each response
           // json.data.splice( 0, requestStart-cacheLower );
           // json.data.splice( requestLength, json.data.length );
 
           // drawCallback(json);
        }
    }
};
 
// Register an API method that will empty the pipelined data, forcing an Ajax
// fetch on the next draw (i.e. `table.clearPipeline().draw()`)
$.fn.dataTable.Api.register( 'clearPipeline()', function () {
    return this.iterator( 'table', function ( settings ) {
        settings.clearCache = true;
    } );
} );
 
 