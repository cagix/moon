(ns forge.input
  (:require [forge.utils.gdx :as interop])
  (:import (com.badlogic.gdx Gdx)))

(def ^:private input-button (partial interop/field "Input$Buttons"))
(def ^:private input-key    (partial interop/field "Input$Keys"))

(defn button-just-pressed?
  ":left, :right, :middle, :back or :forward."
  [b]
  (.isButtonJustPressed Gdx/input (input-button b)))

(defn key-just-pressed?
  "See [[key-pressed?]]."
  [k]
  (.isKeyJustPressed Gdx/input (input-key k)))

(defn key-pressed?
  "For options see [libgdx Input$Keys docs](https://javadoc.io/doc/com.badlogicgames.gdx/gdx/latest/com/badlogic/gdx/Input.Keys.html).
  Keys are upper-cased and dashes replaced by underscores.
  For example Input$Keys/ALT_LEFT can be used with :alt-left.
  Numbers via :num-3, etc."
  [k]
  (.isKeyPressed Gdx/input (input-key k)))

(defn set-processor [processor]
  (.setInputProcessor Gdx/input processor))
