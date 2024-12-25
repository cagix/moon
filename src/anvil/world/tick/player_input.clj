(ns anvil.world.tick.player-input
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [cdq.context :as world]
            [anvil.world.tick :as tick]))

(defn-impl tick/player-input [c]
  (component/manual-tick (entity/state-obj @world/player-eid)
                         c))
