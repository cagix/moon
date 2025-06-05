(ns cdq.render.assoc-paused
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [clojure.x :as x]))

(defn do! [{:keys [ctx/config
                   ctx/player-eid]
            :as ctx}
           {:keys [pausing?]}]
  (let [controls (:controls config)]
    (or #_error
        (and pausing?
             (state/pause-game? (entity/state-obj @player-eid))
             (not (or (x/key-just-pressed? ctx (:unpause-once controls))
                      (x/key-pressed?      ctx (:unpause-continously controls))))))))
