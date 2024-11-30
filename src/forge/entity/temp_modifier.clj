(ns ^:no-doc forge.entity.temp-modifier
  (:require [forge.entity.components :as entity]
            [forge.world :refer [stopped?]]))

(defn tick [{:keys [modifiers counter]} eid]
  (when (stopped? counter)
    (swap! eid dissoc *k*)
    (swap! eid entity/remove-mods modifiers)))
