/* implementing the POI functionalities */
var express = require('express');
var mysql = require('mysql');
var geo = require('geolib');
var fs = require('fs');

var connection = mysql.createConnection({
    host     : '127.0.0.1',
    user     : 'root',
    password : 'teste',
    database : 'pois'
});

client = mysql.createConnection(connection);

connection.connect(function(err) {
    // connected! (unless `err` is set)
    if(err) {
        console.log("Unable to connect to database" + err);
    }
});

exports.addPOI = function(req, res) {
    console.log("Received a request ---->");
    console.log(req.body);

    var poi  = {
        name: req.body.name,
        address: req.body.address,
        latt: req.body.latt,
        logt: req.body.logt,
        description: req.body.description,
        image: req.body.image,
        type: req.body.type
    };

    console.log(poi);

    var query = connection.query('INSERT INTO poi SET ?', poi, function(err, result, fields) {
        if(err) {
            res.send({status:"ERR", message:err.message + 'SQL = ' + query.sql});
            throw err;
        } else {
            res.send({status:"OK", lastId: result.insertId});
        }
    });
};

exports.listAll = function(req, res) {
    var query = connection.query('SELECT * FROM poi', function(err, rows) {
        if(err) {
            res.send({status:"ERR", message:err.message + 'SQL = ' + query.sql});
            throw err;
        } else {
            res.send({status:"OK", poi: rows});
        }
    });
};

exports.getPOI = function(req, res) {
    var id = req.params.id;

    var query = connection.query('SELECT * FROM poi WHERE id =' + id, function(err, rows) {
        if(err) {
            res.send({status:"ERR", message:err.message + 'SQL = ' + query.sql});
            throw err;
        } else {
            res.send({status:"OK", poi: rows});
        }
    });
};

exports.getPOIByDistance = function(req, res) {
    var distance = req.params.distance;
    var lat = req.params.latt;
    var lgt = req.params.logt;

    console.log(req.params);
    console.log(req.body);

    var result = [];

    var query = connection.query('SELECT * FROM poi', function(err, rows) {
        if(err) {
            res.send({status:"ERR", message:err.message + 'SQL = ' + query.sql});
            throw err;
        } else {
            // list the results and see if they are within a specific range
            for(var i in rows) {
                if(geo.getDistance({latitude: lat, longitude: lgt}, {latitude: rows[i].latt, longitude: rows[i].logt}) <= distance) {
                    console.log(rows[i].name);
                    result.push({"id": rows[i].id, "name": rows[i].name, "address": rows[i].address, "latt": rows[i].latt, "logt": rows[i].logt})
                }
            }
            res.send({status:"OK", poi: result});
        }
    });
};

exports.getPOIByType = function(req, res) {
    var distance = req.params.distance;
    var type = req.params.type;
    var lat = req.params.latt;
    var lgt = req.params.logt;

    console.log(req.params);
    console.log(req.body);

    var result = [];

    var query = connection.query('SELECT * FROM poi WHERE type =' + type, function(err, rows) {
        if(err) {
            res.send({status:"ERR", message:err.message + 'SQL = ' + query.sql});
            throw err;
        } else {
            // list the results and see if they are within a specific range
            for(var i in rows) {
                if(geo.getDistance({latitude: lat, longitude: lgt}, {latitude: rows[i].latt, longitude: rows[i].logt}) <= distance) {
                    console.log(rows[i].name);
                    result.push({"id": rows[i].id, "name": rows[i].name, "address": rows[i].address, "latt": rows[i].latt, "logt": rows[i].logt})
                }
            }
            res.send({status:"OK", poi: result});
        }
    });
};

exports.uploadFile = function(req, res) {
    console.log(req.files);
    console.log(req.files.image.originalname);
    console.log(req.files.image.path);
    fs.readFile(req.files.image.path, function (err, data){
        var dirname = "/Users/cserrao/Documents/Aulas/Desenvolvimento de Aplicações para Ambientes Móveis/aulasdaam/AndroidStudioSamples/guideMe/guideme";
        var newPath = dirname + "/images/" +   req.files.image.originalname;
        fs.writeFile(newPath, data, function (err) {
            if(err){
                res.json({'response':"Error"});
            }else {
                res.json({'response':"Saved"});

                fs.unlink(req.files.image.path, function (err) {
                   if(err) {
                       console.log("Error removing file...");
                   }
                });
            }
        });
    });
};

exports.getFile = function (req, res) {
    file = req.params.file;
    var dirname = "/Users/cserrao/Documents/Aulas/Desenvolvimento de Aplicações para Ambientes Móveis/aulasdaam/AndroidStudioSamples/guideMe/guideme";
    var img = fs.readFileSync(dirname + "/images/" + file);
    res.writeHead(200, {'Content-Type': 'image/jpg' });
    res.end(img, 'binary');
};