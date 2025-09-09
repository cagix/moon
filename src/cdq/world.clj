(ns cdq.world
  (:require [cdq.raycaster :as raycaster]))

(defn creatures-in-los-of
  [{:keys [ctx/active-entities
           ctx/raycaster]}
   entity]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(raycaster/line-of-sight? raycaster entity @%))
       (remove #(:entity/player? @%))))
