(ns ^:no-doc anvil.entity.state.stunned
  (:require [anvil.entity :as entity]
            [cdq.context :refer [timer stopped?]]
            [clojure.component :as component :refer [defcomponent]]
            [gdl.context :as c]))

(defcomponent :stunned
  (component/create [[_ eid duration] c]
    {:eid eid
     :counter (timer c duration)})

  (component/tick [[_ {:keys [counter]}] eid c]
    (when (stopped? c counter)
      (entity/event c eid :effect-wears-off)))

  (component/render-below [_ entity c]
    (c/circle c (:position entity) 0.5 [1 1 1 0.6])))
