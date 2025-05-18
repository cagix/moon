(ns cdq.application.render.bind-paused
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :refer [bind-root]]
            [gdl.input :as input]))

(defn- pause-game? []
  (or #_error
      (and ctx/pausing?
           (state/pause-game? (entity/state-obj @ctx/player-eid))
           (not (or (input/key-just-pressed? (get ctx/controls :unpause-once))
                    (input/key-pressed? (get ctx/controls :unpause-continously)))))))

(defn do! []
  (bind-root #'ctx/paused? (pause-game?)))
