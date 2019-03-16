## TODO

This page helps Max keep track of UI development.

### Features:

Model variables:
- Use `[node-or-edge-id].[var-name]` dot notation with existing flat `Dict`
- Include `global.[var-name]` too?
- Swap them out in the editor sidebar
- Infer default set of vars per node 'type' (as indicated by `image`)

Switch between models:
- Bundle persistent parts of current `Model` into an `AmidolModel`
- The new Elm `Model` will be something like a `Dict ModelName AmidolModel`
- Model creation: name, select palette
- Model choice via dropdown
- Model deletion with confirm (postpone unless it's super easy)
- Replace visjs graph on model switch

Reward variables:
- What are they?
- How do they relate to node/edge/global vars?

Experiments + Solver:
- Parameters
- Send to backend
- Display results with [terezka/line-charts](https://package.elm-lang.org/packages/terezka/line-charts/latest/)



### Pending fixes and improvements:

On this branch, the Elm graph editor (Main.elm) is back-burnered
in favor of integrating visjs network.
Some things to do, if I return to it: 
- Node image code is commented out
- Nice affordances for node and link selection
- Key input for deletion (etc)
- Use Dict rather than List for both nodes and edges
- Factor out common label view code
- Layering: translucent labels on top
- Adjust arrow lengths and style
- Adjust label positions and style
- Style usage instructions and buttons
- More consistent naming and code organization
