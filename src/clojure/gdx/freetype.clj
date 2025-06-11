(ns clojure.gdx.freetype
  (:import (com.badlogic.gdx.graphics Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d BitmapFont)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)))

(defn- create-params [{:keys [size
                              min-filter
                              mag-filter]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) min-filter)
    (set! (.magFilter params) mag-filter)
    params))

(defn generate-font [file-handle {:keys [size
                                         quality-scaling
                                         enable-markup?
                                         use-integer-positions?]}]
  (let [generator (FreeTypeFontGenerator. file-handle)
        ^BitmapFont font (.generateFont generator
                                        (freetype-font-params {:size (* size quality-scaling)
                                                               ; :texture-filter/linear because scaling to world-units
                                                               :min-filter Texture$TextureFilter/Linear
                                                               :mag-filter Texture$TextureFilter/Linear}))]
    (.setScale (.getData font) (/ quality-scaling))
    (set! (.markupEnabled (.getData font)) enable-markup?)
    (.setUseIntegerPositions font use-integer-positions?)
    font))
