
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



    // Purpose: cause UI side effects... and nothing else. 
    // Always call last, after setting the relevant shared states.
    applyEventEffects : function(event) {
        // event = { type: <<"add", "subtract">>, nodes: […], edges: […] }
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
                    network.moveNode(nodeId, pos.x, pos.y)
                })
                break
            default:
                break
        }
        UI.updateButtonStates()
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


var UI = {
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
                editNode: function(node, callback) {
                    alert(JSON.stringify(node))
                },
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

        network.on("oncontext", UI.networkRightClick)
        network.on("doubleClick", function(){})
        network.on("oncontext", function(){})
        network.on("deselectNode", function(){})
        network.on("hold", function(){})
        network.on("dragStart", function(e){
            var selectedNodes = network.getSelectedNodes()
            if ( ! _.isEmpty(selectedNodes)) {
                UI.movingNodes = network.getPositions(selectedNodes)
            }
        })
        network.on("dragEnd", function(e){
            if (UI.movingNodes != null) {
                var from = UI.movingNodes
                var to = network.getPositions(_.keys(UI.movingNodes))
                UI.movingNodes = null
                journal.past.push({ "type" : "move", "from": from, "to": to })
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


var ELM = {
    getData : function(){
        return {
            "nodes" : node_data_set.getDataSet()._data,
            "edges" : edge_data_set.getDataSet()._data
        }
    }
}



$(function(){
    // can't set onclick attrs from within Elm!
    $(".palette-img").attr("onclick", "UI.selectPaletteNode(this)")
    $("#undo_button").attr("onclick", "journal.undo()")
    $("#redo_button").attr("onclick", "journal.redo()")

    UI.drawNetwork()
    UI.updateButtonStates()

    var sample_nodes = [
        {id: 1, label: "Susceptible", image: "images/person.png", x: -250, y: 0}, 
        {id: 2, label: "Infected", title: "*cough*  *cough*", image: "images/sick.jpg", x: -0, y: 0}, 
        {id: 3, label: "Recovered", title: "I'm better!", image: "images/happy.png", x: 250, y: 0}
    ]
    var sample_edges = [
        {id: "a", from: 1, to: 2, label: "gets sick"},
        {id: "b", from: 2, to: 3, label: "gets better"}
    ]

    journal.apply( { "type": "add", "nodes": sample_nodes, "edges": sample_edges } )
})
