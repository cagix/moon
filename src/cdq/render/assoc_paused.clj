(ns cdq.render.assoc-paused
  (:require [cdq.entity :as entity]
            [cdq.input :as input]
            [cdq.state :as state]))

(defn- pause-game? [{:keys [ctx/config
                            ctx/player-eid]
                     :as ctx}]
  (let [controls (:controls config)]
    (or #_error
        (and (:pausing? config)
             (state/pause-game? (entity/state-obj @player-eid))
             (not (or (input/key-just-pressed? ctx (:unpause-once controls))
                      (input/key-pressed? ctx (:unpause-continously controls))))))))

(defn do! [ctx]
  (assoc ctx :ctx/paused? (pause-game? ctx)))
