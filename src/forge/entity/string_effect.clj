(ns ^:no-doc forge.entity.string-effect
  (:require [forge.world :refer [stopped?]]))

(defn tick [{:keys [counter]} eid]
  (when (stopped? counter)
    (swap! eid dissoc *k*)))
