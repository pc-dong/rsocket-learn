var express = require('express');
var router = express.Router();
const rsocketClient = require('../service/rsocket-client')

/* GET users listing. */
router.get('/log', function (req, res, next) {
    rsocketClient.log(req.query.message)
    res.send('respond with a resource');
});

router.get('/toUpperCase', function (req, res, next) {
    rsocketClient.toUpperCase(req.query.message)
        .subscribe({
            onError: error => res.send(error.message),
            onComplete: payload => {
                res.send(payload.data);
            }
        });
    // res.send('respond with a resource');
});

router.get('/splitString', function (req, res, next) {
    const array = new Array();
    rsocketClient.splitString(req.query.message)
        .subscribe({
            onError: error => res.send(error.message),
            onNext: payload => array.push(payload.data.toString()),
            onComplete: () => res.send(array),
            onSubscribe: subscription => subscription.request(100)
        });
});


router.post('/channelToUpperCase', function (req, res, next) {
    const array = new Array();
    rsocketClient.channelToUpperCase(req.body)
        .subscribe({
            onError: error => res.send(error.message),
            onNext: payload => {
                array.push(JSON.parse(payload.data.toString()).message);
            },
            onComplete: () => res.send(array),
            onSubscribe: subscription => subscription.request(100)
        });
    // res.send('respond with a resource');
});

module.exports = router;
