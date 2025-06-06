(ns gdl.start
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.files :as files]
            [clojure.gdx.files.file-handle :as file-handle]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.texture.filter :as texture.filter]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.input :as input]
            [clojure.gdx.input.buttons :as input.buttons]
            [clojure.gdx.input.keys :as input.keys]
            [clojure.gdx.math.utils :as math-utils]
            [clojure.gdx.math.vector3 :as vector3]
            [clojure.gdx.utils.align :as align]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.gdx.utils.os :as os]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]
            [clojure.java.awt.taskbar :as taskbar]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as lwjgl.system.configuration]
            [clojure.string :as str]
            [gdl.audio]
            [gdl.graphics]
            [gdl.graphics.camera]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.viewport]
            [gdl.graphics.g2d.bitmap-font]
            [gdl.graphics.tiled-map-renderer :as tiled-map-renderer]
            [gdl.input]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]
            [gdl.utils.disposable :as disposable])
  (:import (com.badlogic.gdx ApplicationListener)
           (com.badlogic.gdx.utils Disposable)
           (gdl.graphics OrthogonalTiledMapRenderer)))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (file-handle/list folder)
         result []]
    (cond (nil? file)
          result

          (file-handle/directory? file)
          (recur (concat remaining (file-handle/list file)) result)

          (extensions (file-handle/extension file))
          (recur remaining (conj result (file-handle/path file)))

          :else
          (recur remaining result))))

(extend-type Disposable
  disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(defn- find-assets [files {:keys [folder extensions]}]
  (map #(str/replace-first % folder "")
       (recursively-search (files/internal files folder)
                           extensions)))

(defn- create-audio [audio files sounds-to-load]
  ;(println "create-audio. (count sounds-to-load): " (count sounds-to-load))
  (let [sounds (into {}
                     (for [file sounds-to-load]
                       [file (audio/sound audio (files/internal files file))]))]
    (reify
      disposable/Disposable
      (dispose! [_]
        (do
         ;(println "Disposing sounds ...")
         (run! disposable/dispose! (vals sounds))))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (sound/play! (get sounds path))))))

(defn- generate-font [file-handle params]
  (let [font (freetype/generate-font file-handle params)]
    (bitmap-font/configure! font params) ; DOTO ?
    (reify
      Disposable
      (dispose [_]
        (.dispose font))

      gdl.graphics.g2d.bitmap-font/BitmapFont
      (draw! [_ batch {:keys [scale x y text h-align up?]}]
        (let [old-scale (bitmap-font/scale-x font)]
          (bitmap-font/set-scale! font (* old-scale scale))
          (bitmap-font/draw! {:font font
                              :batch batch
                              :text text
                              :x x
                              :y (+ y (if up? (bitmap-font/text-height font text) 0))
                              :target-width 0
                              :align (align/->from-k (or h-align :center))
                              :wrap? false})
          (bitmap-font/set-scale! font old-scale))))))

(defn- truetype-font [files {:keys [file size quality-scaling]}]
  (generate-font (files/internal files file)
                 {:size (* size quality-scaling)
                  :scale (/ quality-scaling)
                  :min-filter (texture.filter/->from-keyword :texture-filter/linear) ; because scaling to world-units
                  :mag-filter (texture.filter/->from-keyword :texture-filter/linear)
                  :enable-markup? true
                  :use-integer-positions? false}))  ; false, otherwise scaling to world-units not visible

(defrecord Graphics [graphics
                     textures
                     cursors
                     default-font
                     world-unit-scale
                     ui-viewport
                     world-viewport
                     batch
                     unit-scale
                     shape-drawer-texture
                     shape-drawer
                     tiled-map-renderer]
  disposable/Disposable
  (dispose! [_]
    (disposable/dispose! batch)
    (disposable/dispose! shape-drawer-texture)
    (run! disposable/dispose! (vals textures))
    (run! disposable/dispose! (vals cursors))
    (disposable/dispose! default-font))

  gdl.graphics/Graphics
  (delta-time [_]
    (graphics/delta-time graphics))

  (frames-per-second [_]
    (graphics/frames-per-second graphics))

  (set-cursor! [_ cursor-key]
    (assert (contains? cursors cursor-key))
    (graphics/set-cursor! graphics (get cursors cursor-key)))

  (texture [_ path]
    (assert (contains? textures path) (str path))
    (get textures path))

  (draw-on-world-viewport! [_ f]
    (batch/set-color! batch (color/create :white)) ; fix scene2d.ui.tooltip flickering
    (batch/set-projection-matrix! batch (gdl.graphics.camera/combined (:camera world-viewport)))
    (batch/begin! batch)
    (shape-drawer/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (batch/end! batch))

  (draw-tiled-map! [_ tiled-map color-setter]
    (tiled-map-renderer/draw! (tiled-map-renderer tiled-map)
                              tiled-map
                              color-setter
                              (:camera world-viewport)))
  )

(defn- reify-camera [this]
  (reify
    clojure.lang.ILookup
    (valAt [_ k]
      (case k
        :camera/java-object this))

    gdl.graphics.camera/Camera
    (zoom [_]
      (:orthographic-camera/zoom this))

    (position [_]
      (:camera/position this))

    (combined [_]
      (:camera/combined this))

    (frustum [_]
      (let [frustum-points (take 4 (map vector3/->clj-vec (.planePoints  ; refl
                                                           (:camera/frustum this))))
            left-x   (apply min (map first  frustum-points))
            right-x  (apply max (map first  frustum-points))
            bottom-y (apply min (map second frustum-points))
            top-y    (apply max (map second frustum-points))]
        [left-x right-x bottom-y top-y]))

    (set-position! [_ position]
      (camera/set-position! this position))

    (set-zoom! [_ amount]
      (orthographic-camera/set-zoom! this amount))

    (viewport-width [_]
      (:camera/viewport-width this))

    (viewport-height [_]
      (:camera/viewport-height this))

    (reset-zoom! [cam]
      (orthographic-camera/set-zoom! this 1))

    (inc-zoom! [cam by]
      (orthographic-camera/set-zoom! this (max 0.1 (+ (:orthographic-camera/zoom this) by)))) ))

(defn- fit-viewport [width height camera {:keys [center-camera?]}]
  (let [this (fit-viewport/create width height camera)]
    (reify
      gdl.graphics.viewport/Viewport
      (resize! [_ width height]
        (viewport/update! this width height center-camera?))

      ; touch coordinates are y-down, while screen coordinates are y-up
      ; so the clamping of y is reverse, but as black bars are equal it does not matter
      ; TODO clamping only works for gui-viewport ?
      ; TODO ? "Can be negative coordinates, undefined cells."
      (unproject [_ [x y]]
        (let [clamped-x (math-utils/clamp x
                                          (.getLeftGutterWidth this)
                                          (.getRightGutterX    this))
              clamped-y (math-utils/clamp y
                                          (.getTopGutterHeight this)
                                          (.getTopGutterY      this))]
          (viewport/unproject this clamped-x clamped-y)))

      clojure.lang.ILookup
      (valAt [_ key]
        (case key
          :java-object this
          :width  (.getWorldWidth  this)
          :height (.getWorldHeight this)
          :camera (reify-camera (.getCamera this)))))))

(defn- create-ui-viewport [{:keys [width height]}]
  (fit-viewport width
                height
                (orthographic-camera/create)
                {:center-camera? true}))

(defn- create-world-viewport [world-unit-scale {:keys [width height]}]
  (let [world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)]
    (fit-viewport world-width
                  world-height
                  (orthographic-camera/create {:world-width world-width
                                               :world-height world-height
                                               :y-down? false})
                  {:center-camera? false})))

(defn- white-pixel-texture []
  (let [pixmap (doto (pixmap/create 1 1)
                 (.setColor (color/create :white))
                 (.drawPixel 0 0))
        texture (texture/create pixmap)]
    (.dispose pixmap)
    texture))

(defn- create-graphics [gdx-graphics
                        gdx-files
                        {:keys [textures
                                cursors ; optional
                                cursor-path-format ; optional
                                default-font ; optional, could use gdx included (BitmapFont.)
                                tile-size
                                ui-viewport
                                world-viewport]}]
  (let [batch (sprite-batch/create)
        shape-drawer-texture (white-pixel-texture)
        world-unit-scale (float (/ tile-size))
        ui-viewport (create-ui-viewport ui-viewport)
        textures-to-load (find-assets gdx-files textures)
        ;_ (println "load-textures (count textures-to-load): " (count textures-to-load))
        textures (into {} (for [file (find-assets gdx-files textures)]
                            [file (texture/load! file)]))
        cursors (update-vals cursors
                             (fn [[file [hotspot-x hotspot-y]]]
                               (let [pixmap (pixmap/create (files/internal gdx-files (format cursor-path-format file)))
                                     cursor (graphics/cursor gdx-graphics pixmap hotspot-x hotspot-y)]
                                 (.dispose pixmap)
                                 cursor)))
        default-font (when default-font
                       (truetype-font gdx-files default-font))]
    (map->Graphics {:graphics gdx-graphics
                    :textures textures
                    :cursors cursors
                    :default-font default-font
                    :world-unit-scale world-unit-scale
                    :ui-viewport ui-viewport
                    :world-viewport (create-world-viewport world-unit-scale world-viewport)
                    :batch batch
                    :unit-scale (atom 1)
                    :shape-drawer-texture shape-drawer-texture
                    :shape-drawer (shape-drawer/create batch (texture-region/create shape-drawer-texture 1 0 1 1))
                    :tiled-map-renderer (memoize (fn [tiled-map]
                                                   (OrthogonalTiledMapRenderer. (:tiled-map/java-object tiled-map)
                                                                                (float world-unit-scale)
                                                                                batch)))})))

(comment
 (:shape-drawer (:ctx/graphics @cdq.application/state))
 )

(defn- create-input [this]
  (reify gdl.input/Input
    (button-just-pressed? [_ button]
      (input/button-just-pressed? this (input.buttons/->from-k button)))

    (key-pressed? [_ key]
      (input/key-pressed? this (input.keys/->from-k key)))

    (key-just-pressed? [_ key]
      (input/key-just-pressed? this (input.keys/->from-k key)))

    (mouse-position [_]
      [(input/x this)
       (input/y this)])))
(defn- reify-stage [stage]
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

    (add! [_ actor] ; -> re-use gdl.ui/add! ?
      (ui/add! stage actor))

    (clear! [_]
      (ui/clear! stage))

    (hit [_ position]
      (ui/hit stage position))

    (find-actor [_ actor-name]
      (-> stage
          ui/root
          (ui/find-actor actor-name)))))

(defn- create-context [{:keys [gdx/audio
                               gdx/files
                               gdx/input]
                        :as context}
                       {:keys [sounds ui]
                        :as config}]
  (let [graphics (create-graphics (:gdx/graphics context)
                                  (:gdx/files context)
                                  (:graphics config))
        stage (let [stage (ui/stage (:java-object (:ui-viewport graphics))
                                    (:batch graphics))]
                (input/set-processor! input stage)
                (reify-stage stage))]
    (ui/load! ui)
    {:ctx/input (create-input input)
     :ctx/audio (when sounds (create-audio audio files (find-assets files sounds)))
     :ctx/graphics graphics
     :ctx/stage stage}))

(defn- set-mac-os-config! [{:keys [glfw-async?
                                   dock-icon]}]
  (when glfw-async?
    (lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when dock-icon
    (taskbar/set-icon! dock-icon)))

(defn -main [app-edn-path]
  (let [config (-> app-edn-path
                   io/resource
                   slurp
                   edn/read-string)
        req-resolve-call (fn [k & params]
                           (when-let [f (k config)]
                             (apply (requiring-resolve f) params)))]
    (when (= (get os/mapping (shared-library-loader/os)) :os/mac-osx)
      (set-mac-os-config! (:mac-os config)))
    (lwjgl/application! (:clojure.gdx.lwjgl/config config)
                        (proxy [ApplicationListener] []
                          (create  []
                            ((requiring-resolve (:clojure.gdx.lwjgl/create! config))
                             (create-context {:gdx/app      (gdx/app)
                                              :gdx/audio    (gdx/audio)
                                              :gdx/files    (gdx/files)
                                              :gdx/graphics (gdx/graphics)
                                              :gdx/input    (gdx/input)}
                                             (:gdl.application/context config))
                             config))
                          (dispose []             (req-resolve-call :clojure.gdx.lwjgl/dispose!))
                          (render  []             (req-resolve-call :clojure.gdx.lwjgl/render!))
                          (resize  [width height] (req-resolve-call :clojure.gdx.lwjgl/resize! width height))
                          (pause   []             (req-resolve-call :clojure.gdx.lwjgl/pause!))
                          (resume  []             (req-resolve-call :clojure.gdx.lwjgl/resume!))))))
