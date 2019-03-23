import numpy as np
import simpy
from collections import Counter
from tqdm import tqdm

processList = list()

class AMIDOLRateReward():
    def __init__(self):
        self.rewards = dict()

    def accumulateReward(self, params):
        return(0.0)

    def getDelay(self, params):
        return(np.Infinity)

    def getSample(self, params):
        return(np.Infinity)

    def isEnabled(self, params):
        return(True)


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

def dummyFire(self, params):
    return(params)

class AMIDOLSim():
    def __init__(self, until=10.0):
        self.clock = 0.0
        self.until = until
        self.eventList = []
        self.rateRewardList = []
        self.processed = 0.0

    def registerEvent(self, event):
        self.eventList.append(event)

    def registerRateReward(self, reward):
        self.rateRewardList.append(reward)

    def stepSim(self, params):
        self.processed += 1.0
        nextEventTime = np.Infinity
        nextEventFire = dummyFire
        nextRewardTime = np.Infinity
        nextRewardAccum = dummyFire

        for event in self.eventList:
            if (event.isEnabled(params)):
                delay = event.getDelay(params)
                if ((delay + self.clock) < nextEventTime):
                    nextEventTime = delay + self.clock
                    nextEventFire = event.fireEvent

        for reward in self.rateRewardList:
            sample = reward.getSample(params)
            if ((sample) < nextRewardTime):
                nextRewardTime = sample
                nextRewardAccum = reward.accumulateReward

        if ((nextEventTime is np.Infinity) and (nextRewardTime is np.Infinity)):
            self.clock = np.Infinity
            return()
        if (nextEventTime < nextRewardTime):
            self.clock = nextEventTime
            nextEventFire(params)
        else:
            self.clock = nextRewardTime
            nextRewardAccum(self.clock, params)

    def runSim(self, params):
        self.clock = 0.0
        while((not self.eventList == []) and (not self.rateRewardList == []) and (self.clock < self.until)):
            self.stepSim(params)

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
        self.samplePoints = list(np.arange(0.0, 200.0, 5.0))
        self.delays = list()
        self.delays.append(self.samplePoints[0])
        idx = 1
        lastX = self.samplePoints[0]
        for x in self.samplePoints[1:]:
            self.delays.append(x - lastX)
            lastX = x

    def accumulateReward(self, now, params):
        self.delays.pop(0)
        self.samplePoints.pop(0)
        self.rewards[now] = params.ScinfectcI_Scinfect_IcI_Pccure_I


    def getDelay(self, params):
        if (self.delays):
            return(self.delays[0])
        else:
            return(np.Infinity)

    def getSample(self, params):
        if (self.samplePoints):
            return(self.samplePoints[0])
        else:
            return(np.Infinity)

maxRuns = 100

rvICounter = dict()
processed = 0.0

for trace in tqdm(range(0, maxRuns)):
    params = AMIDOLParameters()
    cure = cureEvent()
    infect = infectEvent()
    rvI = rvIRateReward()

    sim = AMIDOLSim(until=max(rvI.samplePoints))
    sim.registerEvent(cure)
    sim.registerEvent(infect)
    sim.registerRateReward(rvI)
    sim.runSim(params)

    if not(rvICounter):
        for key in rvI.rewards:
            rvICounter[key] = rvI.rewards[key] / float(maxRuns)
    else:
        for key in rvI.rewards:
            rvICounter[key] += rvI.rewards[key] / float(maxRuns)

    processed += sim.processed

print sorted(rvICounter.items())
print(processed)
