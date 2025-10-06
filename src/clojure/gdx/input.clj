(ns clojure.gdx.input
  (:require [com.badlogic.gdx.input :as input]
            [com.badlogic.gdx.input.buttons :as input-buttons]
            [com.badlogic.gdx.input.keys    :as input-keys]))

(defn button-just-pressed? [this button]
  {:pre [(contains? input-buttons/k->value button)]}
  (input/button-just-pressed? this (input-buttons/k->value button)))

(defn key-pressed? [this key]
  (assert (contains? input-keys/k->value key)
          (str "(pr-str key): "(pr-str key)))
  (input/key-pressed? this (input-keys/k->value key)))

(defn key-just-pressed? [this key]
  {:pre [(contains? input-keys/k->value key)]}
  (input/key-just-pressed? this (input-keys/k->value key)))

(defn set-processor! [this input-processor]
  (input/set-processor! this input-processor))

(defn mouse-position [this]
  [(input/x this)
   (input/y this)])
