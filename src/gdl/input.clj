(ns gdl.input
  (:require [gdl.utils :refer [gdx-static-field]])
  (:import (com.badlogic.gdx Gdx)))

(def ^:private k->input-button (partial gdx-static-field "Input$Buttons"))
(def ^:private k->input-key    (partial gdx-static-field "Input$Keys"))

(defn button-just-pressed? [b]
  (.isButtonJustPressed Gdx/input (k->input-button b)))

(defn key-just-pressed? [k]
  (.isKeyJustPressed Gdx/input (k->input-key k)))

(defn key-pressed? [k]
  (.isKeyPressed Gdx/input (k->input-key k)))
