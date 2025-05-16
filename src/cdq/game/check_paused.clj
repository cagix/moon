(ns cdq.game.check-paused
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :as utils]
            [gdl.input :as input]))

(defn do! []
  (utils/bind-root #'ctx/paused? (or #_error
                                     (and ctx/pausing?
                                          (state/pause-game? (entity/state-obj @ctx/player-eid))
                                          (not (or (input/key-just-pressed? (get ctx/controls :unpause-once))
                                                   (input/key-pressed? (get ctx/controls :unpause-continously))))))))
