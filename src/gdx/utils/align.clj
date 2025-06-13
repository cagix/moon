(ns gdx.utils.align
  (:import (com.badlogic.gdx.utils Align)))

(let [mapping {:bottom       Align/bottom
               :bottom-left  Align/bottomLeft
               :bottom-right Align/bottomRight
               :center       Align/center
               :left         Align/left
               :right        Align/right
               :top          Align/top
               :top-left     Align/topLeft
               :top-right    Align/topRight}]
  (defn k->value [k]
    (when-not (contains? mapping k)
      (throw (IllegalArgumentException. (str "Unknown Key: " k ". \nOptions are:\n" (sort (keys mapping))))))
    (k mapping)))
