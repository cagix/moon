(ns cdq.stage
  (:require [cdq.graphics :as graphics])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(defn hit [stage x y touchable?]
  (Stage/.hit stage x y touchable?))

(defn mouse-on-actor? [stage]
  (let [[x y] (graphics/mouse-position (.getViewport stage))]
    (hit stage x y true)))

(defn add-actor [stage actor]
  (Stage/.addActor stage actor))

(defn clear [stage]
  (Stage/.clear stage))

(defn act [stage]
  (Stage/.act stage))

(defn draw [stage]
  (Stage/.draw stage))

(defn root [stage]
  (Stage/.getRoot stage))
