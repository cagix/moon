(ns forge.world.entity-ids
  (:require [forge.utils :refer [bind-root]]))

(declare entity-ids)

(defn init [_]
  (bind-root entity-ids {}))

(defn all-entities []
  (vals entity-ids))

(defn add-entity [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (alter-var-root #'entity-ids assoc id eid)))

(defn remove-entity [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? entity-ids id))
    (alter-var-root #'entity-ids dissoc id)))

