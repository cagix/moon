(ns clojure.gdx.scene2d.stage
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(defn viewport [^Stage stage]
  (.getViewport stage))

(defn add-actor! [^Stage stage actor]
  (.addActor stage actor))

(defn root [^Stage stage]
  (.getRoot stage))
