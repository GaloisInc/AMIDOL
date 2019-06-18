---
layout: post
title: W3 Collaboration - Summer 2019 Meeting
toc: true
---

## Meeting Notes

* [Day 1 W3 Meeting Notes]({% link /W3Collaboration/2019-06-17-W3-Day1-Notes.md %})
* [Day 2 W3 Meeting Notes]({% link /W3Collaboration/2019-06-18-W3-Day2-Notes.md %})

## Questions from GTRI

1. Does the user have parameters, functions, and variables to choose from or is it completely arbitrary? If arbitrary, how do you compare two models using your IR?
  * It is "arbitrary".  A given VDSOL will have a predefined palette, but the palettes are designed to be flexible and extensible.  Model comparison is envisioned to happen in two major ways.
    * **Structural similarity** - Are the models structurally equivalent?  This can be found by using a normal form of the underlying mathematical model, or by generating the state-space for finite state models, or a bounded approximation of the state-space for models with infinite space.
    * **Similarity and bisimilarity with respect to reward variables** - This is probably the "right" way to identify similarity or compare models in most cases.  Domain knowledge, coupled with reward variables defined over the model leads to comparable outcomes for reward variables.  Model comparison is done not on the models themselves, but measures computed on their reward variables.
2. AMDIOL backend can perform transformations on a model resulting in more performable versions of a model for "efficient" solution and takes into account "structural optimizations" to solve a set of reward variables to determine model equivalence. Can you explain how to determine structural optimization and what determines an efficient solution?
  * This is meant as a general statement.  We view the AMIDOL IR like LLVM's bytecode.  It's a universal mathematical language on which transformations can be performed in a domain-agnostic fashion.  For example - state space lumping techniques.  Say we have the following set of chemical equations: $$A + B \overset{\alpha}{\leftrightarrow} C$$ and $$D + E \overset{\alpha}{\leftrightarrow} F$$.  Any estimate for a reward variable on the population of $$C$$ is also an estimate for the equivalent reward variable on the population of $$F$$. Symmetry detection in Petri-nets reduces to a previously solved problem, which means there is a more efficient solution for this model than the naive solution of the entire system.
  * Efficient solution in this case refers to the speed up gained through the application of transforms of this sort or other similar ones.  E.g. importance sampling, importance splitting for stiff system solution; antithetic variance methods; batch-means simulation; etc.
3. If you have two instances of a single model, both with different reward variables, does this result in two entries in the database?
  * We haven't implemented the database yet, so this isn't solved as of yet.  It seems to relate to model similarity.  Do we want to fuse multiple models if they are equivalent?  My initial thought is that we do not.  We may want to connect them, but exact symmetry is hard to guarantee without explicitly generating the state-space.
4. Does the database store all the results from all the past models that it has seen before?
  * We'd like it to be as comprehensive as possible.  The database is a Phase II goal, and not yet complete.
5. Is the noun/verb labeling automated or manually provided?
  * Manually.  We envision this also being something that could be a by product of TA1 performers.  We'd like to explore automatic labeling.
6. “Model composition in AMIDOL is being designed to support state sharing and event synchronization” -- In terms of the state sharing , how do you tell two states are equivalent and can be shared between models?
  * State sharing is semantic knowledge from the modeler.  It has to be specified.  The best results in the previously studied literature advises human-machine teaming solutions which range from simple "share all similarly named state variables" to interactive processes.
  * Labels from the source material from TA1 performers might help?
7. How does the union of predicates of an event contribute to the model?
  * The union of input predicates creates a composed event which is enabled only when both of it's shared event "parents"' predicates evaluate to true.
  * The union of output predicates creates a composed event whose state transition function is the same as the union of the it's shared "parents".
  * In essence - it is only enabled if both of its "parents" would be enabled, and when fired has the same effect as if both of its "parents" fired.
7. How is selection handled when cardinality of either set of predicates is > 1?
  * *Discussion point:* I'm not sure what selection refers to here.
8. How do you know that the noun specified by a user can be tied to an event? Is this determined by the predicates?
  * Nouns are connected to **verbs** via arcs.  Right now nouns and verbs have input and output sets.  Those sets are shared on connection.  The input and output sets are sets of state variables.  The IR defines a noun or verb explicitly with those state variables.  Any event in a noun or a verb has its input predicate specified in terms of state variables local to the noun or verb's IR model.
9. How to identify input predicates that trigger events?
  * *Discussion point:* I'm not sure what is meant to here.  Input predicates do not trigger events.  Input predicates determine if an event is enabled and can fire.  In the discrete interpretation (for ABM, or discrete event simulation) the event list is scanned for enabled events whenever state variables change.  In the continuous interpretation, for event $$e$$ with  rate $$\Lambda(e)$$, and output predicate $$\Delta(e)$$ there is an equivalent set of flow locations for each set defining a state variable translations $$(u,v) \in \Delta(e)  u != v$$ equal to $$(v-u)\Lambda(e)$$.
10. “In practice, they are implemented as integers, and floating point numbers by the AMIDOL source code.” How is this done? Again how are States determined?
  * State variables are just variables in the executable code, either integers or floating point numbers.  **State variables** are determined from the VDSOL.  **States** can be generated through the application of a state space generation algorithm to the AMIDOL IR.
11. How is the rate function distribution determined, is it user-defined or empirically derived?
  * User defined from the TA2 perspective.  In theory we could extract them with the help of a TA1 performer.  They're quite easy to determine from most formal model specifications.  *Let's work an example using SIR, and the crystalization model.*
12. Impulse Reward Variables where is the reward for the completion of events, scope seems to be large, how is this determined?
  * Impulse and rate rewards are tools to define a measure.  For an impulse reward variable with reward $$r$$ and event $$e$$, we create an acccumulator variable $$\rho_{r,e}$$.  If $$\rho_{r,e}$$ is defined as an instant of time reward, it is trivially 0.  If $$\rho_{r,e}$$ is defined as an interval of time reward from $$[t_0, t_n]$$, then the continuous interpretation is simply $$\Lambda(e) \cdot r \cdot(t_n - t_0)$$.  The discrete interpretation is determined by creating an accumulator that is incremented whenever $e$ is fired and the simulation time $$t_0 \leq t_{now} \leq t_n$$.
13. How to determine the rewards that come out of events?
  * Rewards do not come out of events.  Reward variables are computed over a model.  They are formal measures defined on a model.
