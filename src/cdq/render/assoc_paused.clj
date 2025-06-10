(ns cdq.render.assoc-paused
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [gdl.input :as input]))

(defn do! [{:keys [ctx/gdx
                   ctx/config
                   ctx/player-eid]}
           {:keys [pausing?]}]
  (let [controls (:controls config)]
    (or #_error
        (and pausing?
             (state/pause-game? (entity/state-obj @player-eid))
             (not (or (input/key-just-pressed? gdx (:unpause-once controls))
                      (input/key-pressed?      gdx (:unpause-continously controls))))))))
