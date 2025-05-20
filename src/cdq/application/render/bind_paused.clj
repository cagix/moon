(ns cdq.application.render.bind-paused
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :refer [bind-root]]
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
  (bind-root #'ctx/paused? (pause-game? ctx)))
