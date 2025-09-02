(ns cdq.world.entity
  (:require [cdq.gdx.math.geom :as geom]
            [cdq.gdx.math.vector2 :as v]))

(defn position [{:keys [entity/body]}]
  (:body/position body))

(defn rectangle [{:keys [entity/body]}]
  (geom/body->gdx-rectangle body))

(defn overlaps? [entity other-entity]
  (geom/overlaps? (geom/body->gdx-rectangle (:entity/body entity))
                  (geom/body->gdx-rectangle (:entity/body other-entity))))

(defn in-range? [entity target* maxrange]
  (< (- (float (v/distance (position entity)
                           (position target*)))
        (float (/ (:body/width (:entity/body entity))  2))
        (float (/ (:body/width (:entity/body target*)) 2)))
     (float maxrange)))
