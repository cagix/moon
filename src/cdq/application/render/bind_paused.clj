(ns cdq.application.render.bind-paused
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [gdl.input :as input]))

(defn- pause-game? [{:keys [ctx/pausing?
                            ctx/player-eid
                            ctx/controls]}]
  (or #_error
      (and pausing?
           (state/pause-game? (entity/state-obj @player-eid))
           (not (or (input/key-just-pressed? (get controls :unpause-once))
                    (input/key-pressed? (get controls :unpause-continously)))))))

(defn do! [ctx]
  (assoc :ctx/paused? (pause-game? ctx)))
