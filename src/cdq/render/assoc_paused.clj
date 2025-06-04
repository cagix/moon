(ns cdq.render.assoc-paused
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [clojure.input :as input]))

(defn do! [{:keys [ctx/input
                   ctx/config
                   ctx/player-eid]
            :as ctx}
           {:keys [pausing?]}]
  (assoc ctx :ctx/paused?
         (let [controls (:controls config)]
           (or #_error
               (and pausing?
                    (state/pause-game? (entity/state-obj @player-eid))
                    (not (or (input/key-just-pressed? input (:unpause-once controls))
                             (input/key-pressed?      input (:unpause-continously controls)))))))))
