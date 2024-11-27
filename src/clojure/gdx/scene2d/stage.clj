(ns clojure.gdx.scene2d.stage
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(defn clear [^Stage stage]
  (.clear stage))

(defn add [^Stage stage actor]
  (.addActor stage actor))

(defn act [^Stage stage]
  (.act stage))

(defn draw [^Stage stage]
  (.draw stage))

(defn hit [^Stage stage [x y] & {:keys [touchable?]}]
  (.hit stage x y (boolean touchable?)))
