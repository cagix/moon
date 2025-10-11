(ns clojure.gdx.graphics
  (:import (com.badlogic.gdx Graphics)))

(defn set-cursor! [^Graphics graphics cursor]
  (.setCursor graphics cursor))

(defn frames-per-second [^Graphics graphics]
  (.getFramesPerSecond graphics))

(defn delta-time [^Graphics graphics]
  (.getDeltaTime graphics))
