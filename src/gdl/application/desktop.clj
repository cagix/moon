(ns gdl.application.desktop
  (:require [clojure.gdx :as gdx]
            [clojure.string :as str]
            [gdl.audio :as audio]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera]
            [gdl.graphics.texture :as texture]
            [gdl.graphics.g2d.texture-region :as texture-region]
            [gdl.graphics.viewport]
            [gdl.math.utils :as math-utils]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]
            [gdl.utils.disposable :as disposable])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color
                                      Colors
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      Texture$TextureFilter
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d Batch
                                          BitmapFont
                                          SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group
                                            Touchable)
           (com.badlogic.gdx.math Vector2
                                  Vector3)
           (com.badlogic.gdx.utils Disposable
                                   ScreenUtils)
           (com.badlogic.gdx.utils.viewport FitViewport)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)
           (gdl.ui CtxStage)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn- create-color
  ([r g b]
   (create-color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defn- ->color ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (gdx/k->Color c)
        (vector? c) (apply create-color c)
        :else (throw (ex-info "Cannot understand color" c))))

(defn- create-cursor [path [hotspot-x hotspot-y]]
  (let [pixmap (Pixmap. (.internal Gdx/files path))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defn- find-assets [{:keys [folder extensions]}]
  (map #(str/replace-first % folder "")
       (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
              result []]
         (cond (nil? file)
               result

               (.isDirectory file)
               (recur (concat remaining (.list file)) result)

               (extensions (.extension file))
               (recur remaining (conj result (.path file)))

               :else
               (recur remaining result)))))

(defn- create-font-params [{:keys [size
                                   min-filter
                                   mag-filter]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    (set! (.minFilter params) min-filter)
    (set! (.magFilter params) mag-filter)
    params))

(defn- generate-font [file-handle {:keys [size
                                          quality-scaling
                                          enable-markup?
                                          use-integer-positions?]}]
  (let [generator (FreeTypeFontGenerator. file-handle)
        ^BitmapFont font (.generateFont generator
                                        (create-font-params {:size (* size quality-scaling)
                                                             ; :texture-filter/linear because scaling to world-units
                                                             :min-filter Texture$TextureFilter/Linear
                                                             :mag-filter Texture$TextureFilter/Linear}))]
    (.setScale (.getData font) (/ quality-scaling))
    (set! (.markupEnabled (.getData font)) enable-markup?)
    (.setUseIntegerPositions font use-integer-positions?)
    font))

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

(defn- draw-texture-region! [^Batch batch texture-region [x y] [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))

(defmethod draw! :draw/texture-region [[_ texture-region [x y]]
                                       {:keys [batch]}]
  (draw-texture-region! batch
                        texture-region
                        [x y]
                        (texture-region/dimensions texture-region)
                        0  ;rotation
                        ))

(defmethod draw! :draw/image [[_ image position]
                              {:keys [batch]
                               :as graphics}]
  (let [texture-region (gdl.graphics/image->texture-region graphics image)]
    (draw-texture-region! batch
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
    (draw-texture-region! batch
                          texture-region
                          [(- (float x) (/ (float w) 2))
                           (- (float y) (/ (float h) 2))]
                          [w h]
                          rotation
                          )))

(defmethod draw! :draw/centered [[_ image position] this]
  (draw! [:draw/rotated-centered image 0 position] this))

(defn- draw-bitmap-font! [{:keys [^BitmapFont font batch text x y target-width align wrap?]}]
  (.draw font
         batch
         text
         (float x)
         (float y)
         (float target-width)
         (gdx/k->Align align)
         wrap?))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [batch
                                     unit-scale
                                     default-font]}]
  (let [^BitmapFont font (or font default-font)
        scale (* (float @unit-scale)
                 (float (or scale 1)))
        old-scale (.scaleX (.getData font))]
    (.setScale (.getData font) (float (* old-scale scale)))
    (draw-bitmap-font! {:font font
                        :batch batch
                        :text text
                        :x x
                        :y (+ y (if up? (text-height font text) 0))
                        :target-width 0
                        :align (or h-align :center)
                        :wrap? false})
    (.setScale (.getData font) (float old-scale))))

(defn- sd-set-color! [shape-drawer color]
  (ShapeDrawer/.setColor shape-drawer (->color color)))

(defmethod draw! :draw/ellipse [[_ [x y] radius-x radius-y color]
                                {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.ellipse shape-drawer
                        (float x)
                        (float y)
                        (float radius-x)
                        (float radius-y)))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.filledEllipse shape-drawer
                              (float x)
                              (float y)
                              (float radius-x)
                              (float radius-y)))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.circle shape-drawer
                       (float x)
                       (float y)
                       (float radius)))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.filledCircle shape-drawer
                             (float x)
                             (float y)
                             (float radius)))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.rectangle shape-drawer
                          (float x)
                          (float y)
                          (float w)
                          (float h)))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.filledRectangle shape-drawer
                                (float x)
                                (float y)
                                (float w)
                                (float h)))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.arc shape-drawer
                    (float center-x)
                    (float center-y)
                    (float radius)
                    (float (math-utils/degree->radians start-angle))
                    (float (math-utils/degree->radians degree))))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.sector shape-drawer
                       (float center-x)
                       (float center-y)
                       (float radius)
                       (float (math-utils/degree->radians start-angle))
                       (float (math-utils/degree->radians degree))))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.line shape-drawer
                     (float sx)
                     (float sy)
                     (float ex)
                     (float ey)))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color] this]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw! [:draw/line [linex topy] [linex bottomy] color] this))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw! [:draw/line [leftx liney] [rightx liney] color] this))))

(defn- sd-with-line-width [^ShapeDrawer this width draw-fn]
  (let [old-line-width (.getDefaultLineWidth this)]
    (.setDefaultLineWidth this (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth this (float old-line-width))))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [shape-drawer] :as this}]
  (sd-with-line-width shape-drawer width
    (fn []
      (gdl.graphics/handle-draws! this draws))))

(defrecord Graphics [graphics
                     textures
                     cursors
                     default-font
                     world-unit-scale
                     ui-viewport
                     world-viewport
                     ^SpriteBatch batch
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
    (ScreenUtils/clear (->color color)))

  (resize-viewports! [_ width height]
    (gdl.graphics.viewport/resize! ui-viewport    width height)
    (gdl.graphics.viewport/resize! world-viewport width height))

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
    (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (gdl.graphics.camera/combined (:camera world-viewport)))
    (.begin batch)
    (sd-with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (.end batch))

  (draw-tiled-map! [_ tiled-map color-setter]
    (let [^OrthogonalTiledMapRenderer renderer (tiled-map-renderer tiled-map)
          camera (:camera world-viewport)]
      (.setColorSetter renderer (reify ColorSetter
                                  (apply [_ color x y]
                                    (let [[r g b a] (color-setter color x y)]
                                      (Color/toFloatBits (float r)
                                                         (float g)
                                                         (float b)
                                                         (float a))))))
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

(extend-type OrthographicCamera
  gdl.graphics.camera/Camera
  (zoom [this]
    (.zoom this))

  (position [this]
    [(.x (.position this))
     (.y (.position this))])

  (combined [this]
    (.combined this))

  (frustum [this]
    (let [frustum-points (take 4 (map (fn [^Vector3 v3]
                                        [(.x v3)
                                         (.y v3)
                                         (.z v3)])
                                      (.planePoints (.frustum this))))
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

(defn- fit-viewport [width height camera {:keys [center-camera?]}]
  (let [this (FitViewport. width height camera)]
    (reify
      gdl.graphics.viewport/Viewport
      (resize! [_ width height]
        (.update this width height (boolean center-camera?)))

      ; touch coordinates are y-down, while screen coordinates are y-up
      ; so the clamping of y is reverse, but as black bars are equal it does not matter
      ; TODO clamping only works for gui-viewport ?
      ; TODO ? "Can be negative coordinates, undefined cells."
      (unproject [_ [x y]]
        (let [x (math-utils/clamp x (.getLeftGutterWidth this) (.getRightGutterX    this))
              y (math-utils/clamp y (.getTopGutterHeight this) (.getTopGutterY      this))]
          (let [vector2 (.unproject this (Vector2. x y))]
            [(.x vector2)
             (.y vector2)])))

      clojure.lang.ILookup
      (valAt [_ key]
        (case key
          :java-object this
          :width  (.getWorldWidth  this)
          :height (.getWorldHeight this)
          :camera (.getCamera this))))))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn- create-graphics
  [{:keys [textures
           colors ; optional
           cursors ; optional
           cursor-path-format ; optional
           default-font ; optional, could use gdx included (BitmapFont.)
           tile-size
           ui-viewport
           world-viewport]}]
  (doseq [[name color-params] colors]
    (Colors/put name (->color color-params)))
  (let [batch (SpriteBatch.)
        shape-drawer-texture (white-pixel-texture)
        world-unit-scale (float (/ tile-size))
        ui-viewport (fit-viewport (:width ui-viewport)
                                  (:height ui-viewport)
                                  (OrthographicCamera.)
                                  {:center-camera? true})
        textures-to-load (find-assets textures)
        ;(println "load-textures (count textures): " (count textures))
        textures (into {} (for [file textures-to-load]
                            [file (Texture. file)]))
        cursors (update-vals cursors
                             (fn [[file hotspot]]
                               (create-cursor (format cursor-path-format file) hotspot)))]
    (map->Graphics {:graphics Gdx/graphics
                    :textures textures
                    :cursors cursors
                    :default-font (when default-font
                                    (generate-font (.internal Gdx/files (:file default-font))
                                                   (:params default-font)))
                    :world-unit-scale world-unit-scale
                    :ui-viewport ui-viewport
                    :world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                          world-height (* (:height world-viewport) world-unit-scale)]
                                      (fit-viewport world-width
                                                    world-height
                                                    (doto (OrthographicCamera.)
                                                      (.setToOrtho false ; y-down?
                                                                   world-width
                                                                   world-height))
                                                    {:center-camera? false}))
                    :batch batch
                    :unit-scale (atom 1)
                    :shape-drawer-texture shape-drawer-texture
                    :shape-drawer (ShapeDrawer. batch (texture/region shape-drawer-texture 1 0 1 1))
                    :tiled-map-renderer (memoize (fn [tiled-map]
                                                   (OrthogonalTiledMapRenderer. (:tiled-map/java-object tiled-map)
                                                                                (float world-unit-scale)
                                                                                batch)))})))

(extend-type TextureRegion
  gdl.graphics.g2d.texture-region/TextureRegion
  (dimensions [texture-region]
    [(.getRegionWidth  texture-region)
     (.getRegionHeight texture-region)])
  (region [texture-region x y w h]
    (TextureRegion. texture-region
                    (int x)
                    (int y)
                    (int w)
                    (int h))))

(extend-type Texture
  gdl.graphics.texture/Texture
  (region
    ([texture]
     (TextureRegion. texture))
    ([texture x y w h]
     (TextureRegion. texture
                     (int x)
                     (int y)
                     (int w)
                     (int h)))))

(extend-type Group
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
(extend-type Actor
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
                           :disabled Touchable/disabled)))

  (remove! [actor]
    (.remove actor))

  (parent [actor]
    (.getParent actor)))

(extend-type Disposable
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(defn- create-audio [{:keys [sounds]}]
  (let [sounds (into {}
                     (for [path (find-assets sounds)]
                       [path (.newSound Gdx/audio (.internal Gdx/files path))]))]
    (reify
      disposable/Disposable
      (dispose! [_]
        (run! disposable/dispose! (vals sounds)))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (Sound/.play (get sounds path))))))

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
    {:ctx/input Gdx/input
     :ctx/graphics graphics
     :ctx/stage (let [stage (proxy [CtxStage clojure.lang.ILookup] [(:java-object (:ui-viewport graphics))
                                                                    (:batch graphics)
                                                                    (atom nil)]
                              (valAt [id]
                                (ui/find-actor-with-id (CtxStage/.getRoot this) id)))]
                  (.setInputProcessor Gdx/input stage)
                  stage)
     :ctx/audio (when audio
                  (create-audio audio))}))
