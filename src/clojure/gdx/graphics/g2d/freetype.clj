(ns clojure.gdx.graphics.g2d.freetype
  "https://javadoc.io/doc/com.badlogicgames.gdx/gdx-freetype/latest/com/badlogic/gdx/graphics/g2d/freetype/FreeTypeFontGenerator.html

  https://javadoc.io/static/com.badlogicgames.gdx/gdx-freetype/1.13.0/com/badlogic/gdx/graphics/g2d/freetype/FreeTypeFontGenerator.FreeTypeFontParameter.html"
  (:require [clojure.utils.disposable :refer [dispose]])
  (:import (com.badlogic.gdx.graphics Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)))
; com.badlogic.gdx.graphics.Color
; borderColor
; Border color; only used if borderWidth > 0

; float
; borderGamma
; Values < 1 increase the border size.

; boolean
; borderStraight
; true for straight (mitered), false for rounded borders

; float
; borderWidth
; Border width in pixels, 0 to disable

; String
; characters
; The characters the font should contain.

; com.badlogic.gdx.graphics.Color
; color
; Foreground color (required for non-black borders)

; boolean
; flip
; Whether to flip the font vertically

; float
; gamma
; Glyph gamma.

; boolean
; genMipMaps
; Whether to generate mip maps for the resulting texture

; FreeTypeFontGenerator.Hinting
; hinting
; Strength of hinting

; boolean
; incremental
; When true, glyphs are rendered on the fly to the font's glyph page textures as they are needed.

; boolean
; kerning
; Whether the font should include kerning

; com.badlogic.gdx.graphics.Texture.TextureFilter
; magFilter
; Magnification filter

; com.badlogic.gdx.graphics.Texture.TextureFilter
; minFilter
; Minification filter

; boolean
; mono
; If true, font smoothing is disabled.

; com.badlogic.gdx.graphics.g2d.PixmapPacker
; packer
; The optional PixmapPacker to use for packing multiple fonts into a single texture.

; int
; padBottom
; Pixels to add to the glyph in the texture.

; int
; padLeft
; Pixels to add to the glyph in the texture.

; int
; padRight
; Pixels to add to the glyph in the texture.

; int
; padTop
; Pixels to add to the glyph in the texture.

; int
; renderCount
; Number of times to render the glyph.

; com.badlogic.gdx.graphics.Color
; shadowColor
; Shadow color; only used if shadowOffset > 0.

; int
; shadowOffsetX
; Offset of text shadow on X axis in pixels, 0 to disable

; int
; shadowOffsetY
; Offset of text shadow on Y axis in pixels, 0 to disable

; int
; size
; The size in pixels

; int
; spaceX
; Pixels to add to glyph spacing when text is rendered.

; int
; spaceY
; Pixels to add to glyph spacing when text is rendered.

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
    (dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))
