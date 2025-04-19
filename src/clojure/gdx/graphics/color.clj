(ns clojure.gdx.graphics.color
  (:refer-clojure :exclude [munge])
  (:require [clojure.gdx.interop :as interop])
  (:import (com.badlogic.gdx.graphics Color)))

(defn- create
  ([r g b]
   (create r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defn munge ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (interop/k->color c)
        (vector? c) (apply create c)
        :else (throw (ex-info "Cannot understand color" c))))
