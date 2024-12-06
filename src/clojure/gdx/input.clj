(ns clojure.gdx.input
  (:require [clojure.gdx.interop :refer [static-field]])
  (:import (com.badlogic.gdx Gdx)))

(def ^:private k->input-button (partial static-field "Input$Buttons"))
(def ^:private k->input-key    (partial static-field "Input$Keys"))

(defn set-processor [processor]
  (.setInputProcessor Gdx/input processor))

(defn button-just-pressed? [b]
  (.isButtonJustPressed Gdx/input (k->input-button b)))

(defn key-just-pressed? [k]
  (.isKeyJustPressed Gdx/input (k->input-key k)))

(defn key-pressed? [k]
  (.isKeyPressed Gdx/input (k->input-key k)))

(defn x [] (.getX Gdx/input))
(defn y [] (.getY Gdx/input))
