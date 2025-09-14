(ns cdq.world-fns.modules.prepare-creature-properties
  (:require [cdq.world-fns.creature-tiles :as creature-tiles]))

(defn do!
  [{:keys [creature-properties
           graphics]
    :as world-fn-ctx}]
  (update world-fn-ctx :creature-properties creature-tiles/prepare graphics))
