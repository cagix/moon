(ns anvil.controls
  (:require [anvil.graphics.camera :as cam]
            [anvil.math.vector :as v]
            [clojure.gdx :as gdx]))

(defn unpaused? []
  (or (gdx/key-just-pressed? :keys/p)
      (gdx/key-pressed? :keys/space)))

(def ^:private zoom-speed 0.025)

(defn adjust-zoom [camera]
  (when (gdx/key-pressed? :keys/minus)  (cam/inc-zoom camera    zoom-speed))
  (when (gdx/key-pressed? :keys/equals) (cam/inc-zoom camera (- zoom-speed))))

(defn close-windows? []
  (gdx/key-just-pressed? :keys/escape))

(defn minimap? []
  (gdx/key-just-pressed? :keys/tab))

(def window-hotkeys
  {:inventory-window   :keys/i
   :entity-info-window :keys/e})

(defn toggle-visible? [window-id]
  (gdx/key-just-pressed? (get window-hotkeys window-id)))

(defn- add-vs [vs]
  (v/normalise (reduce v/add [0 0] vs)))

(defn- WASD-movement-vector []
  (let [r (when (gdx/key-pressed? :d) [1  0])
        l (when (gdx/key-pressed? :a) [-1 0])
        u (when (gdx/key-pressed? :w) [0  1])
        d (when (gdx/key-pressed? :s) [0 -1])]
    (when (or r l u d)
      (let [v (add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn movement-vector []
  (WASD-movement-vector))

(def help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")
