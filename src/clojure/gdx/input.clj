(ns clojure.gdx.input
  (:import (com.badlogic.gdx Input)))

(defn button-just-pressed? [^Input input button]
  (.isButtonJustPressed input button))

(defn key-pressed? [^Input input key]
  (.isKeyPressed input key))

(defn key-just-pressed? [^Input input key]
  (.isKeyJustPressed input key))

(defn set-processor! [^Input input input-processor]
  (.setInputProcessor input input-processor))

(defn x [^Input input]
  (.getX input))

(defn y [^Input input]
  (.getY input))
