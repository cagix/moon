(ns clojure.gdx.graphics.g2d.bitmap-font
  (:require [clojure.gdx.utils.align :as align]
            clojure.graphics.bitmap-font
            [clojure.string :as str]))

(let [text-height (fn [^com.badlogic.gdx.graphics.g2d.BitmapFont font text]
                    (-> text
                        (str/split #"\n")
                        count
                        (* (.getLineHeight font))))]
  (extend-type com.badlogic.gdx.graphics.g2d.BitmapFont
    clojure.graphics.bitmap-font/BitmapFont
    (configure! [font {:keys [scale enable-markup?  use-integer-positions?]}]
      (.setScale (.getData font) scale)
      (set! (.markupEnabled (.getData font)) enable-markup?)
      (.setUseIntegerPositions font use-integer-positions?)
      font)

    (draw! [font batch {:keys [scale text x y up? h-align target-width wrap?]}]
      {:pre [(or (nil? h-align)
                 (contains? align/k->value h-align))]}
      (let [old-scale (.scaleX (.getData font))]
        (.setScale (.getData font) (float (* old-scale scale)))
        (.draw font
               batch
               text
               (float x)
               (float (+ y (if up? (text-height font text) 0)))
               (float target-width)
               (get align/k->value (or h-align :center))
               wrap?)
        (.setScale (.getData font) (float old-scale))))))
