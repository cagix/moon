(ns cdq.render.bind-paused
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.state :as state]))

(defn- pause-game? [{:keys [ctx/config
                            ctx/player-eid]
                     :as ctx}]
  (or #_error
      (and (:pausing? config)
           (state/pause-game? (entity/state-obj @player-eid))
           (not (or (g/key-just-pressed? ctx (get (:controls config) :unpause-once))
                    (g/key-pressed? ctx (get (:controls config) :unpause-continously)))))))

(defn do! [ctx]
  (assoc ctx :ctx/paused? (pause-game? ctx)))
