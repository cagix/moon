(ns forge.input
  (:require [forge.gdx :as gdx])
  (:import (com.badlogic.gdx Gdx)))

(defn button-just-pressed?
  ":left, :right, :middle, :back or :forward."
  [b]
  (.isButtonJustPressed Gdx/input (gdx/k->input-button b)))

(defn key-just-pressed?
  "See [[key-pressed?]]."
  [k]
  (.isKeyJustPressed Gdx/input (gdx/k->input-key k)))

(defn key-pressed?
  "For options see [libgdx Input$Keys docs](https://javadoc.io/doc/com.badlogicgames.gdx/gdx/latest/com/badlogic/gdx/Input.Keys.html).
  Keys are upper-cased and dashes replaced by underscores.
  For example Input$Keys/ALT_LEFT can be used with :alt-left.
  Numbers via :num-3, etc."
  [k]
  (.isKeyPressed Gdx/input (gdx/k->input-key k)))
