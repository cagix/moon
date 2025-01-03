(ns cdq.effect.sound
  (:require [clojure.gdx :refer [play]]))

(defn applicable? [_ _ctx]
  true)

(defn useful? [_ _ _c]
  false)

(defn handle [[_ sound] _ctx c]
  (play sound))
