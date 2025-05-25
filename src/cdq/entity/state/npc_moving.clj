(ns cdq.entity.state.npc-moving
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.state :as state]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :npc-moving
  (entity/create [[_ eid movement-vector] ctx]
    {:movement-vector movement-vector
     :counter (g/create-timer ctx (* (entity/stat @eid :entity/reaction-time) 0.016))})

  (entity/tick! [[_ {:keys [counter]}] eid ctx]
    (when (g/timer-stopped? ctx counter)
      [[:tx/event eid :timer-finished]]))

  (state/enter! [[_ {:keys [movement-vector]}] eid]
    [[:tx/set-movement eid movement-vector]])

  (state/exit! [_ eid _ctx]
    [[:tx/dissoc eid :entity/movement]]))
