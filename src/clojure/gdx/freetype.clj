(ns clojure.gdx.freetype
  (:import (com.badlogic.gdx.graphics Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)))

(defn- k->texture-filter [k]
  (case k
    :texture-filter/linear Texture$TextureFilter/Linear))

(defn- font-params [{:keys [size
                            min-filter
                            mag-filter]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) (k->texture-filter min-filter))
    (set! (.magFilter params) (k->texture-filter mag-filter))
    params))

(defn generate [file-handle params]
  (let [generator (FreeTypeFontGenerator. (:java-object file-handle))
        font (.generateFont generator (font-params params))]
    (.dispose generator)
    font))
