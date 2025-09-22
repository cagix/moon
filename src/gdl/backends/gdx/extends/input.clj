(ns gdl.backends.gdx.extends.input
  (:require [com.badlogic.gdx.input.buttons :as input.buttons]
            [com.badlogic.gdx.input.keys :as input.keys]
            [gdl.input])
  (:import (com.badlogic.gdx Input)))

(extend-type Input
  gdl.input/Input
  (button-just-pressed? [this button]
    {:pre [(contains? input.buttons/k->value button)]}
    (.isButtonJustPressed this (input.buttons/k->value button)))

  (key-pressed? [this key]
    (assert (contains? input.keys/k->value key)
            (str "(pr-str key): "(pr-str key)))
    (.isKeyPressed this (input.keys/k->value key)))

  (key-just-pressed? [this key]
    {:pre [(contains? input.keys/k->value key)]}
    (.isKeyJustPressed this (input.keys/k->value key)))

  (set-processor! [this input-processor]
    (.setInputProcessor this input-processor))

  (mouse-position [this]
    [(.getX this)
     (.getY this)]))
