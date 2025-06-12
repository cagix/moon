(ns clojure.gdx.freetype
  (:require [clojure.gdx :as gdx])
  (:import (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)))
(defn- create-font-params [{:keys [size
                                   min-filter
                                   mag-filter]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    (set! (.minFilter params) min-filter)
    (set! (.magFilter params) mag-filter)
    params))

(defn generate-font [file-handle {:keys [size
                                         quality-scaling
                                         enable-markup?
                                         use-integer-positions?]}]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator
                            (create-font-params {:size (* size quality-scaling)
                                                 ; :texture-filter/linear because scaling to world-units
                                                 :min-filter (gdx/k->TextureFilter :linear)
                                                 :mag-filter (gdx/k->TextureFilter :linear)}))]
    (gdx/configure-bitmap-font! font {:scale (/ quality-scaling)
                                      :enable-markup? enable-markup?
                                      :use-integer-positions? use-integer-positions?})))
