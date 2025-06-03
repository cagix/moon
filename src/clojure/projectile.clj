(ns clojure.projectile)

(defn size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:sprite/world-unit-dimensions (:entity/image projectile))))
