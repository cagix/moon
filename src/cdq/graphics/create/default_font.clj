(ns cdq.graphics.create.default-font
  (:import (com.badlogic.gdx.graphics Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)))

(defn- generate-font [file-handle params]
  (let [{:keys [size
                quality-scaling
                enable-markup?
                use-integer-positions?
                min-filter
                mag-filter]} params]
    (let [generator (FreeTypeFontGenerator. file-handle)
          font (.generateFont generator
                              (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
                                (set! (.size params) (* size quality-scaling))
                                (set! (.minFilter params) Texture$TextureFilter/Linear)
                                (set! (.magFilter params) Texture$TextureFilter/Linear)
                                params))]
      (.setScale (.getData font) (/ quality-scaling))
      (set! (.markupEnabled (.getData font)) enable-markup?)
      (.setUseIntegerPositions font use-integer-positions?)
      font)))

(defn create [graphics default-font]
  (assoc graphics :graphics/default-font
         (generate-font (:file-handle default-font)
                        (:params default-font))))
