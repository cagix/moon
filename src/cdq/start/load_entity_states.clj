(ns cdq.start.load-entity-states
  (:require [cdq.entity.state :as state])
  (:import (clojure.lang APersistentVector)))

(defn do! [ctx fn->k->var]
  (extend APersistentVector
    state/State
    {:create (fn [[k v] eid ctx]
               (if-let [f (k (:create fn->k->var))]
                 (f eid v ctx)
                 (or v :something))) ; nil components are not tick'ed

     :handle-input (fn [[k v] eid ctx]
                     (if-let [f (k (:handle-input fn->k->var))]
                       (f eid ctx)
                       nil))

     :cursor (fn [[k v] eid ctx]
               (let [->cursor (k (:cursor fn->k->var))]
                 (if (keyword? ->cursor)
                   ->cursor
                   (->cursor eid ctx))))

     :enter (fn [[k v] eid]
              (when-let [f (k (:enter fn->k->var))]
                (f v eid)))

     :exit (fn [[k v] eid ctx]
             (when-let [f (k (:exit fn->k->var))]
               (f v eid ctx)))

     :clicked-inventory-cell (fn [[k v] eid cell]
                               (when-let [f (k (:clicked-inventory-cell fn->k->var))]
                                 (f eid cell)))
     })
  ctx)
