(ns cdq.render.bind-paused
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.state :as state]))

(defn- pause-game? [{:keys [ctx/player-eid] :as ctx}]
  (let [controls (g/config ctx :controls)]
    (or #_error
        (and (g/config ctx :pausing?)
             (state/pause-game? (entity/state-obj @player-eid))
             (not (or (g/key-just-pressed? ctx (:unpause-once controls))
                      (g/key-pressed? ctx (:unpause-continously controls))))))))

(defn do! [ctx]
  (assoc ctx :ctx/paused? (pause-game? ctx)))
