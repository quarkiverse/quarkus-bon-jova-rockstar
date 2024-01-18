import {html, LitElement} from 'lit';
import {rockFiles} from 'build-time-data';
import '@vaadin/grid';
import {columnBodyRenderer} from '@vaadin/grid/lit.js'
import 'qui-ide-link';

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
            return html`
                <vaadin-grid .items="${this._rockFiles}">
                    <vaadin-grid-column 
                            header="Name" 
                            ${columnBodyRenderer(this._nameRenderer, [])} 
                            auto-width 
                            resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column path="contents" header="Contents" auto-width></vaadin-grid-column>
                    <vaadin-grid-column path="rockScore" header="Score" auto-width></vaadin-grid-column>
                    <vaadin-grid-column path="restUrl" header="REST URL" auto-width></vaadin-grid-column>
                </vaadin-grid>
            `;
        } else {
            return html`No Rockstar endpoints found`;
        }
    }

    _nameRenderer(rockFile) {
        return html`
            <qui-ide-link fileName='${rockFile.name}' lineNumber=0><code>${rockFile.name}</code></qui-ide-link>
        `;
    }
}

customElements.define('qwc-bon-jova-rockstar-endpoints', QwcBonJovaRockstarEndpoints);
