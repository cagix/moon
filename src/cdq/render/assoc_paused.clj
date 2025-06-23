(ns cdq.render.assoc-paused
  (:require [clojure.input :as input]))

(defn do! [{:keys [ctx/input
                   ctx/world
                   ctx/config]
            :as ctx}
           {:keys [pausing?
                   state->pause-game?]}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (let [controls (:controls config)]
              (or #_error
                  (and pausing?
                       (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                       (not (or (input/key-just-pressed? input (:unpause-once controls))
                                (input/key-pressed?      input (:unpause-continously controls)))))))))
