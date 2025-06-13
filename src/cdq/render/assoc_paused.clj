(ns cdq.render.assoc-paused ; cdq.input ?!
  (:require [gdl.input :as input]))

(defn do! [{:keys [ctx/input
                   ctx/config
                   ctx/player-eid]}
           {:keys [pausing?
                   state->pause-game?]}]
  (let [controls (:controls config)]
    (or #_error
        (and pausing?
             (state->pause-game? (:state (:entity/fsm @player-eid)))
             (not (or (input/key-just-pressed? input (:unpause-once controls))
                      (input/key-pressed?      input (:unpause-continously controls))))))))
