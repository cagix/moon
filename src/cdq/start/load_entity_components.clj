(ns cdq.start.load-entity-components
  (:require [cdq.entity :as entity]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.symbol :as symbol])
  (:import (clojure.lang APersistentVector)))

(defn do! [ctx edn-resource]
  (let [k->fn (-> edn-resource
                  io/resource
                  slurp
                  edn/read-string
                  symbol/require-resolve-symbols)]
    (extend APersistentVector
      entity/Entity
      {:tick (fn [[k v] eid ctx]
               (when-let [f (k (k->fn :tick))]
                 (f v eid ctx)))

       :create (fn [[k v] ctx]
                 (if-let [f (k (k->fn :create))]
                   (f v ctx)
                   v))

       :create! (fn [[k v] eid ctx]
                  (when-let [f (k (k->fn :create!))]
                    (f v eid ctx)))
       }))
  ctx)
