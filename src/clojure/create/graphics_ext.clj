(ns clojure.create.graphics-ext
  (:require [clojure.files :as files]
            [clojure.gdx :as gdx]
            [clojure.gdx.freetype :as freetype]
            [clojure.graphics :as graphics]
            [clojure.graphics.color :as color]
            [clojure.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.graphics.texture :as texture]
            [clojure.graphics.tiled-map-renderer :as tiled-map-renderer]
            [clojure.graphics.pixmap :as pixmap]
            [clojure.gdx.shape-drawer :as shape-drawer]
            [clojure.utils :as utils]))

(defn- white-pixel-texture []
  (let [pixmap (doto (gdx/pixmap 1 1 :pixmap.format/RGBA8888)
                 (pixmap/set-color! color/white)
                 (pixmap/draw-pixel! 0 0))
        texture (gdx/texture pixmap)]
    (utils/dispose! pixmap)
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
            :ctx/shape-drawer (shape-drawer/create batch (texture/->sub-region shape-drawer-texture 1 0 1 1))
            :ctx/cursors (utils/mapvals (fn [[file [hotspot-x hotspot-y]]]
                                          (let [pixmap (gdx/pixmap (files/internal files (format cursor-path-format file)))
                                                cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
                                            (utils/dispose! pixmap)
                                            cursor))
                                        cursors)
            :ctx/default-font (truetype-font files default-font)
            :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                               (tiled-map-renderer/create tiled-map world-unit-scale batch)))})))
