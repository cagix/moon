(ns forge.utils.gdx
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx.utils Align Scaling)))

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
