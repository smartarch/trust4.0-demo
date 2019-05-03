'use strict';

const router = require('../../ivis-core/server/lib/router-async').create();
const log = require('../../ivis-core/server/lib/log');
const passport = require('../../ivis-core/server/lib/passport');

router.postAsync('/play', passport.loggedIn, passport.csrfProtection, async (req, res) => {

    return res.json();
});

router.postAsync('/pause', passport.loggedIn, passport.csrfProtection, async (req, res) => {

    return res.json();
});

router.postAsync('/reset', passport.loggedIn, passport.csrfProtection, async (req, res) => {

    return res.json();
});


module.exports = router;
