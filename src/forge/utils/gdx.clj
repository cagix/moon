(ns forge.utils.gdx
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx.utils Align Scaling)))

(defn- field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str/replace (str/upper-case (name k)) "-" "_")))))

(defn align [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defn scaling [k]
  (case k
    :fill Scaling/fill))

(def k->color        (partial interop/field "graphics.Color" c))
(def k->input-button (partial interop/field "Input$Buttons"))
(def k->input-key    (partial interop/field "Input$Keys"))
