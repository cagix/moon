(ns cdq.application-state
  (:require [cdq.create.db]
            [cdq.g :as g]
            [gdl.application]))

(defn add-component [ctx k v]
  {:pre [(not (contains? ctx k))]}
  (assoc ctx k v))

(extend-type gdl.application.Context
  g/Config
  (config [{:keys [ctx/config]} key]
    (get config key)))

(defn create! [config]
  (-> (gdl.application/create-state! config)
      (add-component :ctx/config config)
      (cdq.create.db/add-db config)
      ((requiring-resolve 'cdq.game-state/create!))))
