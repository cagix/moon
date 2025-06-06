(ns clojure.gdx.input.buttons
  (:import (com.badlogic.gdx Input$Buttons)))

(def mapping
  {:back    Input$Buttons/BACK
   :forward Input$Buttons/FORWARD
   :left    Input$Buttons/LEFT
   :middle  Input$Buttons/MIDDLE
   :right   Input$Buttons/RIGHT})

(defn ->from-k [k]
  (when-not (contains? mapping k)
    (throw (IllegalArgumentException. (str "Unknown Button: " k ". \nOptions are:\n" (sort (keys mapping))))))
  (k mapping))
