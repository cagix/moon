(ns cdq.entity.state.npc-dead
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :npc-dead
  (entity/create [[_ eid]]
    {:eid eid})

  (state/enter! [[_ {:keys [eid]}]]
    [[:tx/mark-destroyed eid]]))
