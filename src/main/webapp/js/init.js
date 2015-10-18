 //initialise global vars.

//var path='http://fs-moni.cloudapp.net:8080';
  var  path='';
		//var pathP='http://fs-moni.cloudapp.net:8080';
 
 function logout() {
    console.log("logout username::");
   
     var logoutdata = {
          
        }
        $.ajax({
            url: path+'/user/logout',
            type: 'POST',
            dataType: 'json',
            encode: true
        })
            //contentType: "application/json",
                .done(function (data) {
                    console.log("Logout success:" + JSON.stringify(data));
					//save to localstorage (maybe check to save on sessionstorage
					// Store to local storage
					 localStorage.removeItem("username");
					 localStorage.removeItem("uid");
					 localStorage.removeItem("name");
					 localStorage.removeItem("company");
					 localStorage.removeItem("role");
	
                    //ajaxComplete(result, un);
                })
                .fail(function (data) {
                    console.log("error:" + data.responseText);
                    //te(data, un);
                })
}