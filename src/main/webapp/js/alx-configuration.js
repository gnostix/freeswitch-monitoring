$(function () {
    "use strict";

    $(document).ready ( function(){
		$('#table').DataTable();
        $.ajax({
            type: 'GET', // define the type of HTTP verb we want to use (POST for our form)
            url: '/configuration/fs-node/conn-data', // the url where we want to POST
            // url: 'http://fs-moni.cloudapp.net:8080/configuration/fs-node/conn-data', // the url where we want to POST
            //url: 'http://10.5.50.249:8080/configuration/fs-node/conn-data',
			dataType: 'json', // what type of data do we expect back from the server
            contentType: "application/json",
            encode: true
        })
            // using the done promise callback
            .done(function (data) {
                // log data to the console so we can see
                //console.log("on success get connections json" + JSON.stringify(data));

                $("#message").toggleClass('alert-info alert-success');
                $(jQuery.parseJSON(JSON.stringify(data))).each(function() {

                  //  console.log(this.ip + " - ---- -- " + this.port);
                    $('#table > tbody:last').append('<tr><td>'+this.ip+'</td>' +
                        '<td>'+this.port+'</td>' +
                        '<td><button type="button" class="btn btn-danger remove"' +
                        ' onclick ="delete_user($(this),\''+this.ip+'\')">Delete</button></td></tr>');
                });

            })
            // using the fail promise callback
            .fail(function(data) {
                // show any errors
                // best to remove for production
                console.log("on fail " + JSON.stringify(data));
            });
    });


    $(document).ready(function () {

        // process the form
        $('form#eslConnForm').submit(function (event) {
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
               // url: 'http://fs-moni.cloudapp.net:8080/configuration/fs-node/conn-data', // the url where we want to POST
                url: '/configuration/fs-node/conn-data', // the url where we want to POST
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
                        $( ".inputRes" ).replaceWith( data.message);
                        //this changes the class
                        $("#message").toggleClass('alert-info alert-success');
                        $('#table > tbody:last').append('<tr><td>'+formData.ip+'</td><td>'+formData.port+'</td><td><button type="button" class="btn btn-danger remove" onclick ="delete_user($(this),\''+formData.ip+'\')">Delete</button></td></tr>');

                    } else if(data.status===400){
                       // alert(data.status);
                        //this removes a class name
                        //$(".form-horizontal").remove();
                        //this replace text in a  class
                        $( ".inputRes" ).replaceWith( data.message);
                        //this changes the class
                        $("#message").toggleClass('alert-info alert-danger');

                    }

                })
                // using the fail promise callback
                .fail(function(data) {
                    // show any errors
                    // best to remove for production
                    console.log("on fail " + data);
                    $( ".inputRes" ).replaceWith( data.message );
                    $("#message").toggleClass('alert-info alert-success');
                });

            // stop the form from submitting the normal way and refreshing the page
            event.preventDefault();
        });

    });


});

function delete_user(row,ip)
{
    var formData = {
        "ip": ip
    };
    $.ajax({
        type: 'DELETE', // define the type of HTTP verb we want to use (POST for our form)
        url: '/configuration/fs-node/conn-data', // the url where we want to POST
        //url: 'http://fs-moni.cloudapp.net:8080/configuration/fs-node/conn-data', // the url where we want to POST
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
            row.closest('tr').remove();
        })
        // using the fail promise callback
        .fail(function(data) {
            // show any errors
            // best to remove for production
            console.log("on fail " + JSON.stringify(data));
        });



}
