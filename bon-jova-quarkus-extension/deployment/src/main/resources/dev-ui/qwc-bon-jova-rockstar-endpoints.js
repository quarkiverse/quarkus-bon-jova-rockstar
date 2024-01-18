import {html, LitElement} from 'lit';
import {rockFiles} from 'build-time-data';

/**
 * This component shows the Rockstar endpoints.
 */
export class QwcBonJovaRockstarEndpoints extends LitElement {

    constructor() {
        super();
        this._rockFiles = rockFiles;
    }

    render() {
        if (this._rockFiles) {
            return html`<ul>
                ${this._rockFiles.map((rockFile) =>
                html`<li>${rockFile.name}</li>
                        <ul>
                            <li>${rockFile.contents}</li>
                            <li>score: ${rockFile.rockScore}</li>
                            <li><a href="${rockFile.restUrl}" target="_blank">call endpoint</a></li>
                        </ul>`
            )}</ul>`;
        } else {
            return html`No Rockstar endpoints found`;
        }
    }
}

customElements.define('qwc-bon-jova-rockstar-endpoints', QwcBonJovaRockstarEndpoints);
