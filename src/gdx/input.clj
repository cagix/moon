(ns gdx.input
  (:require [clojure.gdx :as gdx])
  (:import (com.badlogic.gdx Input)))

(defn button-just-pressed? [^Input this button]
  (.isButtonJustPressed this (gdx/k->Input$Buttons button)))

(defn key-pressed? [^Input this key]
  (.isKeyPressed this (gdx/k->Input$Keys key)))

(defn key-just-pressed? [^Input this key]
  (.isKeyJustPressed this (gdx/k->Input$Keys key)))

(defn mouse-position [^Input this]
  [(.getX this)
   (.getY this)])

(defn set-processor! [^Input this input-processor]
  (.setInputProcessor this input-processor))
