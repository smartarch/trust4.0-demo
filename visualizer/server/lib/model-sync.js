'use strict';

const log = require('../../ivis-core/server/lib/log');
const axios = require('axios');
const config = require('../../ivis-core/server/lib/config');

async function run() {
    try {
        // const sensorsResp = await axios.get(config.enforcer.url + 'XXX');
    } catch (exc) {
        log.error('SYNC', exc);
    }

    setTimeout(run, config.modelSync.period);
}

async function start() {
    log.info('SYNC', 'Starting model synchronizer');

    // noinspection JSIgnoredPromiseFromCall
    run();
}

module.exports = {
    start
};