(ns ^:no-doc anvil.entity.state.stunned
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [cdq.context :refer [timer stopped?]]
            [gdl.context :as c]))

(defmethods :stunned
  (component/->v [[_ eid duration] c]
    {:eid eid
     :counter (timer c duration)})

  (component/tick [[_ {:keys [counter]}] eid c]
    (when (stopped? c counter)
      (entity/event c eid :effect-wears-off)))

  (component/render-below [_ entity c]
    (c/circle c (:position entity) 0.5 [1 1 1 0.6])))
