(ns clojure.projectile)

(defn size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:world-unit-dimensions (:entity/image projectile))))
