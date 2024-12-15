(ns ^:no-doc anvil.entity.state.stunned
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.world :refer [timer stopped?]]
            [gdl.graphics :as g]
            [gdl.utils :refer [defmethods]]))

(defmethods :stunned
  (component/->v [[_ eid duration]]
    {:eid eid
     :counter (timer duration)})

  (component/tick [[_ {:keys [counter]}] eid]
    (when (stopped? counter)
      (entity/event eid :effect-wears-off)))

  (component/render-below [_ entity]
    (g/circle (:position entity) 0.5 [1 1 1 0.6])))
