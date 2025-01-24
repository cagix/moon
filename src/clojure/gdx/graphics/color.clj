(ns clojure.gdx.graphics.color
  (:refer-clojure :exclude [munge])
  (:require [clojure.gdx.interop :as interop])
  (:import (com.badlogic.gdx.graphics Color)))

(def ^Color ^{:doc "The color black."} black Color/BLACK)
(def ^Color ^{:doc "The color white."} white Color/WHITE)

(defn create
  "Creates a color object, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the internal values after execution."
  ([r g b]
   (create r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defn munge ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (interop/k->color c)
        (vector? c) (apply create c)
        :else (throw (ex-info "Cannot understand color" c))))
