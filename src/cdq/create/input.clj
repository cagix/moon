(ns cdq.create.input
  (:require [cdq.g :as g]
            [clojure.gdx.input :as input])
  (:import (com.badlogic.gdx Gdx)))

(def ^:private -k :ctx/input)

(defn do! [ctx]
  (extend (class ctx)
    g/Input
    {:button-just-pressed? (fn [ctx button] (input/button-just-pressed? (-k ctx) button))
     :key-pressed?         (fn [ctx key]    (input/key-pressed?         (-k ctx) key))
     :key-just-pressed?    (fn [ctx key]    (input/key-just-pressed?    (-k ctx) key))
     :mouse-position       (fn [ctx]        (input/mouse-position       (-k ctx)))})
  (assoc ctx -k Gdx/input))
