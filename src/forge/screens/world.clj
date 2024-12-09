(ns forge.screens.world
  (:require [anvil.app :refer [change-screen]]
            [anvil.controls :as controls]
            [anvil.graphics :refer [set-cursor world-camera]]
            [anvil.stage :as stage]
            [clojure.gdx.graphics :refer [clear-screen]]
            [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.scene2d.actor :refer [visible? set-visible] :as actor]
            [clojure.gdx.scene2d.group :refer [children]]
            [clojure.utils :refer [bind-root ->tile sort-by-order]]
            [forge.world.create :refer [dispose-world]]
            [forge.world.render :refer [render-world]]
            [forge.world.update :refer [update-world]]))

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

(defn enter [_]
  (cam/set-zoom! (world-camera) 0.8))

(defn exit [_]
  (set-cursor :cursors/default))

(defn render [_]
  (clear-screen color/black)
  (render-world)
  (update-world)
  (controls/world-camera-zoom)
  (check-window-hotkeys)
  (cond (controls/close-windows?)
        (close-all-windows)

        (controls/minimap?)
        (change-screen :screens/minimap)))

(defn dispose [_]
  (dispose-world))
