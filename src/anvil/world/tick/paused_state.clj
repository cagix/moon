(ns anvil.world.tick.paused-state
  (:require [anvil.component :as component]
            [anvil.controls :as controls]
            [anvil.world :as world]
            [anvil.world.tick :as tick]
            [anvil.entity :as entity]))

(defmethod component/pause-game? :active-skill          [_] false)
(defmethod component/pause-game? :stunned               [_] false)
(defmethod component/pause-game? :player-moving         [_] false)
(defmethod component/pause-game? :player-item-on-cursor [_] true)
(defmethod component/pause-game? :player-idle           [_] true)
(defmethod component/pause-game? :player-dead           [_] true)

(defn-impl tick/paused-state [pausing?]
  (bind-root world/paused? (or world/error
                               (and pausing?
                                    (component/pause-game? (entity/state-obj @world/player-eid))
                                    (not (controls/unpaused?))))))
