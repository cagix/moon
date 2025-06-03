(ns cdq.render.assoc-paused
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [clojure.input :as input]))

(defn- pause-game? [{:keys [ctx/input
                            ctx/config
                            ctx/player-eid]}]
  (let [controls (:controls config)]
    (or #_error
        (and (:pausing? config)
             (state/pause-game? (entity/state-obj @player-eid))
             (not (or (input/key-just-pressed? input (:unpause-once controls))
                      (input/key-pressed?      input (:unpause-continously controls))))))))

(defn do! [ctx]
  (assoc ctx :ctx/paused? (pause-game? ctx)))
