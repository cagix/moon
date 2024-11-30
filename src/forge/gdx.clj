(ns forge.gdx
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.utils Align Scaling)))

(defn align [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defn scaling [k]
  (case k
    :fill Scaling/fill))

(defn- field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str/replace (str/upper-case (name k)) "-" "_")))))

(defn color
  ([r g b]
   (color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defn ->color ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (field "graphics.Color" c)
        (vector? c) (apply color c)
        :else (throw (ex-info "Cannot understand color" c))))

(def ^:private k->input-button (partial field "Input$Buttons"))
(def ^:private k->input-key    (partial field "Input$Keys"))

(defn button-just-pressed?
  ":left, :right, :middle, :back or :forward."
  [b]
  (.isButtonJustPressed Gdx/input (k->input-button b)))

(defn key-just-pressed?
  "See [[key-pressed?]]."
  [k]
  (.isKeyJustPressed Gdx/input (k->input-key k)))

(defn key-pressed?
  "For options see [libgdx Input$Keys docs](https://javadoc.io/doc/com.badlogicgames.gdx/gdx/latest/com/badlogic/gdx/Input.Keys.html).
  Keys are upper-cased and dashes replaced by underscores.
  For example Input$Keys/ALT_LEFT can be used with :alt-left.
  Numbers via :num-3, etc."
  [k]
  (.isKeyPressed Gdx/input (k->input-key k)))
