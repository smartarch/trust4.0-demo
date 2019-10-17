'use strict';

require('./extensions-common');

const em = require('../ivis-core/server/lib/extension-manager');
const path = require('path');

em.set('app.clientDist', path.join(__dirname, '..', 'client', 'dist'));

em.on('knex.migrate', async () => {
    const knex = require('../ivis-core/server/lib/knex');
    await knex.migrateExtension('trustvis', './knex/migrations').latest();
});

em.on('app.installRoutes', app => {
    const sim = require('./routes/sim');
    app.use('/sim', sim);
});

em.on('app.installAPIRoutes', app => {
    const cardApi = require('./routes/card');
    app.use('/', cardApi);
});

require('../ivis-core/server/index');

