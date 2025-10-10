(ns clojure.gdx.bitmap-font
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx.utils Align)))

(defprotocol BitmapFont
  (draw! [_ batch {:keys [scale text x y up? h-align target-width wrap?]}]))

(extend-type com.badlogic.gdx.graphics.g2d.BitmapFont
  BitmapFont
  (draw! [font batch {:keys [scale text x y up? h-align target-width wrap?]}]
    (let [text-height (fn []
                        (-> text
                            (str/split #"\n")
                            count
                            (* (.getLineHeight font))))
          old-scale (.scaleX (.getData font))]
      (.setScale (.getData font) (* old-scale scale))
      (.draw font
             batch
             text
             (float x)
             (float (+ y (if up? (text-height) 0)))
             (float target-width)
             (or h-align Align/center)
             wrap?)
      (.setScale (.getData font) old-scale))))
