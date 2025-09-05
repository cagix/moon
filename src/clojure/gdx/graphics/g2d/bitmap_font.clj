(ns clojure.gdx.graphics.g2d.bitmap-font
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)))

(defn configure!
  [^BitmapFont font
   {:keys [scale
           enable-markup?
           use-integer-positions?]}]
  (.setScale (.getData font) scale)
  (set! (.markupEnabled (.getData font)) enable-markup?)
  (.setUseIntegerPositions font use-integer-positions?)
  font)
