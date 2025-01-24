(ns cdq.create.default-font
  (:require [clojure.gdx.files :as files]
            [clojure.gdx.graphics.g2d.freetype :as freetype]))

(def config
  {:file "fonts/exocet/films.EXL_____.ttf"
   :size 16
   :quality-scaling 2})

(defn create [_context]
  (let [{:keys [file size quality-scaling]} config
        font (freetype/generate-font (files/internal file)
                                     {:size (* size quality-scaling)})]
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))
