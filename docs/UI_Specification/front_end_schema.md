# AMIDOL Front End
## Formal Definition and Terminology

## Example Front End Schema

```json
{ Noun: {
    id: "string",
    model: "filename",
    icon: "filename.svg"
    inputvariables: [{
      variable: "id"
      }]
    outputvariables: [{
      variable: "id"
      }]
  }
}

{ Verb: {
  id: "string",
  model: "filename",
  icon: "filename.svg"
  inputvariables: [{
    variable: "id"
    }]
  outputvariables: [{
    variable: "id"
    }]
  }
}
```

## Model composition schema

```json
{ ComposedModel: {
  id: "string",
  Model0: {
    id: "string"
    sharedVariables: [{
      id: "string"
      }]
    }
  Model1: {
    id: "string"
    sharedVariables: [{
      id: "string"
      }]
    }  
  }
}
```
