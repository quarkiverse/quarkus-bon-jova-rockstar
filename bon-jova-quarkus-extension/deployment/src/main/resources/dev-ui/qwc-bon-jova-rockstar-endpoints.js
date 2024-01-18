import {LitElement, html, css} from 'lit';
import {rockFiles} from 'build-time-data';
import '@vaadin/grid';
import {columnBodyRenderer} from '@vaadin/grid/lit.js'
import 'qui-ide-link';

/**
 * This component shows the Rockstar endpoints.
 */
export class QwcBonJovaRockstarEndpoints extends LitElement {
    static styles = css`
        a {
            color: var(--lumo-primary-text-color);
            text-decoration: none;
        }
    `;

    constructor() {
        super();
        this._rockFiles = rockFiles;
    }

    render() {
        if (this._rockFiles) {
            return html`
                <vaadin-grid .items="${this._rockFiles}">
                    <vaadin-grid-column header="Name" 
                                        ${columnBodyRenderer(this._nameRenderer, [])} 
                                        auto-width 
                                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column header="Endpoint"
                                        ${columnBodyRenderer(this._endpointRenderer, [])}
                                        auto-width 
                                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column header="Rock Score"
                                        ${columnBodyRenderer(this._rockScoreRenderer, [])}
                                        auto-width
                                        resizable>
                    </vaadin-grid-column>
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

    _endpointRenderer(rockFile) {
        return html`
            <a class="extensionLink" href="${rockFile.restUrl}" target="_blank">${rockFile.restUrl}</a>
        `;
    }

    _rockScoreRenderer(rockFile) {
        return html`
            ${rockFile.rockScore}/100
        `;
    }
}

customElements.define('qwc-bon-jova-rockstar-endpoints', QwcBonJovaRockstarEndpoints);
