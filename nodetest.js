var admin = require('firebase-admin');
var request = require('request');
// Create Google API client 
var googleMapsClient = require('@google/maps').createClient({
  key: 'AIzaSyBORcg3FJS35RW4G8bCddA-jcGyQc7M6Vk'
});
var polyline = require('polyline'); 
// var directionsService = new google.maps.DirectionsService(); 

var API_KEY = "AAAAocDkZCY:APA91bHJuCimRpfEHM-AFZrRatohUZI8X5g4iUQvbftj6bpyutq2n62wGAcpcZcCzTOnUCOq4mtHxt9ousC_qWS56V99uIitjjQcpjsoFQ5QyeXzkguArPptGZNDsADf6LeodtugoqlZ" 
// Your Firebase Cloud Messaging Server API key

// Fetch the service account key JSON file contents
// var serviceAccount = require("C:/Users/Girls Who Code/Desktop/wwm-admin-key.json");

// Initialize the app with a service account, granting admin privileges
admin.initializeApp({
  credential: admin.credential.cert({
    projectId: "wwithme-25166",
    clientEmail: "firebase-adminsdk-n79l8@wwithme-25166.iam.gserviceaccount.com",
    privateKey: "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDIY03gMxY3zMZt\n2FSZ/tYq6BlAxh7W41PO2EbHywY5egPk81ImuzQZGsyLkfyh7e8/De+VGTBjBN3C\n6WBkSDFaJFZL1Fusrq7hEYtLlT0dkx370RUP0GXBuz1MkA/3Bt3WRIcnWO3GC3kX\nf9yQTOaGT6g4zHzAnC34ksqnFsP18isqEGxnit/wpE9OBN0FTZq6wG+yxQFSfrtl\nAxy03q7BKVsiXVbQZQ3Yk4EzQ5JXxn98++bL6z0iY8gYl6DQm+XbLAzrhl7ElfRq\nKEv+7DtjqnTYiPxU+HG63hJC/nH1rTr16gvuN5QSOL0CcqFSUwCOr1HdJdf65GYZ\ne7VfvGCPAgMBAAECggEABYyKbLm9bpCJf70zCovXmzSPwaihOsrsvsuXM8a+RpUt\n/aCnSh9HYAwxiD19eDzbsflNN+m/2ney1GG2CgQQ9pgPAIAohO2jAuSDeo8iNB5A\nzecj5PfP+YwzASdag2xGdFdNdTXgz7a9nnyR7p6fVS5oUCC8sgpUU4/Tc/3Zy1Ou\n21YzRetG1HyxKvSyvIITxmihWlv3BGCUTS+1p3jvFtCaj+DKyiY5UxEbMYwSFVxO\n+Zco/4sdtASGSdo4MAdF+sBv2QVEto/KMTnj/FnUuB1mktwygBbC7fsUYGzpnwC6\n2l2QhI4GCd3VGfY5m0kHLVQBh+qgdj817yUpttFtsQKBgQDv6eIWrk2AozWteSdq\n/nU9YxgiuGem+n9zC7PqL3G8gI5504fHslE2Px0RH4L33YCetbzNekCiaAKSYn2m\ngEQ04AhTvSFpH4J0AEfao/D6MSYo/T6olkG2BpX2Abp9K1j+nqwjd1pzY0TVRSrj\nCqvabJcO2EIRzyizaXjiygaFSwKBgQDV0vZjXJGfNXZpOwNZmAZPnnhREf12SEFR\nILasy+4i+4cbPqzIT/vEO5+l0HwZZrHT++2mrVdCGANU+VhD6daIk8RH1/aSz/HS\nibBPhdV34XlAtD+VcI54rfX2Lnaeh02ZD84sxzFmNuYOs50eYl2Bb1fJt2VioHdX\ngtRDfmo7TQKBgFqOPFF1Tzmr4Rnubgt7qoMWQUjxs+YaDTs+gpV1fQe6uoyPZGPj\nEbZZEj6hV0z5pY/CZ1Zju7vRaI8ab/UF2zjeCyaYb+D/DL1+UaDcWpwFKzMzi5AU\nzw7G45msw1h1oSzSdYUZiuGc6aFabzqtj3ptnIL069NPDzzRJdjsipOLAoGACnrI\ncJ481Ny97PJje6SjyostwmtrTLzF1sAPs/Bav4QsXv13YmnpIPj7HhlVu7j4xjb2\nstCUEj0zDJsb5Fg1l4QGfq7q+s8OPw9YgM1CZRm745vx8dUOZoPAJkyoq6Rd/T51\nEGTC2wudwuKytKaFhq56UvyWD3tl3fgfhx9O/ZECgYEA79yFJjbH6PLa5BN7nJ38\n31IAeA6BOsdnbAszGxybzF8jwHlGB8zlO1goIQBFgBkgj38UcOf2/8dXix796uXg\nDz4+9AlvQsJUbtknN/8/MYeGv/g3NQL5ekkczgpJ41W5hS7tkdrat6p8UpUlmiW4\nIaGYfVIwetkuXgqMxouz7Q0=\n-----END PRIVATE KEY-----\n",
  }),
  databaseURL: "https://wwithme-25166.firebaseio.com/"
});
ref = admin.database().ref();

// Fetch user route 
function getRoute(originLat, originLong, destinationString) 
{
	var points = []; 
	
	// Create request 
	var request = {
		origin: [originLat, originLong], 
		destination: destinationString, 
		mode: 'walking'
	};
	
	// Send request 
	googleMapsClient.directions(request, function(result, status) {
		// Check for successful request 
		if (status.status == 200) {
			
			// Get all routes in the result 
			var routes = status.json.routes; 
			var routePoints = []; 
			// For each route 
			for (var i = 0; i < routes.length; i++) 
			{	
				// Find all points in this route 
				
				// Get the legs in this route 
				var legs = routes[i].legs;
				// For each leg  
				for (var j = 0; j < legs.length; j++) 
				{	
 					// Get all the steps in this leg  
					var steps = legs[j].steps; 
					
					// For each step 
					for (var k = 0; k < steps.length; k++) 
					{
 						// Get the points in the polyline for this step
						var decodedPolylinePoints = polyline.decode(steps[k].polyline.points); 
						for (var m = 0; m < decodedPolylinePoints.length; m++) 
						{
							// Add each point in the polyline for this step to the points for the whole route 
							routePoints.push(decodedPolylinePoints[m]); 
						}
					}
				}
				
				// console.log("Points in this route: "); 
				// console.log(routePoints);  
				for (var i = 0; i < routePoints.length; i++) 
				{
					points.push(routePoints[i]); 
				} 
				
			}
		}
		else console.log("not possible bruh"); 
	}); 
	
	return points; 
}

function onSuccess()
{
  console.log("Yeah it worked!");
}

function listenForNotificationRequests() {
  var requests = ref.child('notificationRequests');
  requests.on('child_added', function(requestSnapshot) {
    var request = requestSnapshot.val();
    sendNotificationToUser(
      request.username, 
      request.message,
      request.titleText,
	  request.fromUser,
      function() {
        requestSnapshot.ref.remove();
      }) 
  }, function(error) {
    console.error(error);
  });
};

function sendNotificationToUser(username, message, titleText, fromU, onSuccess) {
  var payload = {
    data: {
      title: "Walk With Me: " + titleText,
      body: message,
	  fromUser: fromU
    }
  };

  var options = {
  priority: "high",
  timeToLive: 30
};

  admin.messaging().sendToDevice(username, payload, options)
    .then(function(response) {
      console.log("Sent to " + username);
      console.log("Message sent " + message);
      onSuccess();
      console.log("Successfully sent message! Server response:", response);
    })
    .catch(function(error) {
      console.log("Error sending message:", error);
    });
}

// start listening
// listenForNotificationRequests();
// console.log("starting now");
var userRoute = getRoute(42.283429, -71.653056, "90 West Main St., Westborough MA 01581");
var friendRoute = getRoute(42.285051,  -71.656522, "20 Fisher St, Westborough, MA 01581"); 
console.log("USER ROUTE: ");
console.log(userRoute); 
for (var i = 0; i < userRoute.length; i++) 
{
	console.log(userRoute[i]);
}
console.log("FRIEND ROUTE: "); 
console.log(friendRoute); 