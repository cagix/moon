(ns gdl.application
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.freetype :as freetype]
            [clojure.gdx.shape-drawer :as sd]
            [clojure.gdx.tiled :as tiled]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [gdl.audio :as audio]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera]
            [gdl.graphics.texture :as texture]
            [gdl.graphics.g2d.texture-region :as texture-region]
            [gdl.graphics.viewport]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.stage]
            [gdl.utils.disposable])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3ApplicationConfiguration$GLEmulation
                                             Lwjgl3WindowConfiguration)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align
                                   SharedLibraryLoader
                                   Os)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip
                                      VisLabel)
           (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)
           (gdl.ui CtxStage)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

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

(defn- k->glversion [gl-version]
  (case gl-version
    :gl-emulation/angle-gles20 Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20
    :gl-emulation/gl20         Lwjgl3ApplicationConfiguration$GLEmulation/GL20
    :gl-emulation/gl30         Lwjgl3ApplicationConfiguration$GLEmulation/GL30
    :gl-emulation/gl31         Lwjgl3ApplicationConfiguration$GLEmulation/GL31
    :gl-emulation/gl32         Lwjgl3ApplicationConfiguration$GLEmulation/GL32))

(defn- set-window-config-key! [^Lwjgl3WindowConfiguration object k v]
  (case k
    :initial-visible? (.setInitialVisible object (boolean v))
    :windowed-mode   (.setWindowedMode object
                                       (int (:width v))
                                       (int (:height v)))
    :resizable? (.setResizable object (boolean v))
    :decorated? (.setDecorated object (boolean v))
    :maximized? (.setMaximized object (boolean v))
    ;:maximized-monitor (.setMaximizedMonitor object (map->monitor v))
    :auto-iconify? (.setAutoIconify object (boolean v))
    :window-position (.setWindowPosition object
                                         (int (:x v))
                                         (int (:y v)))
    :window-size-limits (.setWindowSizeLimits object
                                              (int (:min-width  v))
                                              (int (:min-height v))
                                              (int (:max-width  v))
                                              (int (:max-height v)))
    :window-icons (.setWindowIcon object ; TODO
                                  ; filetype
                                  ; array of string of file icons
                                  )
    :window-listener (.setWindowListener object
                                         ; Lwjgl3WindowListener v
                                         )
    :initial-background-color (.setInitialBackgroundColor object v)
    ;:fullscreen-mode (.setFullscreenMode object (map->display-mode v))
    :title (.setTitle object (str v))
    :vsync? (.useVsync object (boolean v))))

(defn- set-application-config-key! [^Lwjgl3ApplicationConfiguration object k v]
  (case k
    :audio (.setAudioConfig object
                            (int (:simultaneous-sources v))
                            (int (:buffer-size         v))
                            (int (:buffer-count        v)))
    :disable-audio? (.disableAudio object (boolean v))
    :max-net-threads (.setMaxNetThreads object (int v))
    :opengl-emulation (.setOpenGLEmulation object
                                           (k->glversion (:gl-version v))
                                           (int (:gles-3-major-version v))
                                           (int (:gles-3-minor-version v)))
    :backbuffer (.setBackBufferConfig object
                                      (int (:r       v))
                                      (int (:g       v))
                                      (int (:b       v))
                                      (int (:a       v))
                                      (int (:depth   v))
                                      (int (:stencil v))
                                      (int (:samples v)))
    :transparent-framebuffer (.setTransparentFramebuffer object (boolean v))
    :idle-fps (.setIdleFPS object (int v))
    :foreground-fps (.setForegroundFPS object (int v))
    :pause-when-minimized? (.setPauseWhenMinimized object (boolean v))
    :pause-when-lost-focus? (.setPauseWhenLostFocus object (boolean v))
    ; String preferencesDirectory, Files.FileType preferencesFileType
    #_(defmethod set-option! :preferences [object _ v]
        (.setPreferencesConfig object
                               (str (:directory v))
                               ; com.badlogic.gdx.Files.FileType
                               (k->filetype (:filetype v))))
    ; com.badlogic.gdx.graphics.glutils.HdpiMode/ 'Logical' / 'Pixels'
    #_(defmethod set-option! :hdpi-mode [object _ v]
        ; com.badlogic.gdx.graphics.glutils.HdpiMode
        (.setHdpiMode object (k->hdpi-mode v)))
    #_(defmethod set-option! :gl-debug-output? [object _ v]
        (.enableGLDebugOutput object
                              (boolean (:enable? v))
                              (->PrintStream (:debug-output-stream v))))
    (set-window-config-key! object k v)))

(let [mapping {Os/Android :android
               Os/IOS     :ios
               Os/Linux   :linux
               Os/MacOsX  :mac
               Os/Windows :windows}]
  (defn- operating-system []
    (get mapping SharedLibraryLoader/os)))

(defn start!
  [os-config
   lwjgl3-config
   context
   {:keys [create! dispose! render! resize!]}]
  (when (= (operating-system) :mac)
    (let [{:keys [glfw-async?
                  dock-icon]} (:mac os-config)]
      (when glfw-async?
        (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
      (when dock-icon
        (.setIconImage (Taskbar/getTaskbar)
                       (.getImage (Toolkit/getDefaultToolkit)
                                  (io/resource dock-icon))))))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (load-vis-ui! (:user-interface context))
                          (create! (let [graphics (create-graphics (:graphics context))
                                         stage (proxy [CtxStage clojure.lang.ILookup] [(:ui-viewport graphics)
                                                                                       (:batch graphics)
                                                                                       (atom nil)]
                                                 (valAt [id]
                                                   (ui/find-actor-with-id (CtxStage/.getRoot this) id)))]
                                     (gdx/set-input-processor! stage)
                                     {:ctx/input (gdx/input)
                                      :ctx/graphics graphics
                                      :ctx/stage stage
                                      :ctx/audio (when-let [audio (:audio context)]
                                                   (create-audio audio))})))
                        (dispose []
                          (dispose!))
                        (render []
                          (render!))
                        (resize [width height]
                          (resize! width height)))
                      (let [obj (Lwjgl3ApplicationConfiguration.)]
                        (doseq [[k v] lwjgl3-config]
                          (set-application-config-key! obj k v))
                        obj)))

(extend-type gdl.ui.CtxStage
  gdl.ui.stage/Stage
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

(extend-type com.badlogic.gdx.scenes.scene2d.Actor
  gdl.ui/PActorTooltips
  (add-tooltip! [actor tooltip-text]
    (let [text? (string? tooltip-text)
          label (VisLabel. (if text? tooltip-text ""))
          tooltip (proxy [Tooltip] []
                    ; hooking into getWidth because at
                    ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                    ; when tooltip position gets calculated we setText (which calls pack) before that
                    ; so that the size is correct for the newly calculated text.
                    (getWidth []
                      (let [^Tooltip this this]
                        (when-not text?
                          (let [actor (.getTarget this)
                                ctx (ui/get-stage-ctx actor)]
                            (when ctx ; ctx is only set later for update!/draw! ... not at starting of initialisation
                              (.setText this (str (tooltip-text ctx))))))
                        (proxy-super getWidth))))]
      (.setAlignment label Align/center)
      (.setTarget  tooltip actor)
      (.setContent tooltip label))
    actor)

  (remove-tooltip! [actor]
    (Tooltip/removeTooltip actor)))

(extend-type com.badlogic.gdx.scenes.scene2d.ui.Table
  gdl.ui/PTable
  (add-rows! [table rows]
    (doseq [row rows]
      (doseq [props-or-actor row]
        (cond
         ; this is weird now as actor declarations are all maps ....
         (map? props-or-actor) (-> (ui/add! table (:actor props-or-actor))
                                   (ui/set-cell-opts! (dissoc props-or-actor :actor)))
         :else (ui/add! table props-or-actor)))
      (.row table))
    table))

(extend-protocol gdl.ui/CanAddActor
  com.badlogic.gdx.scenes.scene2d.Group
  (add! [group actor]
    (.addActor group (ui/-create-actor actor)))

  com.badlogic.gdx.scenes.scene2d.Stage
  (add! [stage actor]
    (.addActor stage (ui/-create-actor actor)))

  com.badlogic.gdx.scenes.scene2d.ui.Table
  (add! [table actor]
    (.add table (ui/-create-actor actor))))

(extend-protocol gdl.ui/CanHit
  com.badlogic.gdx.scenes.scene2d.Actor
  (hit [actor [x y]]
    (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
      (.hit actor (.x v) (.y v) true)))

  com.badlogic.gdx.scenes.scene2d.Stage
  (hit [stage [x y]]
    (.hit stage x y true)))
