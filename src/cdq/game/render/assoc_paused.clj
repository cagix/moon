(ns cdq.game.render.assoc-paused
  (:require [cdq.input :as input]
            [clojure.gdx :as gdx]))

(def pausing? true)

(def state->pause-game? {:stunned false
                         :player-moving false
                         :player-item-on-cursor true
                         :player-idle true
                         :player-dead true
                         :active-skill false})

(defn step
  [{:keys [ctx/gdx
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (or #_error
                (and pausing?
                     (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                     (not (or (gdx/key-just-pressed? gdx (:unpause-once input/controls))
                              (gdx/key-pressed? gdx (:unpause-continously input/controls))))))))
