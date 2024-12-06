(ns clojure.gdx.graphics.color
  (:require [clojure.gdx.interop :refer [static-field]])
  (:import (com.badlogic.gdx.graphics Color Colors)))

(def ^Color black Color/BLACK)
(def ^Color white Color/WHITE)

(defn ->color
  ([r g b]
   (->color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a)))
  (^Color [c]
          (cond (= Color (class c)) c
                (keyword? c) (static-field "graphics.Color" c)
                (vector? c) (apply ->color c)
                :else (throw (ex-info "Cannot understand color" c)))))

(defn add [name-str color]
  (Colors/put name-str (->color color)))
