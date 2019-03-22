# Susceptible-Infected-Recovered dynamic disease transmission model
# PyCSeS Implementation
# Author: Eric Lofgren (Eric.Lofgren@gmail.com)

Modelname: SIR
Description: PySCes Model Description Language Implementation of SIR model

# Set model to run with numbers of individuals
Species_In_Conc: False
Output_In_Conc: False

# Differential Equations as Reactions
R1:
	S > I
	beta*S*I/(S+I+R)
	
R2:
	I > R
	gamma*I

# Parameter Values
S = 999
I = 1
R = 0
beta = 0.50
gamma = 1/10
