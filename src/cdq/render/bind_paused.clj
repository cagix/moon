(ns cdq.render.bind-paused
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [gdl.input :as input]))

(defn- pause-game? [{:keys [ctx/config
                            ctx/player-eid]}]
  (or #_error
      (and (:pausing? config)
           (state/pause-game? (entity/state-obj @player-eid))
           (not (or (input/key-just-pressed? (get (:controls config) :unpause-once))
                    (input/key-pressed? (get (:controls config) :unpause-continously)))))))

(defn do! [ctx]
  (assoc ctx :ctx/paused? (pause-game? ctx)))
