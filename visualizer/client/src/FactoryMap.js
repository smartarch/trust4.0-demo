'use strict';

import React, {Component} from "react";
import {withComponentMixins} from "../../ivis-core/client/src/lib/decorator-helpers";
import {withTranslation} from "../../ivis-core/client/src/lib/i18n";
import {withAsyncErrorHandler, withErrorHandling} from "../../ivis-core/client/src/lib/error-handling";
import {withPageHelpers} from "../../ivis-core/client/src/lib/page-common";
import {requiresAuthenticatedUser} from "../../ivis-core/client/src/lib/page";
import {Panel} from "../../ivis-core/client/src/lib/panel";
import {Button} from "../../ivis-core/client/src/lib/bootstrap-components";
import styles from "./FactoryMap.scss";
import axios from "../../ivis-core/client/src/lib/axios";
import {getUrl} from "../../ivis-core/client/src/lib/urls";
import moment from "moment";
import {SVG} from "../../ivis-core/client/src/ivis/SVG";
import factorySvg from "../images/factory.svg";

console.log(factorySvg);

const State = {
    // This has to be aligned with enforcer/src/main/scala/trust40/k4case/Timeline.scala
    START: 0,
    PLAYING: 1,
    PAUSED: 2,
    END: 3
}

const refreshInterval = 50;
const minorStepsInRefetchPeriod = 5;

@withComponentMixins([
    withTranslation,
    withErrorHandling,
    withPageHelpers,
    requiresAuthenticatedUser
])export default class FactoryMap extends Component {
    constructor(props) {
        super(props);

        this.state = {
            playState: State.START,
            ts: null
        };

        this.reset();

        this.refreshIntervalId = null;
    }

    reset() {
        this.keyframes = [];
        this.minorStep = 0;
        this.initialKeyframesFetched = false;
    }

    @withAsyncErrorHandler
    async play() {
        await axios.post(getUrl('sim/play'));

        this.setState({
            playState: State.PLAYING
        });
    }

    @withAsyncErrorHandler
    async stop() {
        await axios.post(getUrl('sim/reset'));

        this.setState({
            playState: State.START,
            ts: null
        });

        this.reset();
    }

    @withAsyncErrorHandler
    async pause() {
        await axios.post(getUrl('sim/pause'));

        this.setState({
            playState: State.PAUSED
        });
    }

    @withAsyncErrorHandler
    async getStatus() {
        const resp = await axios.get(getUrl('sim/status'));
        const ts = moment(resp.data.time);
        const kf = this.keyframes;

        if (kf.length > 0 && kf[kf.length - 1].ts.isAfter(ts)) {
            // This may happen if we receive status from previous epoch after stop. The status from the previous
            // epoch will set ts to something high. The reset status that we receive afterwards won't match
            // the last ts, which is what we check here.
            this.reset();

        } else {
            kf.push({
                ts,
                workers: resp.data.workers,
                permissions: resp.data.permissions
            });

            if (kf.length === 3 && !this.initialKeyframesFetched) {
                this.initialKeyframesFetched = true;
                this.minorStep = 0;
            }
        }
    }

    refresh() {
        if (!this.initialKeyframesFetched) {
            this.minorStep += 1;
            if (this.minorStep === minorStepsInRefetchPeriod) {
                this.minorStep = 0;
                // noinspection JSIgnoredPromiseFromCall
                this.getStatus();
            }

        } else {
            const kf = this.keyframes;

            if (kf.length >= 2) {
                const last = kf[0];
                const next = kf[1];
                const alpha = this.minorStep / minorStepsInRefetchPeriod;

                const interp = (last, next) => last * (1-alpha) + next * alpha;

                const ts = moment(interp(last.ts.valueOf(), next.ts.valueOf()));

                const workers = {};
                for (const key in last.workers) {
                    if (key in next.workers) {
                        const lastWorker = last.workers[key];
                        const nextWorker = next.workers[key];

                        workers[key] = {
                            position: {
                                x: interp(lastWorker.position.x, nextWorker.position.x),
                                y: interp(lastWorker.position.y, nextWorker.position.y)
                            }
                        };
                    }
                }


                this.setState({
                    ts,
                    workers,
                    permissions: last.permissions
                });
            }

            this.minorStep += 1;
            if (this.minorStep === minorStepsInRefetchPeriod) {
                this.minorStep = 0;

                if (kf.length >= 2) {
                    kf.shift();
                }

                // noinspection JSIgnoredPromiseFromCall
                this.getStatus();
            }
        }
    }

    componentDidMount() {
        this.getStatus();
        this.refreshIntervalId = setInterval(::this.refresh, refreshInterval);
    }

    componentWillUnmount() {
        clearInterval(this.refreshIntervalId);
    }

    render() {
        const t = this.props.t;
        const playState = this.state.playState;

        const ts = this.state.ts;
        const tsFormatted = ts ? ts.utc().format('HH:mm:ss') : null;

        if (this.state.permissions && this.state.permissions.length > 0) {
            console.log(this.state.permissions);
        }

        return (
            <Panel title={t('Factory Map')}>
                <div>
                    { playState === State.PLAYING &&
                        <Button className={`btn-primary ${styles.controlButton}`} icon="pause" onClickAsync={::this.pause} />
                    }
                    { (playState === State.START || playState === State.PAUSED) &&
                    <Button className={`btn-primary ${styles.controlButton}`} icon="play" onClickAsync={::this.play} />
                    }
                    { (playState === State.END) &&
                    <Button className={`btn-primary ${styles.controlButton}`} icon="play" disabled={true} />
                    }
                    <Button className={`btn-danger ${styles.controlButton}`} icon="stop" onClickAsync={::this.stop} disabled={playState === State.START}/>
                    <span className={styles.timestamp}>{tsFormatted}</span>
                </div>
                <div>
                    <SVG source={factorySvg}/>
                </div>
            </Panel>
        );
    }
}