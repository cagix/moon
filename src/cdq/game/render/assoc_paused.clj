(ns cdq.game.render.assoc-paused
  (:require [cdq.input :as input]
            [clojure.input]))

(def pausing? true)

(def state->pause-game? {:stunned false
                         :player-moving false
                         :player-item-on-cursor true
                         :player-idle true
                         :player-dead true
                         :active-skill false})

(defn step
  [{:keys [ctx/input
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (or #_error
                (and pausing?
                     (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                     (not (or (clojure.input/key-just-pressed? input (:unpause-once input/controls))
                              (clojure.input/key-pressed? input (:unpause-continously input/controls))))))))
