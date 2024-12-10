(ns forge.app
  (:require [anvil.app :as app]
            [anvil.assets :as assets]
            [anvil.controls :as controls]
            [anvil.db :as db]
            [anvil.graphics :as graphics :refer [set-cursor world-camera]]
            [anvil.screen :as screen]
            [anvil.stage :as stage]
            [anvil.ui :refer [ui-actor text-button] :as ui]
            [clojure.awt :as awt]
            [clojure.component :refer [defsystem]]
            [clojure.edn :as edn]
            [clojure.gdx.asset-manager :as manager]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.input :as input :refer [key-just-pressed?]]
            [clojure.gdx.scene2d.actor :refer [visible? set-visible] :as actor]
            [clojure.gdx.scene2d.group :refer [children]]
            [clojure.gdx.scene2d.stage :as scene2d.stage]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.gdx.utils.viewport :as vp :refer [fit-viewport]]
            [clojure.java.io :as io]
            [clojure.lwjgl :as lwjgl]
            [clojure.string :as str]
            [clojure.utils :refer [bind-root defmethods dev-mode? mapvals]]
            [clojure.vis-ui :as vis]
            [forge.screens.editor :as editor]
            [forge.screens.minimap :as minimap]
            [forge.world.create :refer [create-world]]
            [forge.world.create :refer [dispose-world]]
            [forge.world.render :refer [render-world]]
            [forge.world.update :refer [update-world]])
  (:import (forge OrthogonalTiledMapRenderer)))

(defsystem setup)

(defsystem cleanup)
(defmethod cleanup :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defn start [{:keys [dock-icon lwjgl3 app]}]
  (awt/set-dock-icon dock-icon)
  (when shared-library-loader/mac?
    (lwjgl/configure-glfw-for-mac))
  (lwjgl3/app (reify lwjgl3/Listener
                (create  [_]     (run! setup           app))
                (dispose [_]     (run! cleanup         app))
                (render  [_]     (run! render          app))
                (resize  [_ w h] (run! #(resize % w h) app)))
              (lwjgl3/config lwjgl3)))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))

#_(def effect {:required [#'effect/applicable?
                          #'effect/handle]
               :optional [#'world.update/useful?
                          #'active-skill/render]})

#_(def entity
    {:optional [#'entity/->v
                #'entity/create
                #'world.update/destroy
                #'world.update/tick
                #'render/render-below
                #'render/render-default
                #'render/render-above
                #'render/render-info]})

#_(def entity-state
  (merge-with concat
              entity
              {:optional [#'fsm/enter
                          #'fsm/exit
                          #'fsm/cursor
                          #'world.update/pause-game?
                          #'world.update/manual-tick
                          #'forge.world.create/clicked-inventory-cell
                          #'clicked-skillmenu-skill
                          #'draw-gui-view]}))

; npc moving is basically a performance optimization so npcs do not have to check
; usable skills every frame
; also prevents fast twitching around changing directions every frame

(defmethods :db
  (setup [[_ config]]
    (db/setup config)))

(defmethods :asset-manager
  (setup [[_ folder]]
    (bind-root assets/manager (manager/load-all
                               (for [[asset-type exts] [[:sound   #{"wav"}]
                                                        [:texture #{"png" "bmp"}]]
                                     file (map #(str/replace-first % folder "")
                                               (files/recursively-search folder exts))]
                                 [file asset-type]))))

  (cleanup [_]
    (dispose assets/manager)))

(defmethods :sprite-batch
  (setup [_]
    (bind-root graphics/batch (g/sprite-batch)))

  (cleanup [_]
    (dispose graphics/batch)))

(let [pixel-texture (atom nil)]
  (defmethods :shape-drawer
    (setup [_]
      (reset! pixel-texture (let [pixmap (doto (g/pixmap 1 1)
                                           (.setColor color/white)
                                           (.drawPixel 0 0))
                                  texture (g/texture pixmap)]
                              (dispose pixmap)
                              texture))
      (bind-root graphics/sd (sd/create graphics/batch (g/texture-region @pixel-texture 1 0 1 1))))

    (cleanup [_]
      (dispose @pixel-texture))))

(defmethods :default-font
  (setup [[_ font]]
    (bind-root graphics/default-font (freetype/generate-font font)))

  (cleanup [_]
    (dispose graphics/default-font)))

(defmethods :cursors
  (setup [[_ data]]
    (bind-root graphics/cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                      (let [pixmap (g/pixmap (files/internal (str "cursors/" file ".png")))
                                            cursor (g/cursor pixmap hotspot-x hotspot-y)]
                                        (dispose pixmap)
                                        cursor))
                                    data)))

  (cleanup [_]
    (run! dispose (vals graphics/cursors))))

(defmethods :gui-viewport
  (setup [[_ [width height]]]
    (bind-root app/gui-viewport-width  width)
    (bind-root app/gui-viewport-height height)
    (bind-root app/gui-viewport (fit-viewport width height (g/orthographic-camera))))

  (resize [_ w h]
    (vp/update app/gui-viewport w h :center-camera? true)))

(defmethods :world-viewport
  (setup [[_ [width height tile-size]]]
    (bind-root app/world-unit-scale (float (/ tile-size)))
    (bind-root app/world-viewport-width  width)
    (bind-root app/world-viewport-height height)
    (bind-root app/world-viewport (let [world-width  (* width  app/world-unit-scale)
                                        world-height (* height app/world-unit-scale)
                                        camera (g/orthographic-camera)
                                        y-down? false]
                                    (.setToOrtho camera y-down? world-width world-height)
                                    (fit-viewport world-width world-height camera))))
  (resize [_ w h]
    (vp/update app/world-viewport w h)))

(defmethods :cached-map-renderer
  (setup [_]
    (bind-root app/cached-map-renderer
      (memoize (fn [tiled-map]
                 (OrthogonalTiledMapRenderer. tiled-map
                                              (float app/world-unit-scale)
                                              graphics/batch))))))

(defmethods :vis-ui
  (setup [[_ skin-scale]]
    ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
    ; => actually there is a deeper issue at play
    ; we need to dispose ALL resources which were loaded already ...
    (when (vis/loaded?)
      (vis/dispose))
    (vis/load skin-scale)
    (-> (vis/skin)
        (.getFont "default-font")
        .getData
        .markupEnabled
        (set! true))
    (vis/configure-tooltips {:default-appear-delay-time 0}))

  (cleanup [_]
    (vis/dispose)))

(defsystem actors)
(defmethod actors :default [_])

(defmethods :screens/stage
  (screen/enter [[_ {:keys [stage sub-screen]}]]
    (input/set-processor stage)
    (screen/enter sub-screen))

  (screen/exit [[_ {:keys [stage sub-screen]}]]
    (input/set-processor nil)
    (screen/exit sub-screen))

  (screen/render [[_ {:keys [stage sub-screen]}]]
    (scene2d.stage/act stage)
    (screen/render sub-screen)
    (scene2d.stage/draw stage))

  (screen/dispose [[_ {:keys [stage sub-screen]}]]
    (dispose stage)
    (screen/dispose sub-screen)))

(defmethods :screens
  (setup [[_ {:keys [screens first-k]}]]
    (screen/setup (into {}
                        (for [k screens]
                          [k [:screens/stage {:stage (scene2d.stage/create app/gui-viewport
                                                                           graphics/batch
                                                                           (actors [k]))
                                              :sub-screen [k]}]]))
                  first-k))

  (cleanup [_]
    (screen/dispose-all))

  (render [_]
    (g/clear-screen color/black)
    (screen/render-current)))

(defmethods :screens/main-menu
  (actors [_]
    [(ui/background-image)
     (ui/table
      {:rows
       (remove nil?
               (concat
                (for [world (db/build-all :properties/worlds)]
                  [(text-button (str "Start " (:property/id world))
                                #(do
                                  (screen/change :screens/world)
                                  (create-world world)))])
                [(when dev-mode?
                   [(text-button "Map editor"
                                 #(screen/change :screens/map-editor))])
                 (when dev-mode?
                   [(text-button "Property editor"
                                 #(screen/change :screens/editor))])
                 [(text-button "Exit" app/exit)]]))
       :cell-defaults {:pad-bottom 25}
       :fill-parent? true})
     (ui-actor {:act (fn []
                       (when (key-just-pressed? :keys/escape)
                         (app/exit)))})])

  (screen/enter [_]
    (set-cursor :cursors/default)))

(defmethods :screens/editor
  (actors [_]
    [(ui/background-image)
     (editor/tabs-table "[LIGHT_GRAY]Left-Shift: Back to Main Menu[]")
     (ui-actor {:act (fn []
                       (when (key-just-pressed? :shift-left)
                         (screen/change :screens/main-menu)))})]))

(defmethods :screens/minimap
  (screen/enter  [_] (minimap/enter))
  (screen/exit   [_] (minimap/exit))
  (screen/render [_] (minimap/render)))

(defn- windows []
  (:windows (stage/get)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (children (windows))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

(defmethods :screens/world
  (screen/enter [_]
    (cam/set-zoom! (world-camera) 0.8))

  (screen/exit [_]
    (set-cursor :cursors/default))

  (screen/render [_]
    (render-world)
    (update-world)
    (controls/world-camera-zoom)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (screen/change :screens/minimap)))

  (screen/dispose [_]
    (dispose-world)))
