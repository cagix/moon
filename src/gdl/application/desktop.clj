(ns gdl.application.desktop
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.freetype :as freetype]
            [clojure.gdx.shape-drawer :as sd]
            [clojure.gdx.tiled :as tiled]
            [clojure.string :as str]
            [gdl.audio :as audio]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera]
            [gdl.graphics.texture :as texture]
            [gdl.graphics.g2d.texture-region :as texture-region]
            [gdl.graphics.viewport]
            [gdl.math.utils :as math-utils]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]
            [gdl.utils.disposable])
  (:import (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)
           (gdl.ui CtxStage)))

(extend-type gdl.ui.CtxStage
  stage/Stage
  (render! [stage ctx]
    (reset! (.ctx stage) ctx)
    (.act stage)
    ; We cannot pass this
    ; because input events are handled outside ui/act! and in the Lwjgl3Input system
    #_@(.ctx (-k ctx))
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; FIXME so it has outdated context.
    #_(reset! (.ctx (-k ctx)) nil)
    (reset! (.ctx stage) ctx)
    (.draw stage)
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; so it has outdated context
    ; => maybe context should be an immutable data structure with mutable fields?
    #_(reset! (.ctx (-k ctx)) nil)
    ctx)

  (add! [stage actor] ; -> re-use gdl.ui/add! ?
    (ui/add! stage actor))

  (clear! [stage]
    (.clear stage))

  (hit [stage position]
    (ui/hit stage position))

  (find-actor [stage actor-name]
    (-> stage
        .getRoot
        (ui/find-actor actor-name))))

(defn- texture-region-drawing-dimensions
  [{:keys [unit-scale
           world-unit-scale]}
   texture-region]
  (let [dimensions (texture-region/dimensions texture-region)]
    (if (= @unit-scale 1)
      dimensions
      (mapv (comp float (partial * world-unit-scale))
            dimensions))))

(defmulti ^:private draw!
  (fn [[k] _graphics]
    k))

(defmethod draw! :draw/texture-region [[_ texture-region [x y]]
                                       {:keys [batch]}]
  (gdx/draw-texture-region! batch
                            texture-region
                            [x y]
                            (texture-region/dimensions texture-region)
                            0  ;rotation
                            ))

(defmethod draw! :draw/image [[_ image position]
                              {:keys [batch]
                               :as graphics}]
  (let [texture-region (gdl.graphics/image->texture-region graphics image)]
    (gdx/draw-texture-region! batch
                              texture-region
                              position
                              (texture-region-drawing-dimensions graphics texture-region)
                              0 ; rotation
                              )))

(defmethod draw! :draw/rotated-centered [[_ image rotation [x y]]
                                         {:keys [batch]
                                          :as graphics}]
  (let [texture-region (gdl.graphics/image->texture-region graphics image)
        [w h] (texture-region-drawing-dimensions graphics texture-region)]
    (gdx/draw-texture-region! batch
                              texture-region
                              [(- (float x) (/ (float w) 2))
                               (- (float y) (/ (float h) 2))]
                              [w h]
                              rotation
                              )))

(defmethod draw! :draw/centered [[_ image position] this]
  (draw! [:draw/rotated-centered image 0 position] this))

(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [batch
                                     unit-scale
                                     default-font]}]
  (gdx/draw-text! (or font default-font)
                  batch
                  {:scale (* (float @unit-scale)
                             (float (or scale 1)))
                   :text text
                   :x x
                   :y y
                   :up? up?
                   :h-align h-align
                   :target-width 0
                   :wrap? false}))

(defmethod draw! :draw/ellipse [[_ position radius-x radius-y color]
                                {:keys [shape-drawer]}]
  (sd/ellipse! shape-drawer position radius-x radius-y color))

(defmethod draw! :draw/filled-ellipse [[_ position radius-x radius-y color]
                                       {:keys [shape-drawer]}]
  (sd/filled-ellipse! shape-drawer position radius-x radius-y color))

(defmethod draw! :draw/circle [[_ position radius color]
                               {:keys [shape-drawer]}]
  (sd/circle! shape-drawer position radius color))

(defmethod draw! :draw/filled-circle [[_ position radius color]
                                      {:keys [shape-drawer]}]
  (sd/filled-circle! shape-drawer position radius color))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [shape-drawer]}]
  (sd/rectangle! shape-drawer x y w h color))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [shape-drawer]}]
  (sd/filled-rectangle! shape-drawer x y w h color))

(defmethod draw! :draw/arc [[_ center-position radius start-angle degree color]
                            {:keys [shape-drawer]}]
  (sd/arc! shape-drawer center-position radius start-angle degree color))

(defmethod draw! :draw/sector [[_ center-position radius start-angle degree color]
                               {:keys [shape-drawer]}]
  (sd/sector! shape-drawer center-position radius start-angle degree color))

(defmethod draw! :draw/line [[_ start end color]
                             {:keys [shape-drawer]}]
  (sd/line! shape-drawer start end color))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color]
                             {:keys [shape-drawer]}]
  (sd/grid! shape-drawer leftx bottomy gridw gridh cellw cellh color))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [shape-drawer] :as this}]
  (sd/with-line-width shape-drawer width
    (fn []
      (gdl.graphics/handle-draws! this draws))))

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
  gdl.utils.disposable/Disposable
  (dispose! [_]
    (gdl.utils.disposable/dispose! batch)
    (gdl.utils.disposable/dispose! shape-drawer-texture)
    (run! gdl.utils.disposable/dispose! (vals textures))
    (run! gdl.utils.disposable/dispose! (vals cursors))
    (when default-font
      (gdl.utils.disposable/dispose! default-font)))

  gdl.graphics/Graphics
  (clear-screen! [_ color]
    (gdx/clear-screen! color))

  (resize-viewports! [_ width height]
    (.update ui-viewport    width height true)
    (.update world-viewport width height false))

  (delta-time [_]
    (.getDeltaTime graphics))

  (frames-per-second [_]
    (.getFramesPerSecond graphics))

  (set-cursor! [_ cursor-key]
    (assert (contains? cursors cursor-key))
    (.setCursor graphics (get cursors cursor-key)))

  ; TODO probably not needed I only work with texture-regions
  (texture [_ path]
    (assert (contains? textures path)
            (str "Cannot find texture with path: " (pr-str path)))
    (get textures path))

  (draw-on-world-viewport! [_ f]
    (gdx/draw-on-viewport! batch
                           world-viewport
                           (fn []
                             (sd/with-line-width shape-drawer world-unit-scale
                                                 (fn []
                                                   (reset! unit-scale world-unit-scale)
                                                   (f)
                                                   (reset! unit-scale 1))))))

  (draw-tiled-map! [_ tiled-map color-setter]
    (let [^OrthogonalTiledMapRenderer renderer (tiled-map-renderer tiled-map)
          camera (:camera world-viewport)]
      (.setColorSetter renderer (reify ColorSetter
                                  (apply [_ color x y]
                                    (gdx/->float-bits (color-setter color x y)))))
      (.setView renderer camera)
      ; there is also:
      ; OrthogonalTiledMapRenderer/.renderTileLayer (TiledMapTileLayer layer)
      ; but right order / visible only ?
      (->> tiled-map
           tiled/layers
           (filter tiled/visible?)
           (map (partial tiled/layer-index tiled-map))
           int-array
           (.render renderer))))

  ; FIXME this can be memoized
  ; also good for tiled-map tiles they have to be memoized too
  (image->texture-region [graphics {:keys [image/file
                                           image/bounds]}]
    (assert file)
    (let [texture (gdl.graphics/texture graphics file)]
      (if bounds
        (apply texture/region texture bounds)
        (texture/region texture))))

  (handle-draws! [this draws]
    (doseq [component draws
            :when component]
      (draw! component this))))

(extend-type com.badlogic.gdx.graphics.OrthographicCamera
  gdl.graphics.camera/Camera
  (zoom [this]
    (.zoom this))

  (position [this]
    [(.x (.position this))
     (.y (.position this))])

  (frustum [this]
    (let [frustum-points (take 4 (map gdx/vector3->clj-vec (.planePoints (.frustum this))))
          left-x   (apply min (map first  frustum-points))
          right-x  (apply max (map first  frustum-points))
          bottom-y (apply min (map second frustum-points))
          top-y    (apply max (map second frustum-points))]
      [left-x right-x bottom-y top-y]))

  (set-position! [this [x y]]
    (set! (.x (.position this)) (float x))
    (set! (.y (.position this)) (float y))
    (.update this))

  (set-zoom! [this amount]
    (set! (.zoom this) amount)
    (.update this))

  (viewport-width [this]
    (.viewportWidth this))

  (viewport-height [this]
    (.viewportHeight this))

  (reset-zoom! [cam]
    (gdl.graphics.camera/set-zoom! cam 1))

  (inc-zoom! [cam by]
    (gdl.graphics.camera/set-zoom! cam (max 0.1 (+ (.zoom cam) by)))) )

(extend-type com.badlogic.gdx.utils.viewport.FitViewport
  gdl.graphics.viewport/Viewport
  (unproject [this position]
    (gdx/unproject this position)))

(defn- create-graphics
  [{:keys [textures
           colors ; optional
           cursors ; optional
           cursor-path-format ; optional
           default-font ; optional, could use gdx included (BitmapFont.)
           tile-size
           ui-viewport
           world-viewport]}]
  (gdx/def-colors colors)
  (let [batch (gdx/sprite-batch)
        shape-drawer-texture (gdx/white-pixel-texture)
        world-unit-scale (float (/ tile-size))
        ui-viewport (gdx/fit-viewport (:width  ui-viewport)
                                      (:height ui-viewport)
                                      (gdx/orthographic-camera))
        textures-to-load (gdx/find-assets textures)
        ;(println "load-textures (count textures): " (count textures))
        textures (into {} (for [file textures-to-load]
                            [file (gdx/load-texture file)]))
        cursors (update-vals cursors
                             (fn [[file hotspot]]
                               (gdx/create-cursor (format cursor-path-format file) hotspot)))]
    (map->Graphics {:graphics (gdx/graphics)
                    :textures textures
                    :cursors cursors
                    :default-font (when default-font
                                    (freetype/generate-font (gdx/internal (:file default-font))
                                                            (:params default-font)))
                    :world-unit-scale world-unit-scale
                    :ui-viewport ui-viewport
                    :world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                          world-height (* (:height world-viewport) world-unit-scale)]
                                      (gdx/fit-viewport world-width
                                                        world-height
                                                        (gdx/orthographic-camera :y-down? false
                                                                                 :world-width world-width
                                                                                 :world-height world-height)))
                    :batch batch
                    :unit-scale (atom 1)
                    :shape-drawer-texture shape-drawer-texture
                    :shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))
                    :tiled-map-renderer (memoize (fn [tiled-map]
                                                   (OrthogonalTiledMapRenderer. (:tiled-map/java-object tiled-map)
                                                                                (float world-unit-scale)
                                                                                batch)))})))

(extend-type com.badlogic.gdx.graphics.g2d.TextureRegion
  gdl.graphics.g2d.texture-region/TextureRegion
  (dimensions [texture-region]
    [(.getRegionWidth  texture-region)
     (.getRegionHeight texture-region)])
  (region [texture-region x y w h]
    (gdx/texture-region x y w h)))

(extend-type com.badlogic.gdx.graphics.Texture
  gdl.graphics.texture/Texture
  (region
    ([texture]
     (gdx/texture-region texture))
    ([texture x y w h]
     (com.badlogic.gdx.graphics.g2d.TextureRegion. texture
                                                   (int x)
                                                   (int y)
                                                   (int w)
                                                   (int h)))))

(extend-type com.badlogic.gdx.scenes.scene2d.Group
  gdl.ui/PGroup
  (find-actor [group name]
    (.findActor group name))
  (clear-children! [group]
    (.clearChildren group))
  (children [group]
    (.getChildren group)))

; => these functions here are _private_
; they actually belong in 'clojure.gdx.scenes.scene2d.actor' as public functions API
; then we extend them here
; this can be also automated...
(extend-type com.badlogic.gdx.scenes.scene2d.Actor
  gdl.ui/PActor
  (get-x [actor]
    (.getX actor))

  (get-y [actor]
    (.getY actor))

  (get-name [actor]
    (.getName actor))

  (user-object [actor]
    (.getUserObject actor))

  (set-user-object! [actor object]
    (.setUserObject actor object))

  (visible? [actor]
    (.isVisible actor))

  (set-visible! [actor visible?]
    (.setVisible actor visible?))

  (set-touchable! [actor touchable]
    (.setTouchable actor (case touchable
                           :disabled com.badlogic.gdx.scenes.scene2d.Touchable/disabled)))

  (remove! [actor]
    (.remove actor))

  (parent [actor]
    (.getParent actor)))

(extend-type com.badlogic.gdx.utils.Disposable
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(defn- create-audio [{:keys [sounds]}]
  (let [sounds (into {}
                     (for [path (gdx/find-assets sounds)]
                       [path (gdx/load-sound path)]))]
    (reify
      gdl.utils.disposable/Disposable
      (dispose! [_]
        (run! gdl.utils.disposable/dispose! (vals sounds)))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (gdx/play! (get sounds path))))))

(extend-type com.badlogic.gdx.Input
  gdl.input/Input
  (button-just-pressed? [this button]
    (.isButtonJustPressed this (gdx/k->Input$Buttons button)))

  (key-pressed? [this key]
    (.isKeyPressed this (gdx/k->Input$Keys key)))

  (key-just-pressed? [this key]
    (.isKeyJustPressed this (gdx/k->Input$Keys key)))

  (mouse-position [this]
    [(.getX this)
     (.getY this)]))

(defn- load-vis-ui! [{:keys [skin-scale]}]
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case skin-scale
                :x1 VisUI$SkinScale/X1
                :x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))

(defn create-context [graphics-config
                      user-interface
                      audio]
  (load-vis-ui! user-interface)
  (let [graphics (create-graphics graphics-config)]
    {:ctx/input (gdx/input)
     :ctx/graphics graphics
     :ctx/stage (let [stage (proxy [CtxStage clojure.lang.ILookup] [(:ui-viewport graphics)
                                                                    (:batch graphics)
                                                                    (atom nil)]
                              (valAt [id]
                                (ui/find-actor-with-id (CtxStage/.getRoot this) id)))]
                  (gdx/set-input-processor! stage)
                  stage)
     :ctx/audio (when audio
                  (create-audio audio))}))
