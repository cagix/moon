(ns forge.app
  (:require [clojure.java.io :as io]
            [forge.assets :as assets]
            [forge.screen :as screen]
            [forge.graphics.cursors :as cursors]
            [forge.graphics.image :as image]
            [forge.graphics.shape-drawer :as sd]
            [forge.graphics.text :as text]
            [forge.graphics.tiled :as tiled]
            [forge.graphics.viewport :as vp]
            [forge.ui :as ui]
            [forge.ui.actor :as actor]
            [forge.ui.stage :as stage]
            [forge.utils :refer [dispose mapvals]])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader ScreenUtils)
           (org.lwjgl.system Configuration)
           (java.awt Taskbar Toolkit)

           (com.badlogic.gdx.graphics Color OrthographicCamera Texture)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)))

(declare ^:private batch
         ^:private shape-drawer
         ^:private shape-drawer-texture
         ^:private default-font
         ^:private cached-map-renderer
         ^:private world-unit-scale
         ^:private world-viewport
         ^:private gui-viewport
         ^:private current-screen-key
         ^:private screens)

(defn current-screen []
  (and (bound? #'current-screen-key)
       (current-screen-key screens)))

(defn change-screen
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current-screen)]
    (screen/exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (.bindRoot #'current-screen-key new-k)
    (screen/enter screen)))

(defn- set-dock-icon [image-path]
  (let [toolkit (Toolkit/getDefaultToolkit)
        image (.getImage toolkit (io/resource image-path))
        taskbar (Taskbar/getTaskbar)]
    (.setIconImage taskbar image)))

(defn start-app [{:keys [tile-size
                         world-viewport-width
                         world-viewport-height
                         gui-viewport-width
                         gui-viewport-height
                         ui-skin-scale
                         init-screens
                         first-screen-k]}]
  (set-dock-icon "moon.png")
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (assets/init)
                          (cursors/init)
                          (.bindRoot #'batch (SpriteBatch.))
                          (.bindRoot #'shape-drawer-texture (sd/white-pixel-texture))
                          (.bindRoot #'shape-drawer (sd/create batch shape-drawer-texture))
                          (.bindRoot #'default-font (text/truetype-font
                                                     {:file "fonts/exocet/films.EXL_____.ttf"
                                                      :size 16
                                                      :quality-scaling 2}))
                          (.bindRoot #'world-unit-scale (float (/ tile-size)))
                          (.bindRoot #'world-viewport (let [world-width  (* world-viewport-width world-unit-scale)
                                                            world-height (* world-viewport-height world-unit-scale)
                                                            camera (OrthographicCamera.)
                                                            y-down? false]
                                                        (.setToOrtho camera y-down? world-width world-height)
                                                        (FitViewport. world-width world-height camera)))
                          (.bindRoot #'cached-map-renderer (memoize
                                                            (fn [tiled-map]
                                                              (tiled/renderer tiled-map world-unit-scale batch))))
                          (.bindRoot #'gui-viewport (FitViewport. gui-viewport-width
                                                                  gui-viewport-height
                                                                  (OrthographicCamera.)))
                          (ui/load! ui-skin-scale)
                          (.bindRoot #'screens (mapvals #(screen/stage-screen gui-viewport batch %)
                                                        (init-screens)))
                          (change-screen first-screen-k))

                        (dispose []
                          (assets/dispose)
                          (cursors/dispose)
                          (dispose batch)
                          (dispose shape-drawer-texture)
                          (dispose default-font)
                          (run! screen/dispose (vals screens))
                          (ui/dispose!))

                        (render []
                          (ScreenUtils/clear Color/BLACK)
                          (screen/render (current-screen)))

                        (resize [w h]
                          (vp/update gui-viewport   [w h] :center-camera? true)
                          (vp/update world-viewport [w h])))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle "Moon")
                        (.setForegroundFPS 60)
                        (.setWindowedMode 1440 900))))

(def ^:dynamic ^:private *unit-scale* 1)

(defn gui-mouse-position []
  ; TODO mapv int needed?
  (mapv int (vp/unproject-mouse-posi gui-viewport)))

(defn gui-viewport-width  [] (vp/world-width  gui-viewport))
(defn gui-viewport-height [] (vp/world-height gui-viewport))

(defn pixels->world-units [pixels]
  (* (int pixels) world-unit-scale))

(defn world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (vp/unproject-mouse-posi world-viewport))

(defn world-camera          [] (vp/camera       world-viewport))
(defn world-viewport-width  [] (vp/world-width  world-viewport))
(defn world-viewport-height [] (vp/world-height world-viewport))

(defn image [path]
  (image/create world-unit-scale
                (assets/texture-region path)))

(defn sub-image [image bounds]
  (image/sub-image world-unit-scale
                   image
                   bounds))

(defn sprite-sheet [path tilew tileh]
  {:image (image path)
   :tilew tilew
   :tileh tileh})

(defn sprite [sprite-sheet index]
  (image/sprite world-unit-scale
                sprite-sheet
                index))

(defn draw-text [opts]
  (text/draw batch *unit-scale* default-font opts))

(defn draw-image [image position]
  (image/draw batch *unit-scale* image position))

(defn draw-centered [image position]
  (image/draw-centered batch *unit-scale* image position))

(defn draw-rotated-centered [image rotation position]
  (image/draw-rotated-centered batch *unit-scale* image rotation position))

(defn draw-ellipse [position radius-x radius-y color]
  (sd/set-color shape-drawer color)
  (sd/ellipse shape-drawer position radius-x radius-y))

(defn draw-filled-ellipse [position radius-x radius-y color]
  (sd/set-color shape-drawer color)
  (sd/filled-ellipse shape-drawer position radius-x radius-y))

(defn draw-circle [position radius color]
  (sd/set-color shape-drawer color)
  (sd/circle shape-drawer position radius))

(defn draw-filled-circle [position radius color]
  (sd/set-color shape-drawer color)
  (sd/filled-circle shape-drawer position radius))

(defn draw-arc [center radius start-angle degree color]
  (sd/set-color shape-drawer color)
  (sd/arc shape-drawer center radius start-angle degree))

(defn draw-sector [center radius start-angle degree color]
  (sd/set-color shape-drawer color)
  (sd/sector shape-drawer center radius start-angle degree))

(defn draw-rectangle [x y w h color]
  (sd/set-color shape-drawer color)
  (sd/rectangle shape-drawer x y w h))

(defn draw-filled-rectangle [x y w h color]
  (sd/set-color shape-drawer color)
  (sd/filled-rectangle shape-drawer x y w h))

(defn draw-line [start end color]
  (sd/set-color shape-drawer color)
  (sd/line shape-drawer start end))

(defn draw-grid [leftx bottomy gridw gridh cellw cellh color]
  (sd/set-color shape-drawer color)
  (sd/grid shape-drawer leftx bottomy gridw gridh cellw cellh color))

(defn with-line-width [width draw-fn]
  (sd/with-line-width shape-drawer width draw-fn))

(defn- draw-with [^Viewport viewport unit-scale draw-fn]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (.combined (.getCamera viewport)))
  (.begin batch)
  (with-line-width unit-scale
    #(binding [*unit-scale* unit-scale]
       (draw-fn)))
  (.end batch))

(defn draw-on-world-view [render-fn]
  (draw-with world-viewport world-unit-scale render-fn))

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [tiled-map color-setter]
  (tiled/render (cached-map-renderer tiled-map)
                color-setter
                (world-camera)
                tiled-map))

(defn stage []
  (:stage (current-screen)))

(defn mouse-on-actor? []
  (stage/hit (stage) (gui-mouse-position) :touchable? true))

(defn add-actor [actor]
  (stage/add! (stage) actor))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (::modal (stage))))
  (add-actor
   (ui/window {:title title
               :rows [[(ui/label text)]
                      [(ui/text-button button-text
                                       (fn []
                                         (actor/remove! (::modal (stage)))
                                         (on-click)))]]
               :id ::modal
               :modal? true
               :center-position [(/ (gui-viewport-width) 2)
                                 (* (gui-viewport-height) (/ 3 4))]
               :pack? true})))
