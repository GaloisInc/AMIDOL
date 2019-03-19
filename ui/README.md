# AMIDOL front-end architecture notes

This file is intended to document design decisions and related considerations.
It will evolve as implementation proceeds.
Throughout, first-person language ("I", "me") means Max unless otherwise noted.
But other team members should **feel free to commit changes** here;
over the course of the project, the Git log will be easier to keep track of than a mess of email threads.

## JSON data 

The Elm UI will consist of pure functions that operate over a single data structure.
This is a sketch of the JSON representation of that structure.
Note that Elm's functions are stateless,
so there should be no hidden additional properties beyond what's represented in this structure.
For example, if we want some bit of style (maybe a highlight or border or text color) to be dynamic,
we need some representation of it in here, although not necessarily as CSS.
To keep things simple I propose that rather than implementing an additional update schema,
this entire structure will be sent between front-end and back-end in a single request.
The back-end should just change the properties of interest and return the entire modified structure.
It may be helpful to always normalize the JSON,
sorting objects by string key and arrays by some property of their contained elements.
I intend to avoid heterogeneous arrays.
At present, I haven't bothered to sort object keys here.

The top-level JSON object is a `Model`.

```
Model ::= {
    "title": String
    "nodes": { String: Node, ... } ,  # keyed by Node.id
    "edges": { String: Edge, ... } ,  # keyed by Edge.id
    "vars": { Var: String, ... }      # see Var definition below
}
```

`Node` objects come from visjs.
Their `id` properties, when created through the UI, are UUIDs like "35347575-492b-4d7e-af90-3aa58aae91c9".

```
Node ::= {
    "id": String,      # unique primary key
    "label": String,   # shown in the UI
    "image": String,   # file reference, e.g. "images/person.jpg"
    "x": Float,
    "y": Float
}
```

An `Edge` object comes from visjs, and represents a directed link between two `Node` objects.
When created through the UI, their `id` properties are also UUIDs.

```
link ::= {
    "id": String,     # unique primary key
    "label": String,  # shown in the UI
    "from": Integer,  # some Node.id
    "to": Integer,    # some Node.id
}
```

A `Var` is a `String` containing at least one dot (`.`),
such that the part before the first dot is either
- a key in the `nodes` object of the enclosing `Model`,
- a key in the `edges` object of the enclosing `Model`,
- or the literal string "Model"

The part of a `Var` following the first dot can be any string,
and is shown in the UI as the (implicitly scoped) variable name.

### Example JSON `Model`

{
  "title": "SIR",
  "nodes": {
    "4236e9e8-2879-4dfb-aa97-158b6cda5466": {
      "id": "4236e9e8-2879-4dfb-aa97-158b6cda5466",
      "label": "Dead",
      "image": "images/sick.jpg",
      "x": 5,
      "y": 349
    },
    "I": {
      "id": "I",
      "label": "Infected",
      "image": "images/sick.jpg",
      "x": 0,
      "y": 150
    },
    "R": {
      "id": "R",
      "label": "Recovered",
      "image": "images/happy.png",
      "x": 250,
      "y": 0
    },
    "S": {
      "id": "S",
      "label": "Susceptible",
      "image": "images/person.png",
      "x": -250,
      "y": 0
    }
  },
  "edges": {
    "6eda69cb-2ae9-470c-9022-7c356e9ccbc3": {
      "id": "6eda69cb-2ae9-470c-9022-7c356e9ccbc3",
      "label": "lose immunity",
      "from": "R",
      "to": "S"
    },
    "7bb2fc00-ff29-4e35-b8dd-abfa02b4e4fb": {
      "id": "7bb2fc00-ff29-4e35-b8dd-abfa02b4e4fb",
      "label": "worse",
      "from": "I",
      "to": "4236e9e8-2879-4dfb-aa97-158b6cda5466"
    },
    "cure": {
      "id": "cure",
      "label": "better",
      "from": "I",
      "to": "R"
    },
    "infect": {
      "id": "infect",
      "label": "infect",
      "from": "S",
      "to": "I"
    }
  },
  "vars": {
    "4236e9e8-2879-4dfb-aa97-158b6cda5466.Pop.": "0",
    "I.Pop.": "0",
    "Model.Total pop.": "0",
    "R.Pop.": "0",
    "S.Pop.": "0"
    "cure.beta": "",
    "infect.gamma": ""
  }
}
