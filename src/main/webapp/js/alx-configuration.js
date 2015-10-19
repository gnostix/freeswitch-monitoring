$(function () {
    "use strict";
	//paths for local tests and server
	//var path='http://fs-moni.cloudapp.net:8080';
	//var path='';

$(document).ready ( function(){
	var table = $('#tableConnections').DataTable();	 
		 
		$.ajax({
            type: 'GET', // define the type of HTTP verb we want to use (POST for our form)
           // url: '/configuration/fs-node/conn-data', // the url where we want to POST
            url: path+'/configuration/fs-node/conn-data', // the url where we want to POST
            //url: 'http://10.5.50.249:8080/configuration/fs-node/conn-data',
			dataType: 'json', // what type of data do we expect back from the server
            contentType: "application/json",
            encode: true
        })
            // using the done promise callback
            .done(function (data) {
                // log data to the console so we can see
                //console.log("on success get connections json" + JSON.stringify(data));
				 console.log("LENGTH:"+data.payload.length);
					   console.log("payload:"+data.payload);
					   
					//table.clear().draw();
					if (data.payload.length > 0) {
						//var table = $('#table').DataTable();
						 $("#tableRow").show();

                    $.each(data.payload, function (i, n) {
                         console.log("Sensor Index: " + i + ", Sensor Name: " + n.ip );

                       
                        table.row.add([n.ip, n.port,'<button type="button" class="btn btn-danger remove">Delete</button>']).draw();


                    });
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
				$.gritter.add({
					// (string | mandatory) the heading of the notification
					title: "ERROR",
					// (string | mandatory) the text inside the notification
					text: data.message,
					// (string | optional) the image to display on the left
					image: 'images/growl/error.png',
					// (bool | optional) if you want it to fade out on its own or just sit there
					sticky: false,
					//position: 'bottom-right',
					// (int | optional) the time you want it to be alive for before fading out
					time: ''
					});
				if(data.status===401){
					//alert(JSON.stringify(data));
					window.location = "index.html";
				}
            });
			
			
});


    //$(document).ready(function () {

        // process the form
        $('#eslConnForm').click(function (event) {
              // get the form data
            // there are many ways to get this data using jQuery (you can use the class or id also)
            var formData = {
                "ip": $('input[id=eslIPAddress]').val(),
                "port": parseInt($('input[id=eslPort]').val()),
                "password": $('input[id=eslPassword]').val()
            };

            console.log(JSON.stringify(formData));
            // process the form
            $.ajax({
                type: 'POST', // define the type of HTTP verb we want to use (POST for our form)


                url: path+'/configuration/fs-node/conn-data', // the url where we want to POST


               // url: '/configuration/fs-node/conn-data', // the url where we want to POST
                //url: 'http://10.5.50.249:8080/configuration/fs-node/conn-data', // the url where we want to POST
               	data: JSON.stringify(formData), // our data object
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
                        //$(".form-horizontal").remove();
                        //this replace text in a  class
                       // $( ".inputRes" ).replaceWith( data.message);
                        //this changes the class
                      //  $("#message").toggleClass('alert-info alert-success');
                      //  $('#table > tbody:last').append('<tr><td>'+formData.ip+'</td><td>'+formData.port+'</td><td><button type="button" class="btn btn-danger remove" onclick ="delete_user($(this),\''+formData.ip+'\')">Delete</button></td></tr>');
						$.gritter.add({
					// (string | mandatory) the heading of the notification
					title: "Success!",
					// (string | mandatory) the text inside the notification
					text: data.message,
					// (string | optional) the image to display on the left
					image: 'images/growl/confirm.png',
					// (bool | optional) if you want it to fade out on its own or just sit there
					sticky: false,
					//position: 'bottom-right',
					// (int | optional) the time you want it to be alive for before fading out
					time: ''
					});
						
						
						if($('#tableRow').css('display') == 'none') {
						var table = $('#tableConnections').DataTable();
						 $("#tableRow").show();
					    //table.clear().draw();
						  table.row.add([formData.ip, formData.port,'<button type="button"  class="btn btn-danger remove">Delete</button>']).draw();
						
						}
                    } else if(data.status===400){
                        //alert(data.message);
                        //this removes a class name
                        //$(".form-horizontal").remove();
                        //this replace text in a  class
                       // $( ".inputRes" ).replaceWith( data.message);
                        //this changes the class
                       // $("#message").toggleClass('alert-info alert-danger');
						
						$.gritter.add({
					// (string | mandatory) the heading of the notification
					title: "ERROR",
					// (string | mandatory) the text inside the notification
					text: data.message,
					// (string | optional) the image to display on the left
					image: 'images/growl/error.png',
					// (bool | optional) if you want it to fade out on its own or just sit there
					sticky: false,
					//position: 'bottom-right',
					// (int | optional) the time you want it to be alive for before fading out
					time: ''
					});

                    }

                })
                // using the fail promise callback
                .fail(function(data) {
                   // alert(data.message);
					// show any errors
                    // best to remove for production
                    console.log("on fail " + JSON.stringify(data));
                    //$( ".inputRes" ).replaceWith( data.message );
                   // $("#message").toggleClass('alert-info alert-danger');
					
					$.gritter.add({
					// (string | mandatory) the heading of the notification
					title: "ERROR",
					// (string | mandatory) the text inside the notification
					text: data.statusText,
					// (string | optional) the image to display on the left
					image: 'images/growl/error.png',
					// (bool | optional) if you want it to fade out on its own or just sit there
					sticky: false,
					//position: 'bottom-right',
					// (int | optional) the time you want it to be alive for before fading out
					time: ''
					});
					
					if(data.status===401){
					//alert(JSON.stringify(data));
					window.location = "index.html";
					}
                });

            // stop the form from submitting the normal way and refreshing the page
            event.preventDefault();
        });

   // });




$('#tableConnections tbody').on( 'click', 'button', function() {
   // alert("The paragraph was tbody.");
	var table = $('#tableConnections').DataTable();
	//get the ip to delete
	var data = table.row( $(this).parents('tr') ).data();
       // alert( data[0]);
	table.row( $(this).parents('tr') ).remove().draw();
	var info = table.page.info();
	var count = info.recordsTotal;
	if(count===0) {
		$("#tableRow").hide();
		delete_user(data[0])
	}
	//alert(info);
	//number of rows
	//alert(count);
	
} );





function delete_user(ip)
{
	
	console.log("TABLE::")
    var formData = {
        "ip": ip
    };
    $.ajax({
        type: 'DELETE', // define the type of HTTP verb we want to use (POST for our form)
        //url: '/configuration/fs-node/conn-data', // the url where we want to POST
        url: path+'/configuration/fs-node/conn-data', // the url where we want to POST
        //url: 'http://10.5.50.249:8080/configuration/fs-node/conn-data', // the url where we want to POST
        data: JSON.stringify(formData), // our data object
        dataType: 'json', // what type of data do we expect back from the server
        contentType: "application/json",
        encode: true
    })
        // using the done promise callback
        .done(function (data) {
            // log data to the console so we can see
            console.log("on success delete " + JSON.stringify(data));
           // row.closest('tr').remove();
		  
        })
        // using the fail promise callback
        .fail(function(data) {
            // show any errors
            // best to remove for production
            console.log("on fail " + JSON.stringify(data));
			$.gritter.add({
					// (string | mandatory) the heading of the notification
					title: "ERROR",
					// (string | mandatory) the text inside the notification
					text: data.message,
					// (string | optional) the image to display on the left
					image: 'images/growl/error.png',
					// (bool | optional) if you want it to fade out on its own or just sit there
					sticky: false,
					//position: 'bottom-right',
					// (int | optional) the time you want it to be alive for before fading out
					time: ''
					});
			if(data.status===401){
					//alert(JSON.stringify(data));
					window.location = "index.html";
			}
        });



}

});