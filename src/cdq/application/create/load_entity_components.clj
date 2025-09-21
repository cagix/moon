(ns cdq.application.create.load-entity-components
  (:require [cdq.entity :as entity])
  (:import (clojure.lang APersistentVector)))

(defn do! [ctx k->fn]
  (extend APersistentVector
    entity/Entity
    {:create (fn [[k v] ctx]
               (if-let [f (k (k->fn :create))]
                 (f v ctx)
                 v))

     :create! (fn [[k v] eid ctx]
                (when-let [f (k (k->fn :create!))]
                  (f v eid ctx)))
     })
  ctx)
