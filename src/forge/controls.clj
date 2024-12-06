(ns forge.controls
  (:require [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.input :refer [key-just-pressed?
                                       key-pressed?]]
            [forge.app.world-viewport :refer [world-camera]]
            [forge.core :refer :all]))

(defn unpaused? []
  (or (key-just-pressed? :keys/p)
      (key-pressed? :keys/space)))

(def ^:private zoom-speed 0.025)

(defn- inc-zoom [camera by]
  (cam/set-zoom! camera (max 0.1 (+ (cam/zoom camera) by))))

(defn world-camera-zoom []
  (let [camera (world-camera)]
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

(defn- add-vs [vs]
  (v-normalise (reduce v-add [0 0] vs)))

(defn- WASD-movement-vector []
  (let [r (when (key-pressed? :d) [1  0])
        l (when (key-pressed? :a) [-1 0])
        u (when (key-pressed? :w) [0  1])
        d (when (key-pressed? :s) [0 -1])]
    (when (or r l u d)
      (let [v (add-vs (remove nil? [r l u d]))]
        (when (pos? (v-length v))
          v)))))

(defn movement-vector []
  (WASD-movement-vector))

(def help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")
