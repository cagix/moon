(ns anvil.world.tick.player-input
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.world.tick :as tick]))

(defn-impl tick/player-input [{:keys [cdq.context/player-eid] :as c}]
  (component/manual-tick (entity/state-obj @player-eid)
                         c))
