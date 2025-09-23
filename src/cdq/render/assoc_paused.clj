(ns cdq.render.assoc-paused
  (:require [clojure.input :as input]))

(def ^:private pausing? true)

(def ^:private state->pause-game?
  {:stunned false
   :player-moving false
   :player-item-on-cursor true
   :player-idle true
   :player-dead true
   :active-skill false})

(defn do!
  [{:keys [ctx/input
           ctx/controls
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (or #_error
                (and pausing?
                     (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                     (not (or (input/key-just-pressed? input (:unpause-once controls))
                              (input/key-pressed?      input (:unpause-continously controls))))))))
