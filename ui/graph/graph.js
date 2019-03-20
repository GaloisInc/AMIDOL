
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
                    node_data_set.update({id: nodeId, x: pos.x, y: pos.y})
                })
                break
            case "renameNode":
                node_data_set.update({id: event.id, label: event.to})
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
            case "renameNode":
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
                // dragView: false,
                // zoomView: false,
                selectConnectedEdges: false
            },
            manipulation: {
                initiallyActive: true,
                addNode: function(node, callback){
                    node.label = prompt("Label:")
                    if (node.label != null) {
                        node.image = $("#palette img.selected").attr("src")
                        journal.apply( { "type": "add", "nodes": [node], "edges": [] } )
                    }
                },
                addEdge: function(edge, callback){
                    callback(null)
                    journal.apply( { "type": "add", "nodes": [], "edges": [edge] } )
                },
                editNode: function(node, callback) {
                    newName = prompt("New noun label:")
                    callback(null)
                    journal.apply({
                        type: "renameNode",
                        id: node.id,
                        from: node.label,
                        to: newName
                    })
                },
                editEdge: false,
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
            elmUI.ports.selectModel.send(null)
        })
        network.on("deselectEdge", function(x){
            elmUI.ports.selectModel.send(null)
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
        {id: "S", label: "Susceptible", image: "images/person.png", x: -250, y: 0},
        {id: "i", label: "infect", image: "images/virus.png", x: -125, y: 75},
        {id: "I", label: "Infected", image: "images/patient.png", x: 0, y: 150},
        {id: "c", label: "cure", image: "images/pill.png", x: 125, y: 75},
        {id: "R", label: "Recovered", image: "images/person.png", x: 250, y: 0}
    ]
    var sample_edges = [
        {id: "Si", from: "S", to: "i"},
        {id: "iI", from: "i", to: "I"},
        {id: "Ic", from: "I", to: "c"},
        {id: "cR", from: "c", to: "R"}
    ]

    journal.apply( { "type": "add", "nodes": sample_nodes, "edges": sample_edges } )
})
