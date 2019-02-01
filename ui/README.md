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

The top-level JSON object is (at least) a graph.

```
top ::= {
    "nodes": [ node ],  # sorted by id
    "links": [ link ],  # sorted by id
    ...  # additional global properties as needed
}
```

As other UI elements come into play, they will be represented here in `top`.
My preference is to avoid excessive nesting,
but as we add major components (such as charts or menus),
they will probably deserve their own JSON object definitions.


A `node` object will have (at least) some basic visual properties.
*(Note: JSON doesn't have an Integer type, but Elm does! We can enforce that the JSON numbers are indeed integers.)*

```
node ::= {
    "id": Integer,     # unique primary key
    "view": String,    # file reference, e.g. "picture.svg"
    "label": String,   # shown in the UI
    "location": {
        "x": Float,
        "y": Float
    },
    ...  # additional style or semantic properties
}
```

A directed `link` object represents a connection between two nodes.

```
link ::= {
    "id": Integer,      # unique primary key
    "source": Integer,  # some node.id
    "target": Integer,  # some node.id
    "label": String,
    ...  # additional style or semantic properties
}
```

