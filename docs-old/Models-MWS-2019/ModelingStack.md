* Science as nested optimization
  * Fitting the data as regression
  * Institutional process of discovery explores the power of a class of models.
  * Packages - Stan, Tensor Flow, Jump, IBM CPLEX
  * SIR modeling of recent ebola outbreaks
    * Sierra Leone, etc
* Agent Based Simulation
* Statistical Models
  * Regress the formula and fit to data
* Using Category Theory
  * Sets, groups, fields, rings, graphs, databases
  * Models as categories
    * Relations between models come from CT.
  * Categories: Have objects and morphisms
    * Objects: Ob(C)
    * morphisms are relations between objects Hom_C(x, y)
    * There is always an identity morphism
    * Morphisms compose.  f \in Hom_C(x, y), g \in Hom_C(y, z) we can get fog(x, z)
  * Models are categories
    * Has: initial state, time domain, parameterization, a solution
    * Each of these has types.
  * Categories of models, relationships between models also objects
* "Semantic Program Analysis for Scientific Model Augmentation"

* AutoMATES - University of Arizona
  * Clayton Morrison/Adarsh Pyarelal
  * Sources of Models
    * User defined
    * Text/figure extracted
  * AutoMATES IR
    * Grounded Function Networks (GrFN "Griffin")
  * Evapotransporation codebase - ASCE and Priestley-Taylor
  * Model analysis, structural analysis
    * Find shared nodes, novel nodes, create model diff
  * Sobol Sensitivity Analysis
    * Sensitivity indices allows us to see which variables and which pairs of variables account for the most variance in overall model output
