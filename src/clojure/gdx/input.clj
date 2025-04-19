(ns clojure.gdx.input
  (:require [clojure.gdx.interop :refer [k->input-button k->input-key]])
  (:import (com.badlogic.gdx Gdx)))

(defn button-just-pressed? [button]
  (.isButtonJustPressed Gdx/input (k->input-button button)))

(defn key-just-pressed? [key]
  (.isKeyJustPressed Gdx/input (k->input-key key)))

(defn key-pressed? [key]
  (.isKeyPressed Gdx/input (k->input-key key)))
