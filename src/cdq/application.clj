(ns cdq.application
  (:require cdq.graphics.shape-drawer
            [clojure.gdx.assets :as assets] ; all-of-type -> editor -> decide later
            [clojure.gdx.interop :as interop]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.utils])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture Texture$TextureFilter OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d BitmapFont SpriteBatch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math MathUtils)
           (com.badlogic.gdx.utils SharedLibraryLoader Os)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (gdl StageWithState OrthogonalTiledMapRenderer)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defprotocol Disposable
  (dispose! [_]))

(extend-type com.badlogic.gdx.utils.Disposable
  Disposable
  (dispose! [this]
    (.dispose this)))

(defn- load-assets [{:keys [folder
                            asset-type->extensions]}]
  (assets/create
   (for [[asset-type extensions] asset-type->extensions
         file (map #(str/replace-first % folder "")
                   (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
                          result []]
                     (cond (nil? file)
                           result

                           (.isDirectory file)
                           (recur (concat remaining (.list file)) result)

                           (extensions (.extension file))
                           (recur remaining (conj result (.path file)))

                           :else
                           (recur remaining result))))]
     [file asset-type])))

(defrecord Cursors []
  Disposable
  (dispose! [this]
    (run! dispose! (vals this))))

(defn- load-cursors [config]
  (map->Cursors
   (clojure.utils/mapvals
    (fn [[file [hotspot-x hotspot-y]]]
      (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
            cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
        (.dispose pixmap)
        cursor))
    config)))

(defn- font-params [{:keys [size]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- generate-font [file-handle params]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator (font-params params))]
    (.dispose generator)
    font))

(defn- load-font [{:keys [file size quality-scaling]}]
  (let [^BitmapFont font (generate-font (.internal Gdx/files file)
                                        {:size (* size quality-scaling)})]
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn- sd-set-color [shape-drawer color]
  (ShapeDrawer/.setColor shape-drawer (interop/->color color)))

(extend-type ShapeDrawer
  cdq.graphics.shape-drawer/ShapeDrawer
  (ellipse [this [x y] radius-x radius-y color]
    (sd-set-color this color)
    (.ellipse this
              (float x)
              (float y)
              (float radius-x)
              (float radius-y)))

  (filled-ellipse [this [x y] radius-x radius-y color]
    (sd-set-color this color)
    (.filledEllipse this
                    (float x)
                    (float y)
                    (float radius-x)
                    (float radius-y)))

  (circle [this [x y] radius color]
    (sd-set-color this color)
    (.circle this
             (float x)
             (float y)
             (float radius)))

  (filled-circle [this [x y] radius color]
    (sd-set-color this color)
    (.filledCircle this
                   (float x)
                   (float y)
                   (float radius)))

  (arc [this [center-x center-y] radius start-angle degree color]
    (sd-set-color this color)
    (.arc this
          (float center-x)
          (float center-y)
          (float radius)
          (float (degree->radians start-angle))
          (float (degree->radians degree))))

  (sector [this [center-x center-y] radius start-angle degree color]
    (sd-set-color this color)
    (.sector this
             (float center-x)
             (float center-y)
             (float radius)
             (float (degree->radians start-angle))
             (float (degree->radians degree))))

  (rectangle [this x y w h color]
    (sd-set-color this color)
    (.rectangle this
                (float x)
                (float y)
                (float w)
                (float h)))

  (filled-rectangle [this x y w h color]
    (sd-set-color this color)
    (.filledRectangle this
                      (float x)
                      (float y)
                      (float w)
                      (float h)))

  (line [this [sx sy] [ex ey] color]
    (sd-set-color this color)
    (.line this
           (float sx)
           (float sy)
           (float ex)
           (float ey)))

  (with-line-width [this width draw-fn]
    (let [old-line-width (.getDefaultLineWidth this)]
      (.setDefaultLineWidth this (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth this (float old-line-width)))))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn- create-stage! [config batch viewport]
  ; app crashes during startup before VisUI/dispose and we do cdq.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case (:skin-scale config)
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
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))
  (let [stage (proxy [StageWithState ILookup] [viewport batch]
                (valAt
                  ([id]
                   (group/find-actor-with-id (StageWithState/.getRoot this) id))
                  ([id not-found]
                   (or (group/find-actor-with-id (StageWithState/.getRoot this) id)
                       not-found))))]
    (.setInputProcessor Gdx/input stage)
    stage))

(defn- tiled-map-renderer [batch world-unit-scale]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))

(defn- fit-viewport
  "A ScalingViewport that uses Scaling.fit so it keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the remaining space."
  [width height camera & {:keys [center-camera?]}]
  {:pre [width height]}
  (proxy [FitViewport ILookup] [width height camera]
    (valAt
      ([key]
       (interop/k->viewport-field this key))
      ([key _not-found]
       (interop/k->viewport-field this key)))))

(defn- world-viewport [world-unit-scale config]
  {:pre [world-unit-scale]}
  (let [camera (OrthographicCamera.)
        world-width  (* (:width  config) world-unit-scale)
        world-height (* (:height config) world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width world-height camera)))

(defn- create-context []
  (let [batch (SpriteBatch.)
        shape-drawer-texture (white-pixel-texture)
        world-unit-scale (float (/ 48))
        ; TODO ui-viewport part of stage?
        ui-viewport (fit-viewport 1440 900 (OrthographicCamera.))]
    {:cdq/assets (load-assets {:folder "resources/"
                               :asset-type->extensions {:sound   #{"wav"}
                                                        :texture #{"png" "bmp"}}})
     :cdq.graphics/batch batch
     :cdq.graphics/cursors (load-cursors {:cursors/bag                   ["bag001"       [0   0]]
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
     :cdq.graphics/default-font (load-font {:file "fonts/exocet/films.EXL_____.ttf"
                                            :size 16
                                            :quality-scaling 2})
     :cdq.graphics/shape-drawer (ShapeDrawer. batch
                                              (TextureRegion. ^Texture shape-drawer-texture 1 0 1 1))
     :cdq.graphics/shape-drawer-texture shape-drawer-texture
     :cdq.graphics/tiled-map-renderer (tiled-map-renderer batch world-unit-scale)
     :cdq.graphics/ui-viewport ui-viewport
     :cdq.graphics/world-unit-scale world-unit-scale
     :cdq.graphics/world-viewport (world-viewport world-unit-scale {:width 1440 :height 900})
     :cdq.context/stage (create-stage! {:skin-scale :x1} batch ui-viewport)}))

(defn- dispose-game [context]
  (doseq [[k value] context]
    (if (satisfies? Disposable value)
      (do
       #_(println "Disposing:" k)
       (dispose! value))
      #_(println "Not Disposable: " k ))))

(defn- resize-game [context width height]
  ; could make 'viewport/update protocol' or 'on-resize' protocol
  ; and reify the viewports
  ; so we could have only one
  (Viewport/.update (:cdq.graphics/ui-viewport    context) width height true)
  (Viewport/.update (:cdq.graphics/world-viewport context) width height false))

(def state (atom nil))

(defn start! [create-game! game-loop!]
  (when  (= SharedLibraryLoader/os Os/MacOsX)
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource "moon.png")))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (reset! state (create-game! (create-context))))

                        (dispose []
                          (dispose-game @state))

                        (render []
                          (swap! state game-loop!))

                        (resize [width height]
                          (resize-game @state width height)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle "Cyber Dungeon Quest")
                        (.setWindowedMode 1440 900)
                        (.setForegroundFPS 60))))

(defn post-runnable [f]
  (.postRunnable Gdx/app (fn [] (f @state))))
