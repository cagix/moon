(ns cdq.create.entity-ids
  (:require [cdq.context :as context]
            [clojure.utils :refer [defcomponent]]))

(defn create []
  (atom {}))

(defcomponent :cdq.context/entity-ids
  (context/add-entity [[_ entity-ids] eid]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid)))

  (context/remove-entity [[_ entity-ids] eid]
    (let [id (:entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))))
