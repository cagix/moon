(ns clojure.gdx.graphics.g2d.freetype
  "https://javadoc.io/doc/com.badlogicgames.gdx/gdx-freetype/latest/com/badlogic/gdx/graphics/g2d/freetype/FreeTypeFontGenerator.html

  https://javadoc.io/static/com.badlogicgames.gdx/gdx-freetype/1.13.0/com/badlogic/gdx/graphics/g2d/freetype/FreeTypeFontGenerator.FreeTypeFontParameter.html"
  (:import (com.badlogic.gdx.graphics Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)))

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn generate-font [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. file)
        font (.generateFont generator (ttf-params size quality-scaling))]
    (.dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))
