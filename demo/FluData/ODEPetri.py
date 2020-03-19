from scipy.integrate import odeint
import operator
import inspect
import numpy as np

class ODEPetri:
    def __init__(self):
        self.stateVariables = []
        self.stateVariableNames = {}
        self.dSdT = []

    def add_StateVariable(self, theName, theInitial):
        if (theName in self.stateVariableNames):
            raise NameError(theName + " already exists.")
        nextId = len(self.stateVariables)
        self.stateVariableNames[theName] = nextId
        self.stateVariables.append(theInitial)

    def add_dSdT(self, theName, theDelta):
        if not (theName in self.stateVariableNames):
            raise NameError(theName + " is not a valid state variable.")
        svIdx = self.stateVariableNames[theName]

        while (len(self.dSdT) <= svIdx):
            self.dSdT.append((lambda y, t, C: 0.0))

        self.dSdT[svIdx] = theDelta

    def __str__(self):
        funcStr = ""
        for func in self.dSdT:
            funcStr += str(inspect.getsourcelines(func)[0]) + "\n"

        return(str(zip([x[0] for x in sorted(self.stateVariableNames.items(), key=operator.itemgetter(1))], self.stateVariables))
        + "\n" + funcStr)
