(ns forge.app
  (:require [anvil.assets :as assets]
            [anvil.controls :as controls]
            [anvil.db :as db]
            [anvil.graphics :as g]
            [anvil.graphics.camera :as cam]
            [anvil.input :refer [key-just-pressed?]]
            [anvil.screen :as screen]
            [anvil.stage :as stage]
            [anvil.sprite :as sprite]
            [anvil.ui :refer [ui-actor text-button] :as ui]
            [anvil.ui.actor :refer [visible? set-visible] :as actor]
            [anvil.ui.group :refer [children]]
            [anvil.utils :refer [dispose defmethods dev-mode?]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.screens.editor :as editor]
            [forge.screens.minimap :as minimap]
            [forge.world.create :refer [create-world]]
            [forge.world.create :refer [dispose-world]]
            [forge.world.render :refer [render-world]]
            [forge.world.update :refer [update-world]])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader ScreenUtils)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn setup-ui [skin-scale]
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (ui/loaded?)
    (ui/dispose))
  (ui/load skin-scale)
  (-> (ui/skin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  (ui/configure-tooltips {:default-appear-delay-time 0}))

(defn start [{:keys [db dock-icon asset-folder graphics ui-skin-scale title fps width height]}]
  (db/setup db)
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource dock-icon)))
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create  []
                          (assets/search-and-load asset-folder
                                                  [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                                                   [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]])
                          (g/create graphics)
                          (setup-ui ui-skin-scale)
                          (screen/setup (into {} (for [k [:screens/main-menu
                                                          :screens/map-editor
                                                          :screens/editor
                                                          :screens/minimap
                                                          :screens/world]]
                                                   [k (stage/screen [k])]))
                                        :screens/main-menu))

                        (dispose []
                          (assets/cleanup)
                          (g/cleanup)
                          (ui/dispose)
                          (screen/dispose-all))

                        (render []
                          (ScreenUtils/clear g/black)
                          (screen/render-current))

                        (resize [w h]
                          (g/resize w h)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setForegroundFPS fps)
                        (.setWindowedMode width height))))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))

(defn- background-image []
  (ui/image->widget (sprite/create "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

(defmethods :screens/main-menu
  (stage/actors [_]
    [(background-image)
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
                 [(text-button "Exit" #(.exit Gdx/app))]]))
       :cell-defaults {:pad-bottom 25}
       :fill-parent? true})
     (ui-actor {:act (fn []
                       (when (key-just-pressed? :keys/escape)
                         (.exit Gdx/app)))})])

  (screen/enter [_]
    (g/set-cursor :cursors/default)))

(defmethods :screens/editor
  (stage/actors [_]
    [(background-image)
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
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (children (windows))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

(defmethods :screens/world
  (screen/enter [_]
    (cam/set-zoom! g/camera 0.8))

  (screen/exit [_]
    (g/set-cursor :cursors/default))

  (screen/render [_]
    (render-world)
    (update-world)
    (controls/adjust-zoom g/camera)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (screen/change :screens/minimap)))

  (screen/dispose [_]
    (dispose-world)))
