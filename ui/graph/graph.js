
var node_data_set, edge_data_set, network;

var journal = {
    past : [],
    future : [],

    // `apply`, `undo`, and `redo` manage the journals _and_ trigger effects through applyEventEffects
    apply: function(event) {
        this.past.push(event)
        this.future = []
        this.applyEventEffects(event)
    },

    undo : function() {
        if (this.past.length > 0) {
            var journal_event = this.past.pop()
            this.future.unshift(journal_event)
            this.applyEventEffects(this.invertEvent(journal_event))
        }
    },

    redo : function() {
        if (this.future.length > 0) {
            var journal_event = this.future.shift()
            this.past.push(journal_event)
            this.applyEventEffects(journal_event)
        }
    },



    // Purpose: cause graphUI side effects... and nothing else. 
    // Always call last, after setting the relevant shared states.
    applyEventEffects : function(event) {
        // event = { type: <<"add", "subtract", ...>>, nodes: […], edges: […] }
        switch (event.type) {
            // TODO: allow user to rename nodes and edges
            case "add":
                node_data_set.add(event.nodes)
                edge_data_set.add(event.edges)
                break
            case "subtract":
                var node_ids_to_remove = _.pluck(event.nodes, "id")
                var edge_ids_to_remove = _.pluck(event.edges, "id")
                node_data_set.remove(node_ids_to_remove)
                edge_data_set.remove(edge_ids_to_remove)
                break
            case "move":
                _.mapObject(event.to, function(pos, nodeId){
                    network.moveNode(nodeId, pos.x, pos.y)
                })
                // TODO: change the x and y of the moved nodes in node_data_set
                break
            default:
                break
        }
        graphUI.updateButtonStates()
        var data = {
            "nodes" : node_data_set.getDataSet()._data,
            "edges" : edge_data_set.getDataSet()._data
        }
        // console.log(JSON.stringify(data, null, 2))
        elmUI.ports.graphData.send(data);
    },

    invertEvent : function(initial_event) {
        var event = _.clone(initial_event)  // make a copy, return a new object
        switch (event.type) {
            case "add":
                event.type = "subtract"
                return event
            case "subtract":
                event.type = "add"
                return event
            case "move":
                var from = event.from
                event.from = event.to
                event.to = from
                return event
            default:
                return event
        }
    },

    logHistory : function() {
        console.log(
            JSON.stringify(
                [ _.map(journal.past, function(x){return x.type + ":" + _.map(x.nodes, function(y){return y.id})}),
                  _.map(journal.future, function(x){return x.type + ":" + _.map(x.nodes, function(y){return y.id})}) ]
            )
        )
    }
}


var graphUI = {
    updateButtonStates : function() {
        if (_.isEmpty(journal.past)) {
            $("#undo_button").addClass("disabled")
        } else {
            $("#undo_button").removeClass("disabled")
        }
        if (_.isEmpty(journal.future)) {
            $("#redo_button").addClass("disabled")
        } else {
            $("#redo_button").removeClass("disabled")
        }
    },

    selectPaletteNode : function(elem) {
        $("#palette img").removeClass("selected")
        $(elem).addClass("selected")
    },

    drawNetwork : function() {
        var data = { "nodes" : [], "edges" : [] }
        var options = {
            physics: false,
            nodes: {
                shape: "image"
            },
            edges: {
                smooth: false,
                color: { color: "black" },
                arrows: "to"
            },
            interaction : {
                selectConnectedEdges: false
            },
            manipulation: {
                initiallyActive: true,
                addNode: function(node, callback){
                    node.label = prompt("Name your new node:")
                    if (node.label != null) {
                        node.image = $("#palette img.selected").attr("src")
                        journal.apply( { "type": "add", "nodes": [node], "edges": [] } )
                    }
                },
                addEdge: function(edge, callback){
                    edge.label = prompt("Edge label:")
                    if (edge.label != null) {
                        journal.apply( { "type": "add", "nodes": [], "edges": [edge] } )
                    }
                },
                // editNode: function(node, callback) {
                //     alert(JSON.stringify(node))
                // },
                deleteNode: function(dataIds, callback) {
                    journal.apply( { 
                        "type": "subtract", 
                        "nodes": node_data_set.get(dataIds.nodes), 
                        "edges": edge_data_set.get(dataIds.edges)
                    } )
                },
                deleteEdge: function(dataIds, callback) {
                    journal.apply( { 
                        "type": "subtract", 
                        "nodes": node_data_set.get(dataIds.nodes), 
                        "edges": edge_data_set.get(dataIds.edges) 
                    } )
                }
            }
        }

        node_data_set = new vis.DataSet(data.nodes)
        edge_data_set = new vis.DataSet(data.edges)

        var container = document.getElementById('graph')
        var network_data = { "nodes" : node_data_set, "edges" : edge_data_set}
        network = new vis.Network(container, network_data, options)

        network.on("oncontext", graphUI.networkRightClick)
        network.on("doubleClick", function(){})
        network.on("oncontext", function(){})
        network.on("selectNode", function(x){
            elmUI.ports.selectNode.send(x.nodes[0])
        })
        network.on("deselectNode", function(x){
            elmUI.ports.selectNone.send(null)
        })
        network.on("selectEdge", function(x){
            elmUI.ports.selectEdge.send(x.edges[0])
        })
        network.on("deselectEdge", function(x){
            elmUI.ports.selectNone.send(null)
        })
        network.on("hold", function(){})
        network.on("dragStart", function(e){
            var selectedNodes = network.getSelectedNodes()
            if ( ! _.isEmpty(selectedNodes)) {
                graphUI.movingNodes = network.getPositions(selectedNodes)
            }
        })
        network.on("dragEnd", function(e){
            if (graphUI.movingNodes != null) {
                journal.apply( {
                    "type" : "move",
                    "from": graphUI.movingNodes,
                    "to": network.getPositions(_.keys(graphUI.movingNodes))
                })
                graphUI.movingNodes = null
            }
        })
        // $("#graph").on("mousemove", null)  // Cannot use network 'dragging' event?
    },

    networkRightClick : function(context) {
        console.log(context.event)
        context.event.preventDefault()
    },

    movingNodes : null // or:  { nodeId1: {x: xValue, y: yValue}, … }
}


$(function(){
    // can't set onclick attrs from within Elm!
    $(".palette-img").attr("onclick", "graphUI.selectPaletteNode(this)")
    $("#undo_button").attr("onclick", "journal.undo()")
    $("#redo_button").attr("onclick", "journal.redo()")

    graphUI.drawNetwork()
    graphUI.updateButtonStates()

    var sample_nodes = [
        {id: "1", label: "Susceptible", image: "images/person.png", x: -250, y: 0}, 
        {id: "2", label: "Infected", image: "images/sick.jpg", x: 0, y: 150}, 
        {id: "3", label: "Recovered", image: "images/happy.png", x: 250, y: 0}
    ]
    var sample_edges = [
        {id: "a", from: "1", to: "2", label: "sickens"},
        {id: "b", from: "2", to: "3", label: "recovers"}
    ]

    journal.apply( { "type": "add", "nodes": sample_nodes, "edges": sample_edges } )
})
