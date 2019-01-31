Introduction
============

Complex system analysis currently requires teams of domain experts, data
scientists, mathematicians, and software engineers to support the entire
life cycle of model-based inference. The models that result are often
bespoke, lack generalizability, are not performable, and make it
difficult to synthesize actionable knowledge and policies from their raw
outputs. In this report we describe the current prototype system for
[<span style="font-variant:small-caps;">AMIDOL</span>]{}: the Agile
Metamodel Interface using Domain-specific Ontological Languages, a
project that aims to reduce the overhead associated with the model life
cycle and enables domain experts and scientists to more easily build,
maintain, and reason over models in robust and highly performable ways,
and to respond rapidly to emerging crises in an agile and impactful way.
We discuss the current design principles of the [<span
style="font-variant:small-caps;">AMIDOL</span>]{} prototype, its
capabilities, plans for development, and formal aspects of the system.

[<span style="font-variant:small-caps;">AMIDOL</span>]{} is designed to
support models in a number of scientific, physical, social, and hybrid
domains by allowing domain experts to construct meta-models in a novel
way, using visual domain specific ontological languages (VDSOLs). These
VDSOLs utilize an underlying intermediate abstract representation to
give formal meaning to the intuitive process diagrams scientists and
domain experts normally create. [<span
style="font-variant:small-caps;">AMIDOL</span>]{}’s abstract
representations are executable, allowing [<span
style="font-variant:small-caps;">AMIDOL</span>]{}’s inference engine to
execute prognostic queries on reward models and communicate results to
domain experts. [<span style="font-variant:small-caps;">AMIDOL</span>]{}
binds results to the original ontologies providing more explainability
when compared to conventional methods.

[<span style="font-variant:small-caps;">AMIDOL</span>]{} addresses the
problem of machine-assisted inference with two high-level goals:

1.  improving the ability of domain experts to build and maintain models
    and

2.  improving the explainability and agility of the results of
    machine-inference.

Our techniques for achieving these goals incorporate abstract functional
representations, intermediate languages, and semantic knowledge
representation and binding in graph structures into traditional machine
learning and model solution techniques.

VDSOL Definition
================

[<span style="font-variant:small-caps;">AMIDOL</span>]{} is designed to
support the definition of ontological languages which describe systems
as formal objects. Objects for a given domain are organized into
*toolkits* consisting of **nouns** and **verbs**. Nouns define elements
which make up the state space of a system, and verbs define transitions
in the state space. VDSOLs enable domain experts to build models of
complex systems which are easier to maintain, validate, and verify, and
avoid common pitfalls of monolithic and hand-coded implementations. To
provide visual context for modelers, [<span
style="font-variant:small-caps;">AMIDOL</span>]{} supports the use of
arbitrary scalable vector graphics (SVGs) to represent nouns and verbs,
and features a canvas to draw nouns and verbs with labeled arcs
connecting them to provide context.

The goal of [<span style="font-variant:small-caps;">AMIDOL</span>]{}’s
VDSOLs is to enable domain experts to define their models using an
interface and visual language similar to the semi-formal diagrams they
use today, but with the advantage that [<span
style="font-variant:small-caps;">AMIDOL</span>]{}s VDSOLs have formal,
executable, meaning. VDSOLs provide a performable, reusable, system for
scientists to use when attempting to derive insights relating to the
complex systems they represent.

VDSOLs in [<span style="font-variant:small-caps;">AMIDOL</span>]{} are
constructed using a graphical user interface implemented using
asynchronous javascript and XML to build a responsive interface to
define models of complex systems, reward models used to explore and
understand complex system behavior, and to interact with the results of
solvers implemented in the Machine-Assisted Inference Engine.

Basic Language Properties
-------------------------

#### Nouns

: Nouns in [<span style="font-variant:small-caps;">AMIDOL</span>]{}
represent portions of the model associated with its state space. The
[<span style="font-variant:small-caps;">AMIDOL</span>]{} IR translates
noun elements into state variables and constants. Nouns are represented
by custom SVGs, and can be connected to verbs which act upon them. Users
can set the label associated with a noun, which impacts the naming of
state variables associated with the noun.

#### Verbs

: Verbs in [<span style="font-variant:small-caps;">AMIDOL</span>]{}
represent activities or events which can occur in a model, and which act
upon nouns changing the state of the system. Verbs are associated with a
few mandatory and optional properties which impact their translation
into the intermediate representation. All verbs have a mandatory rate
which defines the rate at which the associated event occurs. This rate
can be dependent on nouns in the model. Verbs have an optional enabling
condition which can be dependent on nouns in the model. The enabling
condition defines that state variable bounds during which the associated
event is enabled, and can be specified as an algebraic expression over
state variables associated with nouns in a model. Verbs also have an
optional output function which defines which nouns are impacted when the
associated event fires.

Composability of Atomic Models
------------------------------

[@sanders1992dependability; @sanders1988construction]

UI/UX Design
------------

JSON Export Language
--------------------

Abstract Intermediate Representation
====================================

The Abstract Intermediate Representation (IR) for [<span
style="font-variant:small-caps;">AMIDOL</span>]{} is meant to be a
universal way to specify models, regardless of their domain, and
provides a Turing-complete way to specify models performably, while
avoiding domain specific considerations.

Markov models [@howard2012dynamic]

Petri-nets with inhibitor arcs [@chiola1993generalized]

Stochastic activity networks
[@movaghar1985performability; @sanders2000stochastic]

Language Properties
-------------------

Formally, the IR is a 5-tuple, $(S, E, L, \Phi, \Lambda, \Delta)$ where:

-   $S$ is a finite set of state variables
    $\{s_0, s_1, \ldots, s_{n-1}\}$ that take on values in $\mathbb{N}$.

-   $E$ is a finite set of events $\{e_0, e_1, \ldots, e_{m-1}\}$ that
    may occur in the model.

-   $L: S|E \rightarrow \mathbb{N}$ is the event and state variable
    labeling function that maps elements of $S and E$ into the original
    ontology.

-   $\Phi: E \times N_0 \times N_1 \times \ldots \times N_{n-1} \rightarrow \{0, 1\}$
    is the event enabling predicate.

-   $\Lambda: E \times N_0 \times N_1 \times \ldots \times N_{n-1} \rightarrow (0, \infty)$
    is the transition rate specification.

-   $\Delta: E \times N_0 \times N_1 \times \ldots \times N_{n-1} \rightarrow N_0 \times N_1 \times \ldots \times N_{n-1}$
    is the state variable transition function specification.

Informally the IR represents models defined in a given VDSOL using an
formalism based on Generalized Stochastic Petri-nets with inhibitor arcs
(which have the result of making Petri-nets Turing complete). Instead of
inhibtor arcs, we utilize the more intuitive and performable method of
allowing events to have input predicates ($Phi$) which can be evaluated
to determine if an event is enabled, and output predicates which define
the side effects of event firing.

#### State variables

:

#### Events

:

#### Input predicates

:

#### Output predicates

:

#### Representation

:

Inference Engine
================

#### ODE Solver

:

#### Numerical Solution

:

#### Discrete Event Simulation

:

Reward Variables and Reward Models
==================================

The [<span style="font-variant:small-caps;">AMIDOL</span>]{}
intermediate representation allows for the specification of reward
variables or structures over a given model, and the composition of these
structures with a model to produce composed models which can then be
solved by the inference engine. Given a model
$M = (S, E, L, \Phi, \Lambda, \Delta)$ we define two basic types of
rewards structures, rewards over state variable values (rate rewards),
and rewards over events (impulse rewards).

[@qureshi1996algorithms; @deavours1999efficient; @ciardo1996well; @sanders1991reduced]

Rate Reward Variables
---------------------

A rate reward is formally defined as a function
$\mathcal{R}: P(S, \mathbb{N}) \rightarrow \mathbb{R}$ where
$q \in P(S, \mathbb{N})$ is the reward accumulated when for each
$(s,n) \in q$ the marking of the state variable $s$ is $n$. Informally a
rate reward variable $x$ accumulates a defined reward whenever a subset
of the state variables take on prescribed values.

Impulse Reward Variables
------------------------

An impulse reward is formally defined as a function
$\mathcal{I}: E \rightarrow \mathbb{R}$ where $e \in E, \mathcal(I)_e$
is the reward for the completion of $e$. Informally an impulse reward
variable $x$ accumulates a defined reward whenever the event $e$ fires.

Temporal Characteristics of Reward Variables
--------------------------------------------

Both rate and impulse reward variables measure the behavior of a model
$M$ with respect to time. As such, a reward variable $\theta$ is
declared as either an instant-of-time variable, an interval-of-time
variable, a time-averaged interval-of-time variable, or a steady state
variable. An instant of time variable $\Theta_t$ is defined as:

$$\theta_t = \sum_{\nu \in P(S, \mathbb{N})} \mathcal{R}(\nu) \cdot \mathcal{I}^{\nu}_t + \sum_{e \in E} \mathcal{I}(e) \cdot I_t^e$$

Intuitively a rate reward declared as an instant-of-time variable
[@freire1990technique] can be used to measure the value of a state
variable precisely at time $t$, and an impulse reward declared as an
instant-of-time variable can be used to measure whether a given event
fired at precisely time $t$. While the latter is not a particularly
useful measure (as the probability of an event with a firing time drawn
from a continuous distribution at time $t$ is $0$) it is defined for
closure reasons, and for cases with discrete distributions and discrete
time steps.

An interval-of-time variable intuitively accumulates reward over some
fixed interval of time $[t, t+1]$. Given such a variable
$\theta_{[t, t+1]}$ we formally define interval-of-time variables as:

$$\theta_{[t,t+1]} = \sum_{\nu \in P(S, \mathbb{N})} \mathcal{R}(\nu) \cdot \mathcal{J}^{\nu}_{[t, t+1]} + \sum_{e \in E} \mathcal{I}(e)N^e_{[t,t+1]}$$

where

-   $J^{\nu}_{[t,t+1]}$ is a random variable which represents the total
    time the model spent in a marking such that for each
    $(s, n) \in \nu$, the state variable $s$ has a value of $n$ during
    the period $[t, t+1]$.

-   $I^e_{t\rightarrow\infty}$ is a random variable which represents the
    number of times an event $e$ has fired during the period $[t, t+1]$.

Time-averaged interval of time variables quantify accumulated reward
over some interval of time. Such a variable $\theta'_{[t,t+1]}$ is
defined formally as:

$$\theta'_{[t,t+1]} = \frac{\theta_{[t,t+1]}}{l}$$

Translation of Reward Variables to IR
-------------------------------------

Expressions on Reward Variables
-------------------------------

Design of Experiments and Results Database
==========================================

Results Database
----------------

Prognostic Queries
------------------

Model Comparison
----------------

Design of Experiments
---------------------

Conterfactural Exploration, Planning, Crisis Response
-----------------------------------------------------

Correctness and Uncertainty
---------------------------

Communication of Results
------------------------

Domain Models
=============

We are currently testing [<span
style="font-variant:small-caps;">AMIDOL</span>]{} using several domain
models whose primary domain is epidemiology. We have selected a range of
models to test different scenarios, use cases, and assumptions to aid in
the prototype design of [<span
style="font-variant:small-caps;">AMIDOL</span>]{}.

SIS/SIRS
--------

H1N1 $R_0$ importance [@fraser2009pandemic].

Ebola $R_0$ importance [@fisman2014early]

CDC Data [@cdc2019fluview]

The SIS/SIRS model is one of the simplest models we have deployed for
testing with [<span style="font-variant:small-caps;">AMIDOL</span>]{},
with the advantage that the model itself is relatively simple, but
utlizes real data, and can be used to answer important epidemiological
questions. The primary objective of the SIS/SIRS model is to identify
the *basic reproduction number* associated with an infection, also known
as $R_0$, or *r nought*. $R_0$ was first used in 1952 when studying
malaria and is a measure of the potential for an infection to spread
through a population. If $R_0 < 1$, then the infection will die out in
the long run. If $R_0 > 1$, then the infection will spread. The higher
the value of $R_0$, the more difficult it is to control an epidemic.

Given a 100% effective vaccine, the proportion of the population that
needs to be vaccinated is $1 - 1/R_0$, meaning that $R_0$ can be used to
plan disease response. This assumes a homogenous population, and
contains many other simplifying assumptions and does not generalize to
more complex numbers. We have several main goals for SIS/SIRS models:

1.  Fitting the models for the data in hindsight to perform goodness of
    fit estimates.

2.  Finding the *retrospective* $R_0$ estimate over the entire epidemic
    curve.

3.  Finding the *real-time* $R_0$ estimate while the epidemic is
    ongoing.

#### Data

: For these models we will be working with the WHO/NREVSS (World Health
Organization/National Respiratory and Enteric Virus Surveillance System)
data sets at the resolution of Department of Human and Health Services
designated regions.

![Department of Human and Health Services designated
regions.[]{data-label="Fig:Regions"}](figs/regionsmap.pdf){width="\textwidth"}

Using data from a given region, and a given strain, we will estimate R0
for the epidemic curve as shown in Figure \[Fig:R0\]

![2007 - 2008 Flu
Season[]{data-label="Fig:R0"}](figs/2007-2008-SIRS.pdf){width="\textwidth"}

Artificial Chemistry
--------------------

Viral Infection Model
---------------------

![Simple noun (circle) and verb (square) representation of Tat model
without ambiguity and
aliasing.[]{data-label="Fig:HIV-Tat-VDSOL"}](figs/HIV-Tat-figure.pdf){width="\textwidth"}

![Simple noun (circle) and verb (square) representation of Tat model
without ambiguity and
aliasing.[]{data-label="Fig:HIV-Tat-VDSOL"}](figs/TatModel.pdf){width="\textwidth"}

Note the use of multiple “Tat” symbols in Figure \[Fig:HIV-Tat\].
Sometimes scientists draw the same symbol multiple places as an “alias”
for the same underlying state variable.

$$\begin{aligned}
LTR \overset{k_{basal}}{\rightarrow} LTR + nRNA\\
nRNA \overset{k_{export}}{\rightarrow} cRNA\\
  cRNA \overset{k1_{translate}}{\rightarrow} GFP + cRNA\\
  cRNA \overset{k2_{translate}}{\rightarrow} Tat + cRNA\\
  Tat \overset{k_{bind}/k_{unbind}}{\leftrightarrow} pTEFb_d\\
  LTR + pTEFb_d \overset{k_{acetyl}/k_{deacetly}}{\leftrightarrow} pTEFb_a\\
  pTEFb_a \overset{k_{transact}}{\leftrightarrow} LTR + nRNA + Tat\\
  GFP \overset{d_{GFP}}{\rightarrow} \emptyset\\
  Tat \overset{d_{Tat}}{\rightarrow} \emptyset\\
  cRNA \overset{d_{CYT}}{\rightarrow} \emptyset\\
  nRNA \overset{d_{NUC}}{\rightarrow} \emptyset\end{aligned}$$

H5N1 Model
----------

H3N2 Model
----------

User Stories
============

Code Repositories and Current Builds
====================================

The UI (an Elm project contained in the ‘ui‘ directory) is a series of
HTML pages through which users will interact with the system. That
includes drawing/manipulating semi-formal models, issuing queries,
loading data.

The main system (a Scala project contained in the ‘ir‘ directory)
compiles questions that users may have about models they’ve created into
code artifacts targeting different simulation/solver backends. Over
time, we expect this system to also manipulate and compose datasets
(from real-world observations as well as from the output of other
simulations).

Front-End Architecture for VDSOLs
---------------------------------

Amidol’s user interface is a small collection of web pages which
communicate with the Scala back-end using JSON over HTTP. This
client/server approach leverages standard browser technologies like
HTML5, CSS, and SVG graphics for rapid development of rich interactions
tailored for the specific needs of scientific modeling. It also
decouples the concerns of visual presentation and manipulation from the
underlying representations of model semantics.

The Javascript code which implements the user interface is compiled from
Elm, a strongly typed functional language designed specifically for
front-end web development. Elm’s runtime system is similar to that of
popular Javascript frameworks like React, but the language provides some
distinct advantages in terms of correctness, performance, and ease of
refactoring. In addition to the capabilities provided by the Elm package
ecosystem, Elm applications can interoperate directly with Javascript,
allowing Amidol to make use of the best available web-based data
visualization libraries.

Integration concepts: - API schema and versioning - supports
collaboration between multiple users - one server, potentially multiple
client implementations - persistent server, ephemeral clients -
resource-heavy servers (cluster?), lightweight clients

Backend Architecture for IR and Inference Engine
------------------------------------------------

The backend component interfaces with the UI via a local web server. The
idea here is that every time the user interacts with the UI either to
modify or to ask questions of the model, that information also gets
relayed to the backend via a set of web endpoints. When the backend
receives new information about the model from the UI, it parses this
into some internal representation. This includes such tasks as parsing
equations from user-inputted strings and checking that state variables
being referred to actually exist. Questions asked of the model follow a
slightly longer path: after being parsed out and validated, the backend
figures out how to transform the IR and the question into executable
code, executes this code, and returns the result back out to the user.

Within the backend, the IR is stored in a graph format resembling what
the user constructed in the UI. By maintaining some degree of
similarity, we hope to make it simpler to translate results obtained in
the backend back out into something end users can easily understand.
Questions asked about models are translated into code artifacts
targeting existing solver and simulation programs. We wish to avoid
doing actual simulations within the system, instead focusing on
intelligently compiling queries into programs that external solvers can
run. In order to do this sort of thing, we still do need to implement a
minimum amount of symbolic algebra (ex: detecting when a continuous rate
model is linear). For the time being, we’ve been targeting Python’s
SciPy module as a backend to answer basic simulation questions such as:
the the initial value problem for general systems as well as for
continuous-time Markov chains.

The backend is written in Scala, leveraging a set of libraries built on
top of the Akka actor system for the web server, JSON processing,
asynchronous computation, and eventually for the graph in which the IR
and data will be stored. The advantages to using Scala include:
deployable anywhere the JVM runs, large set of available libraries, and
a functional outlook which lends itself well to compiler problems.

Integrating AMIDOL
------------------

I think it’s worth saying somewhere that the JSON API which defines
client/server interaction is the only channel by which tangible VDSOL
models and computational AFI communicate. In other words, the UI doesn’t
hold any persistent model state which is not represented in the API; it
is precisely a view or translation of the AFI/IR. This property is what
enables the list of integration concepts. In a word, it’s a RESTful
architecture.

![[]{data-label="Fig:Editor"}](figs/Editor.png){width="\textwidth"}

![[]{data-label="Fig:LoadModel"}](figs/LoadModel-crop.pdf){width="\textwidth"}

![[]{data-label="Fig:Query"}](figs/QueryIntegrateBackend-crop.pdf){width="\textwidth"}

![[]{data-label="Fig:QueryResult"}](figs/QueryResult.png){width="\textwidth"}

Roadmap for Future Development
==============================
