(ns anvil.world.tick.player-input
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.world :as world]
            [anvil.world.tick :as tick]
            [gdl.utils :refer [defn-impl]]))

(defn-impl tick/player-input []
  (component/manual-tick (entity/state-obj @world/player-eid)))
