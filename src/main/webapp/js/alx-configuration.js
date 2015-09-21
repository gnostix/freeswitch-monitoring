$(function () {
    "use strict";


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
                url: 'http://fs-moni.cloudapp.net:8080/configuration/fs-node/conn-data', // the url where we want to POST
                data: JSON.stringify(formData), // our data object
                dataType: 'json', // what type of data do we expect back from the server
                contentType: "application/json",
                encode: true
            })
                // using the done promise callback
                .done(function (data) {
                    // log data to the console so we can see
                    console.log("on success " + data);
                    // here we will handle errors and validation messages
                })
                // using the fail promise callback
                .fail(function(data) {
                    // show any errors
                    // best to remove for production
                    console.log("on fail " + data);
                    $(".form-horizontal").remove();
                    $( ".inputRes" ).replaceWith( data );
                });

            // stop the form from submitting the normal way and refreshing the page
            event.preventDefault();
        });

    });


});