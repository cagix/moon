(ns cdq.create.gdx
  (:require [gdl.ui.stage :as stage]
            [clojure.files :as files]
            [clojure.files.file-handle :as fh]
            [clojure.gdx :as gdx]
            [clojure.gdx.freetype :as freetype]
            [clojure.gdx.shape-drawer :as shape-drawer]
            [clojure.gdx.ui :as ui]
            [clojure.graphics :as graphics]
            [clojure.graphics.texture :as texture]
            [clojure.graphics.pixmap :as pixmap]
            [clojure.input :as input]
            [clojure.string :as str]
            [clojure.utils.disposable :as disp])
  (:import (cdq.graphics OrthogonalTiledMapRenderer)))

(defn- white-pixel-texture []
  (let [pixmap (doto (gdx/pixmap 1 1 :pixmap.format/RGBA8888)
                 (pixmap/set-color! (gdx/->color :white))
                 (pixmap/draw-pixel! 0 0))
        texture (pixmap/texture pixmap)]
    (disp/dispose! pixmap)
    texture))

(defn- truetype-font [files {:keys [file size quality-scaling]}]
  (freetype/generate (files/internal files file)
                     {:size (* size quality-scaling)
                      :scale (/ quality-scaling)
                      :min-filter :texture-filter/linear ; because scaling to world-units
                      :mag-filter :texture-filter/linear
                      :enable-markup? true
                      :use-integer-positions? false})) ; false, otherwise scaling to world-units not visible

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (fh/list folder)
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(defn- assets-to-load [files
                       {:keys [folder
                               asset-type-extensions]}]
  (for [[asset-type extensions] asset-type-extensions
        file (map #(str/replace-first % folder "")
                  (recursively-search (files/internal files folder)
                                      extensions))]
    [file asset-type]))

(defn do! [ctx {:keys [assets
                       tile-size
                       ui-viewport
                       world-viewport
                       cursor-path-format ; optional
                       cursors ; optional
                       default-font ; optional, could use gdx included (BitmapFont.)
                       ui
                       ]}]
  (ui/load! ui)
  (let [files (gdx/files)
        graphics (gdx/graphics)
        input (gdx/input)
        batch (gdx/sprite-batch)
        shape-drawer-texture (white-pixel-texture)
        world-unit-scale (float (/ tile-size))
        ui-viewport (gdx/ui-viewport ui-viewport)]
    (assoc ctx
           :ctx/input input
           :ctx/graphics graphics
           :ctx/assets (gdx/asset-manager (assets-to-load files assets))
           :ctx/world-unit-scale world-unit-scale
           :ctx/ui-viewport ui-viewport
           :ctx/world-viewport (gdx/world-viewport world-unit-scale world-viewport)
           :ctx/batch batch
           :ctx/unit-scale (atom 1)
           :ctx/shape-drawer-texture shape-drawer-texture
           :ctx/shape-drawer (shape-drawer/create batch (texture/region shape-drawer-texture 1 0 1 1))
           :ctx/cursors (update-vals cursors
                                     (fn [[file [hotspot-x hotspot-y]]]
                                       (let [pixmap (gdx/pixmap (files/internal files (format cursor-path-format file)))
                                             cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
                                         (disp/dispose! pixmap)
                                         cursor)))
           :ctx/default-font (when default-font (truetype-font files default-font))
           :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                              (OrthogonalTiledMapRenderer. (:tiled-map/java-object tiled-map)
                                                                           (float world-unit-scale)
                                                                           (:sprite-batch/java-object batch))))
           :ctx/stage (let [stage (ui/stage (:java-object ui-viewport)
                                            batch)]
                        (input/set-processor! input stage)
                        (reify
                          ; TODO is disposable but not sure if needed as we handle batch ourself.
                          clojure.lang.ILookup
                          (valAt [_ key]
                            (key stage))

                          stage/Stage
                          (render! [_ ctx]
                            (ui/act! stage ctx)
                            (ui/draw! stage ctx)
                            ctx)

                          (add! [_ actor] ; -> re-use clojure.gdx.ui/add! ?
                            (ui/add! stage actor))

                          (clear! [_]
                            (ui/clear! stage))

                          (hit [_ position]
                            (ui/hit stage position))

                          (find-actor [_ actor-name]
                            (-> stage
                                ui/root
                                (ui/find-actor actor-name))))))))
