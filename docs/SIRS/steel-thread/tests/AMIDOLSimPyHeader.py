import numpy as np
import simpy

processList = list()

class AMIDOLRateReward():
    def __init__(self):
        self.rewards = dict()

    def accumulateReward(self, env, params):
        return(0.0)

    def getDelay(self, params):
        return(simpy.core.Infinity)

    def isEnabled(self, params):
        return(True)

    def simpyProcess(self, env, params):
        while(True):
            try:
                if(self.isEnabled(params)):
                    yield env.timeout(self.getDelay(params))
                    self.accumulateReward(env, params)
                else:
                    yield env.timeout(simpy.core.Infinity)
            except simpy.Interrupt as i:
                continue
        print(self.getName() + " terminating.")

class AMIDOLEvent():
    def getName(self):
        return("GenericEvent")

    def getRate(self, params):
        return(1.0)

    def getDelay(self, params):
        delay = np.random.exponential(self.getRate(params))
        return(delay)

    def isEnabled(self, params):
        return(True)

    def fireEvent(self, params):
        return(params)

    def reactivation(self, env):
        global processList
        for process in processList:
            if (process != env.active_process):
                process.interrupt()

    def simpyProcess(self, env, params):
        while(True):
            try:
                if(self.isEnabled(params)):
                    yield env.timeout(self.getDelay(params))
                    self.fireEvent(params)
                else:
                    yield env.timeout(simpy.core.Infinity)
                self.reactivation(env)
            except simpy.Interrupt as i:
                continue
        print(self.getName() + " terminating.")
