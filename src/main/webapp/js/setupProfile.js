$(function () {
    "use strict";
	//paths for local tests and server
	//var path='http://fs-moni.cloudapp.net:8080';
	//var path='';

$(document).ready ( function(){
	//global setting override
        
		$.extend($.gritter.options, {
		   // class_name: 'gritter-light', // for light notifications (can be added directly to $.gritter.add too)
		    position: 'bottom-right', // possibilities: bottom-left, bottom-right, top-left, top-right
			fade_in_speed: 100, // how fast notifications fade in (string or int)
			fade_out_speed: 100, // how fast the notices fade out
			time: 5000 // hang on the screen for...
		});
        
	var table = $('#table').DataTable();	 
	// dialog properties
         $("#codesdialog").dialog({
            minWidth: 900,
            maxHeight: 600,
            autoOpen: false,
            modal: true
         });

		 
		getFileCodes();
			//find inner class
			//$('#tableRowCodes').find('tablePanelCode').css('font-size','22px');
});

$(function () {
    'use strict';
    // Change this to the location of your server-side upload handler:
   // var path='http://fs-moni.cloudapp.net:8080';
	//var url = '/configuration/dialcodes';
	var url = path+'/configuration/dialcodes';
    $('#fileupload').fileupload({
        url: url,
		 acceptFileTypes: '/(\.|\/)(png)$/i',
    // The maximum allowed file size in bytes:
		maxFileSize: 1000000, // 1 MB
        dataType: 'json',
        done: function (e, data) {
		
		//growl message
		console.log(":PPPPPPPPPPPKKK::"+JSON.stringify(data));
		$.gritter.add({
				// (string | mandatory) the heading of the notification
				title: data.result.message,
				// (string | mandatory) the text inside the notification
				text: 'You have uplaoded a file with total number of '+ data.result.payload+ ' codes',
				// (string | optional) the image to display on the left
				image: 'images/growl/confirm.png',
				// (bool | optional) if you want it to fade out on its own or just sit there
				sticky: false,
				//position: 'bottom-right',
				// (int | optional) the time you want it to be alive for before fading out
				time: ''
			});

		//
		 $("#fUpload").hide();
		 getFileCodes();
		  
          //  $.each(data.result.files, function (index, file) {
			//	console.log(":file.name::"+file.name);
            //    $('<p/>').text(file.name).appendTo('#files');
          //  });
        },
        progressall: function (e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            $('#progress .progress-bar').css(
                'width',
                progress + '%'
            );
        }
    }).prop('disabled', !$.support.fileInput)
        .parent().addClass($.support.fileInput ? undefined : 'disabled');
});

$(document).on('click', '.viewFile', function(e) {
	var r = $(this).closest('.panelFile').find('.filenameText');
 var link = $(this).parent().find('.filenameText').text();
console.log(r.text()); //"Hello"


$('#codesTable').DataTable( {
     "destroy": true,
	 "ajax": {
     "url":  path+'/configuration/dialcodes/'+r.text(),
     "dataSrc": function ( json ) {
	  // console.log(json.payload);
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
					 console.log(j.data);
       return j.data;
      }
    }
} );

/*
//lazyloading
 var table = $('#codesTable').DataTable( {
        destroy: true,
		"processing": true,
        "serverSide": true,
        "ajax": $.fn.dataTable.pipeline( {
            url: path+'/configuration/dialcodes/'+r.text(),
            "dataSrc" : "response.payload",
			data: "payload",
			"columns": [
				{ "payload": "country" },
				{ "payload": "code" }
			    ]
			//pages: 5 // number of pages to cache
        } )
		
    } );
*/	
	
//
 //var table = $('#codesTable').DataTable();
       // table.clear().draw();

      //  getCodeDetails(table,r.text());
        //open dialog
        $('#codesdialog').dialog('open');
});

$(document).on('click', '.deleteFile', function(e) {
  console.log("deleteFile");
  	var r = $(this).closest('.panelFile').find('.filenameText');
 var link = $(this).parent().find('.filenameText').text();
console.log("deleteFile::"+r.text()); //Hello"
	$.ajax({
                type: 'DELETE', // define the type of HTTP verb we want to use (POST for our form)


                // url: '/configuration/dialcodes', 
				url: path+'/configuration/dialcodes/'+r.text(),data: r.text(), // our data object
                dataType: 'json', // what type of data do we expect back from the server
                contentType: "application/json",
                encode: true
            })
                // using the done promise callback
                .done(function (data) {
                    // log data to the console so we can see
                    console.log("on success " + JSON.stringify(data));
                    // here we will handle errors and validation messages
                    if(data.status===200){
                        //this removes a class name
                        $("."+r.text()).remove();
                        
                    } else if(data.status===400){
						console.log("400");
                        //alert(data.message);
                        //this removes a class name
                        //$(".form-horizontal").remove();
                        //this replace text in a  class
                        //$( ".inputRes" ).replaceWith( data.message);
                        //this changes the class
                       // $("#message").toggleClass('alert-info alert-danger');
						$("#fUpload").show();
                    }
					getFileCodes();
					
					//delete green bar
					$('#progress .progress-bar').css('width','0%');
                })
                // using the fail promise callback
                .fail(function(data) {
                   // alert(data.message);
					// show any errors
                    // best to remove for production
                    console.log("on fail " + JSON.stringify(data));
                    $( ".inputRes" ).replaceWith( data.message );
                    $("#message").toggleClass('alert-info alert-danger');
					if(data.status===401){
					//alert(JSON.stringify(data));
					window.location = "index.html";
					}
                });


	});
 
 function getFileCodes() {
	 
	 
	 $.ajax({
            type: 'GET', // define the type of HTTP verb we want to use (POST for our form)
           // url: '/configuration/dialcodes', 
            url: path+'/configuration/dialcodes', // the url where we want to POST
           dataType: 'json', // what type of data do we expect back from the server
            contentType: "application/json",
            encode: true
        })
            // using the done promise callback
            .done(function (data) {
                $( "#filecodes" ).empty();
				// log data to the console so we can see
                //console.log("on success get connections json" + JSON.stringify(data));
				 console.log("LENGTH:"+data.payload.length);
					   console.log("payload:"+data.payload);
					   
					//table.clear().draw();
					//show default all the time
					if (data.payload.length > 1) {
						//var table = $('#table').DataTable();
						//empty div
						//$( "#filecodes" ).empty();
						 $("#fUpload").hide();
						 $("#filecodes").show();

                    $.each(data.payload, function (i, n) {
                         console.log("File: " + i + ", "+"Name:"  + n );
						//$( "#filecodes" ).text( "<p>"+n+"</p>" );
						if(n.fileName.toLowerCase() === "default".toLowerCase()) {
						var _innerHTML= '<div class="panelFile col-lg-3 col-md-3 ' + n + '"><div class="panel ghost-btn btn-white">'+
                           ' <div class="panel-heading">'+
                               ' <div class="row">'+
                                  '  <div class="col-xs-3">'+
                                      '  <i class="fa fa-tasks fa-3x"></i>'+
                                   ' </div>'+
                                   ' <div class="col-xs-9 text-right" id="fileDetails">'+                                        
                                    '<div class="medium">' + n.totalCodes + '</div> '+
                                    ' <div class="filenameText">' + n.fileName + '</div> '+
								  '  </div>'+
                               ' </div>'+
                           ' </div>'+
                           ' <a href="#">'+
                              '  <div class="panel-footer">'+
                                  '  <span class="pull-left"><i class="fa fa-eye fa-2x viewFile"></i></span>'+
                                  ' <div class="clearfix"></div>'+
                                '</div>'+
                            '</a>'+
						'</div> </div>';
						 $('#filecodes').append(_innerHTML);	
						} else {
                       
                     // var div = document.createElement('div');
						//div.setAttribute('class','col-lg-2 col-md-3 '+ n);
						//div.innerHTML = document.getElementById('blockOfStuff').innerHTML;
						//$( "#filenameText" ).text(i);
						//document.getElementById('filecodes').appendChild(div);
						 var _innerHTML= '<div class="panelFile col-lg-3 col-md-3 ' + n + '"><div class="panel ghost-btn btn-white">'+
                           ' <div class="panel-heading">'+
                               ' <div class="row">'+
                                  '  <div class="col-xs-3">'+
                                      '  <i class="fa fa-tasks fa-3x"></i>'+
                                   ' </div>'+
                                   ' <div class="col-xs-9 text-right" id="fileDetails">'+                                        
                                    '<div class="medium">' + n.totalCodes + '</div> '+
                                    ' <div class="filenameText">' + n.fileName + '</div> '+
								  '  </div>'+
                               ' </div>'+
                           ' </div>'+
                           ' <a href="#">'+
                              '  <div class="panel-footer">'+
                                  '  <span class="pull-left"><i class="fa fa-eye fa-2x viewFile"></i></span>'+
                                   ' <span class="pull-right"><i class="fa fa-times-circle-o fa-2x deleteFile"></i></span>'+
                                   ' <div class="clearfix"></div>'+
                                '</div>'+
                            '</a>'+
						'</div> </div>';
						 $('#filecodes').append(_innerHTML);
						}
						 //hide upload
						 // $("#fileUpload").hide();
                     });
                  } else {
					//show upload
						  $("#fUpload").show();
						  $("#filecodes").show();
						  var _innerHTML= '<div class="panelFile col-lg-3 col-md-3 default"><div class="panel ghost-btn btn-white">'+
                           ' <div class="panel-heading">'+
                               ' <div class="row">'+
                                  '  <div class="col-xs-3">'+
                                      '  <i class="fa fa-tasks fa-3x"></i>'+
                                   ' </div>'+
                                   ' <div class="col-xs-9 text-right" id="fileDetails">'+                                        
                                    '<div class="medium">8789</div> '+
                                    ' <div class="filenameText">def</div> '+
								  '  </div>'+
                               ' </div>'+
                           ' </div>'+
                           ' <a href="#">'+
                              '  <div class="panel-footer">'+
                                  '  <span class="pull-left"><i class="fa fa-eye fa-2x viewFile"></i></span>'+
                                  ' <div class="clearfix"></div>'+
                                '</div>'+
                            '</a>'+
						'</div> </div>';
						 $('#filecodes').append(_innerHTML);
				  }


               /*  $("#message").toggleClass('alert-info alert-success');
                $(jQuery.parseJSON(JSON.stringify(data))).each(function() {

                  //  console.log(this.ip + " - ---- -- " + this.port);
                    $('#table > tbody:last').append('<tr><td>'+this.ip+'</td>' +
                        '<td>'+this.port+'</td>' +
                        '<td><button type="button" class="btn btn-danger remove"' +
                        ' onclick ="delete_user($(this),\''+this.ip+'\')">Delete</button></td></tr>');
                }); */

            })
            // using the fail promise callback
            .fail(function(data) {
                // show any errors
                // best to remove for production
                console.log("on fail " + JSON.stringify(data));
				if(data.status===401){
					//alert(JSON.stringify(data));
					window.location = "index.html";
				}
            });
			
 }
 

function getCodeDetails(table,filename) {
        console.log("getCodeDetails --------------------------------");
        var pop = "";
        var message = "";
        
        $.ajax({
            type: 'GET', // define the type of HTTP verb we want to use (POST for our form)
           // url: '/configuration/dialcodes/', // the url where we want to POST
             url: path+'/configuration/dialcodes/'+filename, // the url where we want to POST
            dataType: 'json', // what type of data do we expect back from the server
            contentType: "application/json",
            encode: true
        })
            // using the done promise callback
            .done(function (result) {
			console.log("LENGTH:"+result.payload.size);
					 $.each(result.payload, function(key,value) {
					//console.log(key+':'+value);
					table.row.add([key,value]).draw();
					});

                /*if (result.payload.size > 0) {


                    $.each(result, function (i, n) {
                         console.log("Sensor Index: " + i + ", Sensor Name: " + n );

                        // for (var i = 0; i < n.length ; i++) {
                        // alert(n.uuid + " - ---- -- ");
                        // $('#example > tbody:last').append('<tr><td>'+n.uuid+'</td>' +
                        // '<td>'+n.fromUser+'</td>' +
                        // '<td>'+n.toUser+'</td></tr>');
                        // }
						//console.log(convertMillisecondsToCustomFormat(Date.parse(n.callerChannelAnsweredTime)).clock);
                        table.row.add([n,i]).draw();

                    });
                }
				*/

               
            })
            // using the fail promise callback
            .fail(function (data) {
                // show any errors
                // best to remove for production
                pop = "JSON.stringify(data)";
                console.log("on fail " + JSON.stringify(data));
				if(data.status===401){
					//alert(JSON.stringify(data));
					window.location = "index.html";
				}
            });


    }


});