(ns cdq.create.default-font
  (:require [clojure.gdx.files :as files]
            [clojure.gdx.graphics.g2d.freetype :as freetype])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)))

(defn create [{:keys [file size quality-scaling]}]
  (let [^BitmapFont font (freetype/generate-font (files/internal file)
                                                 {:size (* size quality-scaling)})]
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))
