using DiffEqBiological
using DifferentialEquations
using Plots
using DiffEqMonteCarlo

# float: Current state dependent rate of infection
rateInfect(u,p,t) = p[1]*u[1]*u[2]/(u[1]+u[2]+u[3])
# Delta functions associated with infect event
function eventInfect!(integrator)
  integrator.u[1] -= 1
  integrator.u[2] += 1
end
jump1 = ConstantRateJump(rateInfect,eventInfect!)

# float: Current state dependent rate of recovery
rateRecover(u,p,t) = p[2]*u[2]
# Delta functions associated with recovery event
function eventRecover!(integrator)
  integrator.u[2] -= 1
  integrator.u[3] += 1
end
jump2 = ConstantRateJump(rateRecover,eventRecover!)

# float array: Initial values of s, i, and r
u0 = [500.0; 1.0; 0.0]
# float tuple: time domain of the problem
tspan = (0.0,100.0)
# float array: values of beta and gamma
p = (2.0 * 1.0/3.0, 1.0/3.0)
prob = DiscreteProblem(u0,tspan,p)
jump_prob = JumpProblem(prob,Direct(),jump1,jump2)
#sol = solve(jump_prob,FunctionMap())
monte_prob = MonteCarloProblem(jump_prob)
@time sol_monte = solve(monte_prob, num_monte=1000)
summ = EnsembleSummary(sol_monte)
plot(summ)
savefig("Julia-Gillespie.pdf")
