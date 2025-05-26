(ns gdl.application
  (:require [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.math.math-utils :as math-utils]
            [clojure.space.earlygrey.shape-drawer :as sd]
            [gdl.assets :as assets]
            [gdl.c :as c]
            [gdl.graphics :as graphics]
            [gdl.tiled :as tiled] ; only renderer, internal
            [gdl.ui :as ui] ; partly internal ? ? are the widgets part of ctx ???
            [gdl.utils :as utils]  ; dispose, safe-get, mapvals
            [qrecord.core :as q])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Gdx
                             Input$Keys
                             Input$Buttons)
           (com.badlogic.gdx.graphics Color
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Disposable
                                   ScreenUtils)
           (com.badlogic.gdx.utils.viewport FitViewport)))

;; Publics

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defprotocol Viewport
  (update-vp! [_])
  (mouse-position [_]))

(defn- fit-viewport [width height camera {:keys [center-camera?]}]
  (let [this (FitViewport. width height camera)]
    (reify
      Viewport
      (update-vp! [_]
        (.update this
                 (.getWidth  Gdx/graphics)
                 (.getHeight Gdx/graphics)
                 center-camera?))

      ; touch coordinates are y-down, while screen coordinates are y-up
      ; so the clamping of y is reverse, but as black bars are equal it does not matter
      ; TODO clamping only works for gui-viewport ?
      ; TODO ? "Can be negative coordinates, undefined cells."
      (mouse-position [_]
        (let [mouse-x (math-utils/clamp (.getX Gdx/input)
                                        (.getLeftGutterWidth this)
                                        (.getRightGutterX    this))
              mouse-y (math-utils/clamp (.getY Gdx/input)
                                        (.getTopGutterHeight this)
                                        (.getTopGutterY      this))]
          (let [v2 (.unproject this (Vector2. mouse-x mouse-y))]
            [(.x v2) (.y v2)])))

      ILookup
      (valAt [_ key]
        (case key
          :java-object this
          :width  (.getWorldWidth  this)
          :height (.getWorldHeight this)
          :camera (.getCamera      this))))))

(defn- create-ui-viewport [{:keys [width height]}]
  (fit-viewport width
                height
                (OrthographicCamera.)
                {:center-camera? true}))

(defn- create-world-viewport [world-unit-scale {:keys [width height]}]
  (let [camera (OrthographicCamera.)
        world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width
                  world-height
                  camera
                  {:center-camera? false})))

(defn- truetype-font [{:keys [file size quality-scaling]}]
  (let [font (freetype/generate (.internal Gdx/files file)
                                {:size (* size quality-scaling)})]
    (bitmap-font/configure! font {:scale (/ quality-scaling)
                                  :enable-markup? true
                                  :use-integer-positions? true}) ; otherwise scaling to world-units not visible
    ))

(defn- create-cursor [path hotspot-x hotspot-y]
  (let [pixmap (Pixmap. (.internal Gdx/files path))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defprotocol Batch
  (draw-on-viewport! [_ viewport draw-fn])
  (draw-texture-region! [_ texture-region [x y] [w h] rotation color]))

(defn- sprite-batch []
  (let [this (SpriteBatch.)]
    (reify
      Batch
      (draw-on-viewport! [_ viewport draw-fn]
        (.setColor this Color/WHITE) ; fix scene2d.ui.tooltip flickering
        (.setProjectionMatrix this (camera/combined (:camera viewport)))
        (.begin this)
        (draw-fn)
        (.end this))

      (draw-texture-region! [_ texture-region [x y] [w h] rotation color]
        (if color (.setColor this color))
        (.draw this
               texture-region
               x
               y
               (/ (float w) 2) ; rotation origin
               (/ (float h) 2)
               w
               h
               1 ; scale-x
               1 ; scale-y
               rotation)
        (if color (.setColor this Color/WHITE)))

      Disposable
      (dispose [_]
        (.dispose this))

      ILookup
      (valAt [_ key]
        (case key
          :java-object this)))))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn- k->code [key]
  (case key
    :minus  Input$Keys/MINUS
    :equals Input$Keys/EQUALS
    :space  Input$Keys/SPACE
    :p      Input$Keys/P
    :enter  Input$Keys/ENTER
    :escape Input$Keys/ESCAPE
    :i      Input$Keys/I
    :e      Input$Keys/E
    :d      Input$Keys/D
    :a      Input$Keys/A
    :w      Input$Keys/W
    :s      Input$Keys/S
    ))

(defn- button->code [button]
  (case button
    :left Input$Buttons/LEFT
    ))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} scale world-unit-scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (graphics/dimensions texture-region)
                                             scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- sprite* [texture-region world-unit-scale]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1 world-unit-scale) ; = scale 1
      map->Sprite))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defmulti ^:private draw! (fn [[k] _ctx]
                            k))

(defmethod draw! :draw/image [[_ {:keys [texture-region color] :as image} position]
                              {:keys [ctx/batch
                                      ctx/unit-scale]}]
  (draw-texture-region! batch
                        texture-region
                        position
                        (unit-dimensions image unit-scale)
                        0 ; rotation
                        color))

(defmethod draw! :draw/rotated-centered [[_ {:keys [texture-region color] :as image} rotation [x y]]
                                         {:keys [ctx/batch
                                                 ctx/unit-scale]}]
  (let [[w h] (unit-dimensions image unit-scale)]
    (draw-texture-region! batch
                          texture-region
                          [(- (float x) (/ (float w) 2))
                           (- (float y) (/ (float h) 2))]
                          [w h]
                          rotation
                          color)))

(defmethod draw! :draw/centered [[_ image position] ctx]
  (draw! [:draw/rotated-centered image 0 position] ctx))

  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [ctx/default-font
                                     ctx/batch
                                     ctx/unit-scale]}]
  (bitmap-font/draw! (or font default-font)
                     batch
                     {:scale (* (float unit-scale)
                                (float (or scale 1)))
                      :x x
                      :y y
                      :text text
                      :h-align h-align
                      :up? up?}))

(defmethod draw! :draw/ellipse [[_ [x y] radius-x radius-y color]
                                {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/sector! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/line! shape-drawer sx sy ex ey))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color] ctx]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw! [:draw/line [linex topy] [linex bottomy] color] ctx))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw! [:draw/line [leftx liney] [rightx liney] color] ctx))))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [ctx/shape-drawer]
                                         :as ctx}]
  (sd/with-line-width shape-drawer width
    (fn []
      (c/handle-draws! ctx draws))))

(defprotocol Viewports
  (update-viewports! [_]))

(q/defrecord Context [ctx/assets
                      ctx/batch
                      ctx/unit-scale
                      ctx/world-unit-scale
                      ctx/shape-drawer-texture
                      ctx/shape-drawer
                      ctx/cursors
                      ctx/default-font
                      ctx/world-viewport
                      ctx/ui-viewport
                      ctx/tiled-map-renderer
                      ctx/stage]
  Disposable
  (dispose [_]
    (Disposable/.dispose assets)
    (Disposable/.dispose batch)
    (Disposable/.dispose shape-drawer-texture)
    (run! Disposable/.dispose (vals cursors))
    (Disposable/.dispose default-font)
    ; TODO vis-ui dispose
    )

  Viewports
  (update-viewports! [_]
    (update-vp! ui-viewport)
    (update-vp! world-viewport))

  c/Assets
  (sound [_ path]
    (assets/sound assets path))

  (texture [_ path]
    (assets/texture assets path))

  (all-sounds [_]
    (assets/all-sounds assets))

  (all-textures [_]
    (assets/all-textures assets))

  c/Graphics
  (delta-time [_]
    (.getDeltaTime Gdx/graphics))

  (set-cursor! [_ cursor-key]
    (.setCursor Gdx/graphics (utils/safe-get cursors cursor-key)))

  (frames-per-second [_]
    (.getFramesPerSecond Gdx/graphics))

  (clear-screen! [_]
    (ScreenUtils/clear Color/BLACK))

  (handle-draws! [ctx draws]
    (doseq [component draws
            :when component]
      (draw! component ctx)))

  (set-camera-position! [_ position]
    (camera/set-position! (:camera world-viewport) position))

  (draw-on-world-viewport! [ctx fns]
    (draw-on-viewport! batch
                       world-viewport
                       (fn []
                         (sd/with-line-width shape-drawer world-unit-scale
                           (fn []
                             (doseq [f fns]
                               (f (assoc ctx :ctx/unit-scale world-unit-scale))))))))

  (draw-tiled-map! [_ tiled-map color-setter]
    (tiled/draw! (tiled-map-renderer tiled-map)
                 tiled-map
                 color-setter
                 (:camera world-viewport)))

  (world-mouse-position [_]
    (mouse-position world-viewport))

  (ui-mouse-position [_]
    (mouse-position ui-viewport))

  (ui-viewport-width [_]
    (:width ui-viewport))

  (ui-viewport-height [_]
    (:height ui-viewport))

  (world-viewport-width [_]
    (:width world-viewport))

  (world-viewport-height [_]
    (:height world-viewport))

  (camera-position [_]
    (camera/position (:camera world-viewport)))

  (inc-zoom! [_ amount]
    (camera/inc-zoom! (:camera world-viewport) amount))

  (camera-frustum [_]
    (camera/frustum (:camera world-viewport)))

  (visible-tiles [_]
    (camera/visible-tiles (:camera world-viewport)))

  (camera-zoom [_]
    (camera/zoom (:camera world-viewport)))

  (pixels->world-units [_ pixels]
    (* pixels world-unit-scale))

  (sprite [this texture-path]
    (sprite* (graphics/texture-region (c/texture this texture-path))
             world-unit-scale))

  (sub-sprite [_ sprite [x y w h]]
    (sprite* (graphics/sub-region (:texture-region sprite) x y w h)
             world-unit-scale))

  (sprite-sheet [this texture-path tilew tileh]
    {:image (sprite* (graphics/texture-region (c/texture this texture-path))
                     world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [this {:keys [image tilew tileh]} [x y]]
    (c/sub-sprite this image [(* x tilew) (* y tileh) tilew tileh]))

  c/Stage
  (draw-stage! [ctx]
    (reset! (.ctx stage) ctx)
    (ui/draw! stage)
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; so it has outdated context
    ; => maybe context should be an immutable data structure with mutable fields?
    #_(reset! (.ctx stage) nil)
    nil)

  (update-stage! [ctx]
    (reset! (.ctx stage) ctx)
    (ui/act! stage)
    ; We cannot pass this
    ; because input events are handled outside ui/act! and in the Lwjgl3Input system:
    ;                         com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.<init>   Lwjgl3Application.java:  153
    ;                           com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.loop   Lwjgl3Application.java:  181
    ;                              com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window.update        Lwjgl3Window.java:  414
    ;                        com.badlogic.gdx.backends.lwjgl3.DefaultLwjgl3Input.update  DefaultLwjgl3Input.java:  190
    ;                                            com.badlogic.gdx.InputEventQueue.drain     InputEventQueue.java:   70
    ;                             gdl.ui.proxy$gdl.ui.CtxStage$ILookup$a65747ce.touchUp                         :
    ;                                     com.badlogic.gdx.scenes.scene2d.Stage.touchUp               Stage.java:  354
    ;                              com.badlogic.gdx.scenes.scene2d.InputListener.handle       InputListener.java:   71
    #_@(.ctx stage)
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; so it has outdated context
    #_(reset! (.ctx stage) nil)
    nil)

  (get-actor [_ id]
    (id stage))

  (find-actor-by-name [_ name]
    (-> stage
        ui/root
        (ui/find-actor name)))

  (add-actor! [_ actor]
    (ui/add! stage actor))

  (mouseover-actor [this]
    (ui/hit stage (c/ui-mouse-position this)))

  (reset-actors! [_ actors]
    (ui/clear! stage)
    (run! #(ui/add! stage %) actors))

  c/Input
  (button-just-pressed? [_ button]
    (.isButtonJustPressed Gdx/input (button->code button)))

  (key-pressed? [_ key]
    (.isKeyPressed Gdx/input (k->code key)))

  (key-just-pressed? [_ key]
    (.isKeyJustPressed Gdx/input (k->code key))))

(defn create-state! [config]
  (ui/load! (:ui config))
  (let [batch (sprite-batch)
        shape-drawer-texture (white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))
        ui-viewport (create-ui-viewport (:ui-viewport config))
        stage (ui/stage (:java-object ui-viewport)
                        (:java-object batch))]
    (.setInputProcessor Gdx/input stage)
    (map->Context {:assets (assets/create (:assets config))
                   :batch batch
                   :unit-scale 1
                   :world-unit-scale world-unit-scale
                   :shape-drawer-texture shape-drawer-texture
                   :shape-drawer (sd/create (:java-object batch)
                                            (graphics/texture-region shape-drawer-texture 1 0 1 1))
                   :cursors (utils/mapvals
                             (fn [[file [hotspot-x hotspot-y]]]
                               (create-cursor (format (:cursor-path-format config) file)
                                              hotspot-x
                                              hotspot-y))
                             (:cursors config))
                   :default-font (truetype-font (:default-font config))
                   :world-viewport (create-world-viewport world-unit-scale
                                                          (:world-viewport config))
                   :ui-viewport ui-viewport
                   :tiled-map-renderer (memoize (fn [tiled-map]
                                                  (tiled/renderer tiled-map
                                                                  world-unit-scale
                                                                  (:java-object batch))))
                   :stage stage})))
