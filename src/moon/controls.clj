(ns moon.controls
  (:require [gdl.graphics.camera :as cam]
            [gdl.graphics.world-view :as world-view]
            [gdl.input :refer [key-pressed? key-just-pressed?]]))

(defn unpaused? []
  (or (key-just-pressed? :keys/p)
      (key-pressed? :keys/space)))

(def ^:private zoom-speed 0.025)

(defn- inc-zoom [camera by]
  (cam/set-zoom! camera (max 0.1 (+ (cam/zoom camera) by))))

(defn world-camera-zoom []
  (let [camera (world-view/camera)]
    (when (key-pressed? :keys/minus)  (inc-zoom camera    zoom-speed))
    (when (key-pressed? :keys/equals) (inc-zoom camera (- zoom-speed)))))

(defn close-windows? []
  (key-just-pressed? :keys/escape))

(defn minimap? []
  (key-just-pressed? :keys/tab))

(def ^:private window-hotkeys
  {:inventory-window   :keys/i
   :entity-info-window :keys/e})

(defn toggle-visible? [window-id]
  (key-just-pressed? (get window-hotkeys window-id)))

(def help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")
