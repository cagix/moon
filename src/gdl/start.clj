(ns gdl.start
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.assets.manager :as assets-manager]
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
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
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
            [gdl.assets :as assets]
            [gdl.audio.sound :as sound]
            [gdl.graphics]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.camera]
            [gdl.graphics.texture]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.viewport]
            [gdl.graphics.g2d.bitmap-font :as bitmap-font]
            [gdl.input]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]
            [gdl.utils.disposable :as disposable])
  (:import (com.badlogic.gdx ApplicationListener)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d BitmapFont
                                          TextureRegion)
           (com.badlogic.gdx.utils Disposable)
           (gdl.graphics OrthogonalTiledMapRenderer)))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- set-scale! [^BitmapFont font scale]
  (.setScale (.getData font) (float scale)))

(defn- scale-x [^BitmapFont font]
  (.scaleX (.getData font)))

(extend-type Disposable
  disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(defn- reify-texture-region [^TextureRegion this]
  (reify
    clojure.lang.ILookup
    (valAt [_ k]
      (case k
        :texture-region/dimensions [(.getRegionWidth  this)
                                    (.getRegionHeight this)]
        :texture-region/java-object this))

    gdl.graphics.texture/TextureRegion
    (sub-region [_ x y w h]
      (reify-texture-region (TextureRegion. this
                                            (int x)
                                            (int y)
                                            (int w)
                                            (int h))))))

(defn- reify-texture [^Texture this]
  (reify
    disposable/Disposable
    (dispose! [_]
      (.dispose this))

    gdl.graphics.texture/Texture
    (region [_]
      (reify-texture-region (TextureRegion. this)))
    (region [_ x y w h]
      (reify-texture-region (TextureRegion. this
                                            (int x)
                                            (int y)
                                            (int w)
                                            (int h))))))

(defn- reify-sound [^Sound this]
  (reify sound/Sound
    (play! [_]
      (.play this))))

(defn- k->class ^Class [asset-type-k]
  (case asset-type-k
    :sound Sound
    :texture Texture))

(defmulti ^:private reify-asset class)
(defmethod reify-asset Sound   [this] (reify-sound   this))
(defmethod reify-asset Texture [this] (reify-texture this))

(defn- create-asset-manager [assets]
  (let [this (assets-manager/create (map
                                     (fn [[file asset-type]]
                                       [file (k->class asset-type)])
                                     assets))]
    (reify
      disposable/Disposable
      (dispose! [_]
        (assets-manager/dispose! this))

      clojure.lang.IFn
      (invoke [_ path]
        (reify-asset (assets-manager/safe-get this path)))

      assets/Assets
      (all-of-type [_ asset-type-k]
        (assets-manager/all-of-class this (k->class asset-type-k))))))

(defn- create-graphics [this]
  (reify gdl.graphics/Graphics
    (delta-time [_]
      (graphics/delta-time this))

    (frames-per-second [_]
      (graphics/frames-per-second this))

    (set-cursor! [_ cursor]
      (graphics/set-cursor! this cursor))))

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
      (let [frustum-points (take 4 (map vector3/->clj-vec (.planePoints (:camera/frustum this))))
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

(defn- sprite-batch []
  (let [this (sprite-batch/create)]
    (reify
      clojure.lang.ILookup
      (valAt [_ k]
        (case k
          :sprite-batch/java-object this))

      Disposable
      (dispose [_]
        (.dispose this))

      batch/Batch
      (set-color! [_ color]
        (.setColor this ^Color color))

      (draw! [_ texture-region {:keys [x
                                       y
                                       origin-x
                                       origin-y
                                       width
                                       height
                                       scale-x
                                       scale-y
                                       rotation]}]
        (.draw this
               (:texture-region/java-object texture-region)
               x
               y
               origin-x
               origin-y
               width
               height
               scale-x
               scale-y
               rotation))

      (begin! [_]
        (.begin this))

      (end! [_]
        (.end this))

      (set-projection-matrix! [_ matrix]
        (.setProjectionMatrix this matrix)))))

(defn- white-pixel-texture []
  (let [pixmap (doto (pixmap/create 1 1)
                 (.setColor (color/create :white))
                 (.drawPixel 0 0))
        texture (texture/create pixmap)]
    (.dispose pixmap)
    texture))

(defn- generate-font [file-handle params]
  (let [^BitmapFont font (freetype/generate-font file-handle params)
        {:keys [scale
                enable-markup?
                use-integer-positions?]} params]
    (.setScale (.getData font) (float scale))
    (set! (.markupEnabled (.getData font)) enable-markup?)
    (.setUseIntegerPositions font use-integer-positions?)
    (reify
      Disposable
      (dispose [_]
        (.dispose font))

      bitmap-font/BitmapFont
      (draw! [_ batch {:keys [scale x y text h-align up?]}]
        (let [old-scale (float (scale-x font))
              target-width (float 0)
              wrap? false]
          (set-scale! font (* old-scale (float scale)))
          (.draw font
                 (:sprite-batch/java-object batch)
                 text
                 (float x)
                 (float (+ y (if up? (text-height font text) 0)))
                 target-width
                 (align/->from-k (or h-align :center))
                 wrap?)
          (set-scale! font old-scale))))))

(defn- truetype-font [files {:keys [file size quality-scaling]}]
  (generate-font (files/internal files file)
                 {:size (* size quality-scaling)
                  :scale (/ quality-scaling)
                  :min-filter (texture.filter/->from-keyword :texture-filter/linear) ; because scaling to world-units
                  :mag-filter (texture.filter/->from-keyword :texture-filter/linear)
                  :enable-markup? true
                  :use-integer-positions? false}))  ; false, otherwise scaling to world-units not visible

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

(defn- assets-to-load [files {:keys [folder
                                     asset-type-extensions]}]
  (for [[asset-type extensions] asset-type-extensions
        file (map #(str/replace-first % folder "")
                  (recursively-search (files/internal files folder)
                                      extensions))]
    [file asset-type]))

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

#_(defn- create-audio [sounds-to-load]
  (into {}
        (for [file sounds-to-load]
          (audio/sound audio (files/internal files file)))))

(defn- create-context [{:keys [gdx/files
                               gdx/input
                               gdx/graphics]}
                       {:keys [assets
                               tile-size
                               ui-viewport
                               world-viewport
                               cursor-path-format ; optional
                               cursors ; optional
                               default-font ; optional, could use gdx included (BitmapFont.)
                               ui]}]
  (let [batch (sprite-batch)
        shape-drawer-texture (white-pixel-texture)
        world-unit-scale (float (/ tile-size))
        ui-viewport (create-ui-viewport ui-viewport)
        stage (let [stage (ui/stage (:java-object ui-viewport)
                                    batch)]
                (input/set-processor! input stage)
                (reify-stage stage))]
    (ui/load! ui)
    {:ctx/input (create-input input)
     ;:ctx/audio (create-audio)
     :ctx/assets (create-asset-manager (assets-to-load files assets))
     :ctx/graphics (create-graphics graphics)
     :ctx/world-unit-scale world-unit-scale
     :ctx/ui-viewport ui-viewport
     :ctx/world-viewport (create-world-viewport world-unit-scale world-viewport)
     :ctx/batch batch
     :ctx/unit-scale (atom 1)
     :ctx/shape-drawer-texture shape-drawer-texture
     :ctx/shape-drawer (shape-drawer/create batch (TextureRegion. shape-drawer-texture 1 0 1 1))
     :ctx/cursors (update-vals cursors
                               (fn [[file [hotspot-x hotspot-y]]]
                                 (let [pixmap (pixmap/create (files/internal files (format cursor-path-format file)))
                                       cursor (graphics/cursor graphics pixmap hotspot-x hotspot-y)]
                                   (.dispose pixmap)
                                   cursor)))
     :ctx/default-font (when default-font
                         (truetype-font files default-font))
     :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                        (OrthogonalTiledMapRenderer. (:tiled-map/java-object tiled-map)
                                                                     (float world-unit-scale)
                                                                     (:sprite-batch/java-object batch))))
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
