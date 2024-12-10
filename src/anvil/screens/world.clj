(ns anvil.screens.world
  (:require [anvil.graphics :as g]
            [anvil.graphics.camera :as cam]
            [anvil.controls :as controls]
            [anvil.screen :as screen]
            [anvil.stage :as stage]
            [anvil.ui.actor :refer [visible? set-visible] :as actor]
            [anvil.ui.group :refer [children]]
            [forge.world.create :refer [dispose-world]]
            [forge.world.render :refer [render-world]]
            [forge.world.update :refer [update-world]]))

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
    (cam/set-zoom! g/camera 0.8))

  (exit [_]
    (g/set-cursor :cursors/default))

  (render [_]
    (render-world)
    (update-world)
    (controls/adjust-zoom g/camera)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (screen/change :screens/minimap)))

  (dispose [_]
    (dispose-world)))

(defn screen []
  (stage/screen :sub-screen (->WorldScreen)))
