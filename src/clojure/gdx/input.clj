(ns clojure.gdx.input
  (:require [clojure.gdx.interop :as interop])
  (:import (com.badlogic.gdx Input)))

(defn button-just-pressed? [^Input input button]
  (.isButtonJustPressed input (interop/k->input-button button)))

(defn key-pressed? [^Input input key]
  (.isKeyPressed input (interop/k->input-key key)))

(defn key-just-pressed? [^Input input key]
  (.isKeyJustPressed input (interop/k->input-key key)))

(defn set-processor! [^Input input input-processor]
  (.setInputProcessor input input-processor))

(defn mouse-position [^Input input]
  [(.getX input)
   (.getY input)])
