(ns cdq.entity.state.npc-dead
  (:require [cdq.state :as state]
            [cdq.utils :refer [defmethods]]))

(defmethods :npc-dead
  (state/enter! [_ eid]
    [[:tx/mark-destroyed eid]]))
