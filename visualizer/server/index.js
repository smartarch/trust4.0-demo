'use strict';

require('./extensions-common');

const em = require('../ivis-core/server/lib/extension-manager');
const path = require('path');
const sim = require('./routes/sim');

em.set('app.clientDist', path.join(__dirname, '..', 'client', 'dist'));

em.on('knex.migrate', async () => {
    const knex = require('../ivis-core/server/lib/knex');
    await knex.migrateExtension('trustvis', './knex/migrations').latest();
});

em.on('app.installRoutes', app => {
    app.use('/sim', sim);
});

require('../ivis-core/server/index');

