(ns cdq.entity.body
  (:require [cdq.gdx.math.geom :as geom]))

(defn overlaps? [body other-body]
  (geom/overlaps? (geom/body->gdx-rectangle body)
                  (geom/body->gdx-rectangle other-body)))
