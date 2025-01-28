(ns cdq.stage
  (:require [cdq.graphics :as graphics])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(defn mouse-on-actor? [stage]
  (let [[x y] (graphics/mouse-position (Stage/.getViewport stage))]
    (Stage/.hit stage x y true)))

(defn add-actor [stage actor]
  (Stage/.addActor stage actor))

(defn root [stage]
  (Stage/.getRoot stage))
