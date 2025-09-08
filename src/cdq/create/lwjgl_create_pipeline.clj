(ns cdq.create.lwjgl-create-pipeline
  (:require [cdq.files]
            [clojure.earlygrey.shape-drawer :as sd]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.tiled-map-renderer :as tm-renderer]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d :as scene2d]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.vis-ui :as vis-ui]
            [clojure.java.io :as io]))

(def font-file "exocet/films.EXL_____.ttf")
(def font-params {:size 16
             :quality-scaling 2
             :enable-markup? true
             ; false, otherwise scaling to world-units not visible
             :use-integer-positions? false})

(def ui-viewport-width 1440)
(def ui-viewport-height 900)

(def world-viewport-width 1440)
(def world-viewport-height 900)

(def sounds "sounds.edn")
(def sound-path-format "sounds/%s.wav")

(def path-format "cursors/%s.png")

(def cursor-data
  {:cursors/bag                   ["bag001"       [0   0]]
   :cursors/black-x               ["black_x"      [0   0]]
   :cursors/default               ["default"      [0   0]]
   :cursors/denied                ["denied"       [16 16]]
   :cursors/hand-before-grab      ["hand004"      [4  16]]
   :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
   :cursors/hand-grab             ["hand003"      [4  16]]
   :cursors/move-window           ["move002"      [16 16]]
   :cursors/no-skill-selected     ["denied003"    [0   0]]
   :cursors/over-button           ["hand002"      [0   0]]
   :cursors/sandclock             ["sandclock"    [16 16]]
   :cursors/skill-not-usable      ["x007"         [0   0]]
   :cursors/use-skill             ["pointer004"   [0   0]]
   :cursors/walking               ["walking"      [16 16]]})

(defn do!
  [{:keys [ctx/reset-game-state-fn
           ctx/world-unit-scale]
    :as ctx}]
  (vis-ui/load! {:skin-scale :x1})
  ((requiring-resolve reset-game-state-fn)
   (let [batch (sprite-batch/create)
         ui-viewport (viewport/fit ui-viewport-width ui-viewport-height (camera/orthographic))
         input (gdx/input)
         stage (scene2d/stage ui-viewport batch)
         shape-drawer-texture (let [pixmap (doto (pixmap/create)
                                             (pixmap/set-color! color/white)
                                             (pixmap/draw-pixel! 0 0))
                                    texture (texture/create pixmap)]
                                (pixmap/dispose! pixmap)
                                texture)]
     (input/set-processor! input stage)
     (merge ctx
            {:ctx/batch batch
             :ctx/ui-viewport ui-viewport
             :ctx/input input
             :ctx/stage stage
             :ctx/tiled-map-renderer (tm-renderer/create world-unit-scale batch)
             :ctx/graphics (gdx/graphics)
             :ctx/textures (into {} (for [[path file-handle] (cdq.files/search (gdx/files)
                                                                               {:folder "resources/"
                                                                                :extensions #{"png" "bmp"}})]
                                      [path (texture/from-file file-handle)]))
             :ctx/audio (into {}
                              (for [sound-name (->> sounds io/resource slurp edn/read-string)
                                    :let [path (format sound-path-format sound-name)]]
                                [sound-name
                                 (audio/sound (gdx/audio) (files/internal (gdx/files) path))]))
             :ctx/cursors (update-vals cursor-data
                                       (fn [[file [hotspot-x hotspot-y]]]
                                         (let [pixmap (pixmap/create (files/internal (gdx/files) (format path-format file)))
                                               cursor (graphics/cursor (gdx/graphics) pixmap hotspot-x hotspot-y)]
                                           (.dispose pixmap)
                                           cursor)))
             :ctx/world-viewport (let [world-width  (* world-viewport-width world-unit-scale)
                                       world-height (* world-viewport-height world-unit-scale)]
                                   (viewport/fit world-width
                                                 world-height
                                                 (camera/orthographic :y-down? false
                                                                      :world-width world-width
                                                                      :world-height world-height)))
             :ctx/default-font (freetype/generate-font (files/internal (gdx/files) font-file)
                                                       font-params)
             :ctx/shape-drawer-texture shape-drawer-texture
             :ctx/shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))}))))
