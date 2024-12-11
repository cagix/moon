(ns anvil.app
  (:require [anvil.controls :as controls]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.assets :as assets]
            [gdl.db :as db]
            [gdl.graphics :as g]
            [gdl.graphics.camera :as cam]
            [gdl.screen :as screen]
            [gdl.stage :as stage]
            [gdl.ui :as ui]
            [gdl.ui.actor :refer [visible? set-visible] :as actor]
            [gdl.ui.group :refer [children]]
            [forge.world.create :refer [create-world dispose-world]]
            [forge.world.render :refer [render-world]]
            [forge.world.update :refer [update-world]]))

; * Minimal dependencies editor (no world-viewport?, default-font,cursors?)

; * Mapgen Test (or test itself) a separate app make working - is the 'tests' app

; * Remove screens themself, tag it

; * Remove non-essential stuff (windows, widgets?, text?!)

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

(deftype WorldScreen []
  screen/Screen
  (enter [_]
    (cam/set-zoom! g/camera 0.8)) ; TODO no enter -> pass as arg to camera

  (exit [_]
    (g/set-cursor :cursors/default)) ; TODO no exit

  (render [_]
    (render-world)
    (update-world)
    (controls/adjust-zoom g/camera)
    (check-window-hotkeys)
    (when (controls/close-windows?)
      (close-all-windows)))

  (dispose [_]
    (dispose-world)))

(defn world-screen []
  (stage/screen :sub-screen (->WorldScreen)))

(defn- start [{:keys [db app-config graphics ui world-id]}]
  (db/setup db)
  (app/start app-config
             (reify app/Listener
               (create [_]
                 (assets/setup)
                 (g/setup graphics)
                 (ui/setup ui)
                 (screen/setup {:screens/world (world-screen)}
                               :screens/world)
                 (create-world (db/build world-id)))

               (dispose [_]
                 (assets/cleanup)
                 (g/cleanup)
                 (ui/cleanup)
                 (screen/cleanup))

               (render [_]
                 (g/clear-screen)
                 (screen/render-current))

               (resize [_ w h]
                 (g/resize w h)))))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))
