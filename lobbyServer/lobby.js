// Load the TCP Library
net = require('net');
var port = 5000;
var maxConnections = 1000;
var timePerConnection = 10;
var minBroadcastRate = 2000;

var dataDelimiter = "#del#";
var clientDelimiter = "\n";

var hosts = {};
var connections = 0;
var sockets = [];

var broadcastTimer = false;

/*
get list at every broadcast:
{"m":"l"}#del#

add a server named suprt duper server with port 1337: 
{"m":"n","d"{"n":"Super duper server","p":"1337"}}#del#
*/


net.createServer(function (socket) {
 
  connections++;
  if(connections > maxConnections) {
    return sendFail(socket, "Lobby server is full");
  }

  socket.name = socket.remoteAddress + ":" + socket.remotePort
  socket.broadcastToMe = false;
  console.log("New connection: "+socket.name+", #"+connections);
  sockets.push(socket);

  var data = "";
 
  socket.on('data', function (chunk) {
    data += chunk.toString().replace(/(\r\n|\n|\r)/gm,""); // telnet is ok
    console.log(data);
    if(data.substring(data.length-dataDelimiter.length) == dataDelimiter) {
      handleRequest(socket, data.substring(0,data.length-dataDelimiter.length));
      data = "";
    }
  });
 
  socket.on('end', function () {
    removeSocket(socket);
    if(socket.hasOwnProperty('isServer')) {
      removeHost(socket.name);
    }
  });

  socket.on('close', function () {
    removeSocket(socket);
    if(socket.hasOwnProperty('isServer')) {
      removeHost(socket.name);
    }
  });

  socket.on('error', function(err) {
    if (err.errno === process.ECONNRESET) {
      console.log("jävla econnreset");
    } else {
      console.log("error: "+err);
    }
  })

}).listen(port);

function handleRequest(socket, data) {
  console.log(socket.name +" sent data "+data);
  var json;
  try {
    json = JSON.parse(data);
  } catch (err) {
    console.log("JSON parse error:" + err + "\n of data:"+data);
    socket.end("Error: bad json format: "+data+clientDelimiter);
    return;
  }
  if(json.hasOwnProperty('m')) {
    switch(json['m']) {
      case "l":
        socket.broadcastToMe = true;
        if(broadcastTimer === false) {
          broadcastList();
        }
        break;
      case "n":
        if(json.hasOwnProperty('d') === false) {
          return sendFail(socket, "missing data");
        }
        addNewServer(socket, json['d']);
        socket.broadcastToMe = false;
        break;
      case "r":
        if(json.hasOwnProperty('d') === false) {
          return sendFail(socket, "missing data");
        }
        updateServer(socket, json['d']);
        socket.broadcastToMe = false;
        break;
      default:
        socket.end("bad"+clientDelimiter);
        break;
    }
  } else {
    socket.end("missing message"+clientDelimiter);
  }
}

function removeSocket(socket) {
  if(sockets.indexOf(socket) >= 0) {
    sockets.splice(sockets.indexOf(socket), 1);
    connections--;
  }
}


function broadcastList() {
  var broadcastRate = getBroadcastRate();
  var i =0;
  sockets.forEach(function (socket) {
    if(socket.broadcastToMe === true) {
      socket.write(JSON.stringify({m:"l", d:{l:hosts, t:broadcastRate}})+"\n");
      i++;
    }
  });
  if(i == 0) {
    clearTimeout(broadcastTimer);
    broadcastTimer = false;
  } else {
    broadcastTimer = setTimeout(broadcastList, broadcastRate);
  }
}

function addNewServer(socket, json) {
  if(hosts.hasOwnProperty(socket.name)) {
    return sendFail(socket, "You alredy exists, change port");
  }
  if(json.hasOwnProperty["n"] === false || json.hasOwnProperty["p"] === false || json.hasOwnProperty["c"] === false || json.hasOwnProperty["m"] === false) {
    return sendFail(socket, "need name and port");
  }
  if(typeof json["n"] !== "string") {
    return sendFail(socket, "need name in string, not "+(typeof json["n"]));
  }
  if(typeof json["p"] !== "string" && typeof json["p"] !== "number") {
    return sendFail(socket, "need port in string/number, not "+(typeof json["p"]));
  }
  if(typeof json["c"] !== "string" && typeof json["c"] !== "number") {
    return sendFail(socket, "need count in string/number, not "+(typeof json["c"]));
  }
  if(typeof json["m"] !== "string" && typeof json["m"] !== "number") {
    return sendFail(socket, "need max count in string/number, not "+(typeof json["m"]));
  }
  for (var id in hosts) {
    if(id.substring(0, id.indexOf(":")) === socket.remoteAddress && hosts[id]["p"] === json["p"]+"") {
      return sendFail(socket, "Your server alredy exists, change port");
    }
  }

  hosts[socket.name] = {
    n:json["n"],
    c:json["c"]+"",
    m:json["m"]+"",
    p:json["p"]+""
  };
  socket.isServer = true;
  socket.write(JSON.stringify({m:"k"})+clientDelimiter);
}

function updateServer(socket, json) {
  if(!hosts.hasOwnProperty(socket.name)) {
    return sendFail(socket, "Your server dos not exist");
  }
  if(json.hasOwnProperty["c"] === true && (typeof json["c"] === "string" || typeof json["c"] === "number")) {
    hosts[socket.name]["c"] = json["c"];
  }
  if(json.hasOwnProperty["m"] === true && (typeof json["m"] === "string" || typeof json["m"] === "number")) {
    hosts[socket.name]["m"] = json["m"];
  }
  socket.write(JSON.stringify({m:"k"})+clientDelimiter);
}

function sendFail(socket, msg) {
  socket.end(JSON.stringify({m:"f", d:msg})+clientDelimiter);
}

function removeHost(id) {
  if(hosts.hasOwnProperty(id)) {
    delete hosts[id];
  }
}

function getBroadcastRate() {
  if(connections*timePerConnection < minBroadcastRate) {
    return minBroadcastRate;
  }
  return connections*timePerConnection;
}

// Put a friendly message on the terminal of the server.
console.log("Lobby server running at port "+port+"\n");