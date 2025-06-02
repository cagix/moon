(ns clojure.create.graphics-ext
  (:require [clojure.files :as files]
            [clojure.gdx :as gdx]
            [clojure.gdx.freetype :as freetype]
            [clojure.graphics :as graphics]
            [clojure.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.graphics.texture :as texture]
            [clojure.graphics.tiled-map-renderer :as tiled-map-renderer]
            [clojure.graphics.shape-drawer :as sd]
            [clojure.utils :as utils])
  (:import (com.badlogic.gdx.graphics Color
                                      Texture
                                      Pixmap
                                      Pixmap$Format)))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn- truetype-font [files {:keys [file size quality-scaling]}]
  (let [font (freetype/generate (files/internal files file)
                                {:size (* size quality-scaling)
                                 :min-filter :texture-filter/linear ; because scaling to world-units
                                 :mag-filter :texture-filter/linear})]
    (bitmap-font/configure! font {:scale (/ quality-scaling)
                                  :enable-markup? true
                                  :use-integer-positions? false}))) ; false, otherwise scaling to world-units not visible

(defn do! [{:keys [ctx/config
                   ctx/files
                   ctx/graphics
                   ctx/world-unit-scale]
            :as ctx}]
  (merge ctx
         (let [{:keys [cursor-path-format
                       cursors
                       default-font]} config
               batch (gdx/sprite-batch)
               shape-drawer-texture (white-pixel-texture)]
           {:ctx/batch batch
            :ctx/unit-scale (atom 1)
            :ctx/shape-drawer-texture shape-drawer-texture
            :ctx/shape-drawer (sd/create batch (texture/->sub-region shape-drawer-texture 1 0 1 1))
            :ctx/cursors (utils/mapvals (fn [[file [hotspot-x hotspot-y]]]
                                          (let [pixmap (gdx/pixmap (files/internal files (format cursor-path-format file)))
                                                cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
                                            (utils/dispose! pixmap)
                                            cursor))
                                        cursors)
            :ctx/default-font (truetype-font files default-font)
            :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                               (tiled-map-renderer/create tiled-map world-unit-scale batch)))})))
