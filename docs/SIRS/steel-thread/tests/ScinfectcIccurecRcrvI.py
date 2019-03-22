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

class AMIDOLParameters():
    def __init__(self):
        self.S_Pcinfect_S = 51999999
        self.ScinfectcI_Scinfect_IcI_Pccure_I = 1
        self.ScinfectcIccure_RcR_P = 0
        self.beta = 1.0/3.0*1.24
        self.gamma = 1.0/3.0

class infectEvent(AMIDOLEvent):
    def getName(self):
        return("InfectEvent")

    def getRate(self, v):
        rate = 1.0 / (v.beta*v.S_Pcinfect_S*v.ScinfectcI_Scinfect_IcI_Pccure_I/(v.S_Pcinfect_S+v.ScinfectcI_Scinfect_IcI_Pccure_I+v.ScinfectcIccure_RcR_P))
        return(rate)

    def isEnabled(self, v):
        return((v.beta*v.S_Pcinfect_S * v.ScinfectcI_Scinfect_IcI_Pccure_I) > 0.0)

    def fireEvent(self, v):
        v.S_Pcinfect_S -= 1.0
        v.ScinfectcI_Scinfect_IcI_Pccure_I += 1.0

class cureEvent(AMIDOLEvent):

    def getName(self):
        return("CureEvent")

    def getRate(self, v):
        rate = 1.0/(v.gamma*v.ScinfectcI_Scinfect_IcI_Pccure_I)
        return(rate)

    def isEnabled(self, v):
        return(v.gamma*v.ScinfectcI_Scinfect_IcI_Pccure_I > 0.0)

    def fireEvent(self, v):
        v.ScinfectcI_Scinfect_IcI_Pccure_I -= 1.0
        v.ScinfectcIccure_RcR_P += 1.0

class rvIRateReward(AMIDOLRateReward):
    def __init__(self):
        self.rewards = dict()
        self.samplePoints = [0.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 85.0, 90.0, 95.0, 100.0]
        self.delays = list()
        self.delays.append(self.samplePoints[0])
        idx = 1
        lastX = self.samplePoints[0]
        for x in self.samplePoints[1:]:
            self.delays.append(x - lastX)
            lastX = x

    def accumulateReward(self, env, params):
        self.rewards[env.now] = params.ScinfectcI_Scinfect_IcI_Pccure_I


    def getDelay(self, params):
        return(self.delays.pop(0))

params = AMIDOLParameters()
cure = cureEvent()
infect = infectEvent()
rvI = rvIRateReward()

env = simpy.Environment()
cureProcess = env.process(cure.simpyProcess(env, params))
processList.append(cureProcess)
infectProcess = env.process(infect.simpyProcess(env, params))
processList.append(infectProcess)
rvIProcess = env.process(rvI.simpyProcess(env, params))

env.run(until=90.0)

print(rvI.rewards)
