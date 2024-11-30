(ns forge.gdx
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics Color)
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

(def k->color        (partial field "graphics.Color"))
(def k->input-button (partial field "Input$Buttons"))
(def k->input-key    (partial field "Input$Keys"))

(defn color
  ([r g b]
   (color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defn ->color ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (k->color c)
        (vector? c) (apply color c)
        :else (throw (ex-info "Cannot understand color" c))))
