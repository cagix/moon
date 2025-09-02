(ns cdq.world.entity
  (:require [cdq.gdx.math.geom :as geom]))

(defn position [{:keys [entity/body]}]
  (:body/position body))

(defn rectangle [{:keys [entity/body]}]
  (geom/body->gdx-rectangle body))
