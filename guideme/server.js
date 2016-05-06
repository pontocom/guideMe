var express = require('express'),
    bodyParser = require('body-parser'),
    poi = require('./poi.js');

var multer = require('multer');

var app = express();

app.use(bodyParser.urlencoded({
    extended: true
}));
app.use(bodyParser.json());

app.use(multer({dest: './tmp/'}))

// Configuration
app.use(express.static(__dirname + '/public'));

app.post('/poi', poi.addPOI);
app.get('/poi', poi.listAll);
app.get('/poi/:id', poi.getPOI);
app.get('/poi/range/:latt/:logt/:distance', poi.getPOIByDistance);
app.get('/poi/range/:latt/:logt/:distance/:type', poi.getPOIByType);

// handle image upload to the server
app.post('/upload', poi.uploadFile);
// get image from the server
app.get('/uploads/:file', poi.getFile);


var server = app.listen(3000, function () {

    var host = server.address().address
    var port = server.address().port

    console.log('Listening at http://%s:%s', host, port)

});
