(ns clojure.gdx.input
  (:require [com.badlogic.gdx.input.buttons :as input-buttons]
            [com.badlogic.gdx.input.keys    :as input-keys])
  (:import (com.badlogic.gdx Input)))

(defn button-just-pressed? [^Input this button]
  {:pre [(contains? input-buttons/k->value button)]}
  (.isButtonJustPressed this (input-buttons/k->value button)))

(defn key-pressed? [^Input this key]
  (assert (contains? input-keys/keyword->value key)
          (str "(pr-str key): "(pr-str key)))
  (.isKeyPressed this (input-keys/keyword->value key)))

(defn key-just-pressed? [^Input this key]
  {:pre [(contains? input-keys/keyword->value key)]}
  (.isKeyJustPressed this (input-keys/keyword->value key)))

(defn set-processor! [^Input this input-processor]
  (.setInputProcessor this input-processor))

(defn mouse-position [^Input this]
  [(.getX this)
   (.getY this)])
