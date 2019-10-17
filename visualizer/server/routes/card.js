'use strict';

const router = require('../../ivis-core/server/lib/router-async').create();
const log = require('../../ivis-core/server/lib/log');
const passport = require('../../ivis-core/server/lib/passport');
const axios = require('axios');
const config = require('../../ivis-core/server/lib/config');

function getEnforcerUrl(path) {
    return config.enforcer.url + path;
}

router.getAsync('/access/:workerId', async (req, res) => {
    const resp = await axios.post(getEnforcerUrl('access/' + req.params.workerId));
    return res.json(resp.data);
});

module.exports = router;
