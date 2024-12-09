(ns anvil.level)

(defmulti generate-level* (fn [world] (:world/generator world)))

(defn generate-level [world-props]
  (assoc (generate-level* world-props)
         :world/player-creature
         (:world/player-creature world-props)))
