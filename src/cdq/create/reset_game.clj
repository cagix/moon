(ns cdq.create.reset-game)

(defn do!
  [{:keys [ctx/config]
    :as ctx}
   starting-level]
  ((requiring-resolve (:reset-game-state! config)) ctx starting-level))
