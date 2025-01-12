(ns gdl.input
  (:require [clojure.interop :refer [k->input-button k->input-key]]))

(defn x []
  (.getX com.badlogic.gdx.Gdx/input))

(defn y []
  (.getY com.badlogic.gdx.Gdx/input))

(defn button-just-pressed? [button]
  (.isButtonJustPressed com.badlogic.gdx.Gdx/input (k->input-button button)))

(defn key-just-pressed? [key]
  (.isKeyJustPressed com.badlogic.gdx.Gdx/input (k->input-key key)))

(defn key-pressed? [key]
  (.isKeyPressed com.badlogic.gdx.Gdx/input (k->input-key key)))

(defn set-processor [input-processor]
  (.setInputProcessor com.badlogic.gdx.Gdx/input input-processor))
