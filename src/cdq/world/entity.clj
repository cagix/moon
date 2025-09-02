(ns cdq.world.entity
  (:require [cdq.gdx.math.geom :as geom]))

(defn position [{:keys [entity/body]}]
  (:body/position body))

(defn rectangle [{:keys [entity/body]}]
  (geom/body->gdx-rectangle body))

(defn overlaps? [entity other-entity]
  (geom/overlaps? (geom/body->gdx-rectangle (:entity/body entity))
                  (geom/body->gdx-rectangle (:entity/body other-entity))))
