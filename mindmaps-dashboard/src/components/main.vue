<!--
MindmapsDB - A Distributed Semantic Database
Copyright (C) 2016  Mindmaps Research Ltd

MindmapsDB is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MindmapsDB is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MindmapsDB. If not, see <http://www.gnu.org/licenses/gpl.txt>.
-->

<template>
<section class="wrapper">

    <!-- Header-->
    <nav class="navbar navbar-default navbar-fixed-top">
        <div class="container-fluid">
            <div class="navbar-header">
                <div id="mobile-menu">
                    <div class="left-nav-toggle">
                        <a href="#">
                            <i class="stroke-hamburgermenu"></i>
                        </a>
                    </div>
                </div>
                <a class="navbar-brand" href="/">
                    MindmapsDB
                    <span>{{version}}</span>
                </a>
            </div>
            <div id="navbar" class="navbar-collapse collapse">
                <div class="left-nav-toggle">
                    <a href="">
                        <i class="stroke-hamburgermenu"></i>
                    </a>
                </div>
            </div>
        </div>
    </nav>
    <!-- End header-->

    <!-- Navigation-->
    <aside class="navigation">
        <nav>
            <ul class="nav luna-nav">
                <li v-link-active>
                    <a v-link="{ path: '/status' }">Status</a>
                </li>

                <li v-link-active>
                    <a v-link="{ path: '/shell' }">Graql Visualiser</a>
                </li>

                <li v-link-active>
                    <a href="https://mindmaps.io/pages/index.html">Documentation</a>
                </li>

                <li class="nav-info">
                    <div class="m-t-xs">
                        <!-- <span><i class="pe-7s-share graph-choose-icon"></i>Current graph:</span> -->
                        <br/>


                        <button class="btn btn-default btn-block btn-squared" type="button" data-toggle="modal" data-target="#keyspace-modal">
                            <i class="pe-7s-share graph-choose-icon"></i>{{activeGraph}}
                        </button>

                        <div id="keyspace-modal" class="modal fade" tabindex="-1" role="dialog" aria-hidden="true" style="display: none;">
                            <div class="modal-dialog modal-sm">
                                <div class="modal-content">
                                    <div class="modal-header text-center">
                                        <h4 class="modal-title">Choose your graph</h4>
                                        <small>Some are big some are small</small>
                                    </div>
                                    <div class="modal-body">
                                        <ul class="dd-list">
                                            <li class="dd-item" v-for="g in graphNames" v-bind:class="{'li-active':graphActive(g)}" @click="engineClient.setActiveGraph(g)"><div class="dd-handle">{{g}}</div></li>
                                        </ul>
                                    </div>
                                    <div class="modal-footer">
                                        <button class="btn btn-default" type="button" data-dismiss="modal">Close</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </li>

            </ul>
        </nav>
    </aside>
    <!-- End navigation-->


    <!-- Main content-->
    <section class="content">
        <router-view></router-view>
    </section>

    <!-- End main content-->
</section>
</template>

<style>
.li-active {
    background-color: #337ab7;
}
.graph-choose-icon: {
    font-size: 14px;
}
</style>

<script>
import EngineClient from '../js/EngineClient.js';

export default {
    data() {
        return {
            version: undefined,
            graphNames: [],
            activeGraph: undefined,
            engineClient: {}
        }
    },

    created() {
        engineClient = new EngineClient();
    },

    attached() {
        engineClient.getStatus(this.statusResponse);
        engineClient.getGraphNames((r, e) => { this.graphNames=(r == null ? ['error'] : r) });
    },

    methods: {
        statusResponse(r, e) {
            if(r == null) {
                this.version = 'error';

            } else {
                this.version = r['project.version'];
                engineClient.setActiveGraph(r['graphdatabase.default-graph-name']);

                // Needed for dynamic updates in DOM
                this.activeGraph = engineClient.getActiveGraph();
            }
        },

        setActiveGraph(activeGraph) {
            engineClient.setActiveGraph(activeGraph);
            this.activeGraph = activeGraph;
        },

        graphActive(name) {
            return this.activeGraph === name;
        }
    }
}
</script>
