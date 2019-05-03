'use strict';

import React, {Component} from "react";
import {withComponentMixins} from "../../ivis-core/client/src/lib/decorator-helpers";
import {withTranslation} from "../../ivis-core/client/src/lib/i18n";
import {withErrorHandling} from "../../ivis-core/client/src/lib/error-handling";
import {withPageHelpers} from "../../ivis-core/client/src/lib/page-common";
import {requiresAuthenticatedUser} from "../../ivis-core/client/src/lib/page";
import {Panel} from "../../ivis-core/client/src/lib/panel";
import {Button} from "../../ivis-core/client/src/lib/bootstrap-components";
import styles from "./FactoryMap.scss";
import axios from "../../ivis-core/client/src/lib/axios";
import {getUrl} from "../../ivis-core/client/src/lib/urls";

const State = {
    START: 0,
    PLAYING: 1,
    PAUSED: 2,
    END: 3
}

@withComponentMixins([
    withTranslation,
    withErrorHandling,
    withPageHelpers,
    requiresAuthenticatedUser
])export default class FactoryMap extends Component {
    constructor(props) {
        super(props);

        this.state = {
            playState: State.START
        }
    }
    
    async play() {
        await axios.post(getUrl('sim/play'));

        this.setState({
            playState: State.PLAYING
        });
    }
    
    async stop() {
        await axios.post(getUrl('sim/reset'));

        this.setState({
            playState: State.START
        })
    }

    async pause() {
        await axios.post(getUrl('sim/pause'));

        this.setState({
            playState: State.PAUSED
        })
    }

    render() {
        const t = this.props.t;
        const playState = this.state.playState;

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
                </div>
            </Panel>
        );
    }
}