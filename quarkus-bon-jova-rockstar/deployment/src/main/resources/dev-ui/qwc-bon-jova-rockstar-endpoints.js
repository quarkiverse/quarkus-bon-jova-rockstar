import {css, html, LitElement} from 'lit';
import {rockFiles} from 'build-time-data';
import {columnBodyRenderer, gridRowDetailsRenderer} from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/details';
import 'echarts-gauge-grade';
import '@quarkus-webcomponents/codeblock';
import 'qui-ide-link';
import 'qui-badge';
import {observeState} from "./lit-state";
import {themeState} from "./theme-state";

/**
 * This component shows the Rockstar endpoints.
 */
export class QwcBonJovaRockstarEndpoints extends observeState(LitElement) {
    static styles = css`
        .bonjova {
            height: 100%;
            padding-bottom: 10px;
        }

        a {
            color: var(--lumo-primary-text-color);
            text-decoration: none;
        }

        .cardGrid {
            display: flex;
            flex-wrap: wrap;
            align-items: stretch;
            gap: 20px;
            padding-left: 5px;
            padding-right: 10px;
        }

        .detailCard {
            min-width: 400px;
        }

        .codeBlock {
            gap: 10px;
            flex-direction: column;
            padding-left: 10px;
            padding-right: 10px;
            min-height: 300px;
        }

        .rockScoreChart {
            height: 300px;
        }

        echarts-gauge-grade {
            display: flex;
            justify-content: center;
            align-items: center;
        }
    `;

    static properties = {
        _detailsOpenedItem: {state: true, type: Array}
    }

    constructor() {
        super();
        this._rockFiles = rockFiles;
        this._detailsOpenedItem = [];
    }

    render() {
        if (this._rockFiles) {
            return html`
                <vaadin-grid .items="${this._rockFiles}"
                             class="bonjova"
                             theme="row-stripes"
                             .detailsOpenedItems="${this._detailsOpenedItem}"
                             @active-item-changed="${(event) => {
                                 const prop = event.detail.value;
                                 this._detailsOpenedItem = prop ? [prop] : [];
                             }}"
                             ${gridRowDetailsRenderer(this._expandedRowRenderer, [])}
                >

                    <vaadin-grid-sort-column header="Name"
                                             path="name"
                                             ${columnBodyRenderer(this._nameRenderer, [])}
                                             auto-width
                                             resizable>
                    </vaadin-grid-sort-column>
                    <vaadin-grid-column header="Endpoint"
                                        path="restUrl"
                                        ${columnBodyRenderer(this._endpointRenderer, [])}
                                        auto-width
                                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column header="Rock Score"
                                        path="rockScore"
                                        ${columnBodyRenderer(this._rockScoreRenderer, [])}
                                        frozen-to-end>
                    </vaadin-grid-column>
                </vaadin-grid>`;
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
        let level = this._getLevel(rockFile.rockScore);
        return html`
            <qui-badge level="${level}">${rockFile.rockScore}/100</qui-badge>
        `;
    }

    _expandedRowRenderer(rockFile) {
        let level = this._getLevel(rockFile.rockScore);
        return html`
            <div class="cardGrid">
                <qui-card class="detailCard" title="Program Contents">
                    <div slot="content">
                        <div class="codeBlock">
                            <qui-code-block mode="java"
                                            content="${rockFile.contents}" theme="${themeState.theme.name}">
                            </qui-code-block>
                        </div>
                    </div>
                </qui-card>
                <qui-card class="detailCard" title="Rock Score">
                    <div slot="content">
                        <div class="rockScoreChart">
                            <echarts-gauge-grade
                                    title="Rock Score 🎸"
                                    percentage="${rockFile.rockScore}"
                                    sectionColors="--lumo-${level}-color">
                            </echarts-gauge-grade>
                        </div>
                    </div>
                </qui-card>
            </div>
        `;
    }

    _getLevel(score) {
        let level = "error";
        if (score >= 50 && score < 100) {
            level = "warning";
        } else if (score === 100) {
            level = "success";
        }
        return level;
    }
}

customElements.define('qwc-bon-jova-rockstar-endpoints', QwcBonJovaRockstarEndpoints);
