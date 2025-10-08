(ns cdq.input
  (:require [clojure.string :as str]
            [clojure.math.vector2 :as v])
  (:import (com.badlogic.gdx Input
                             Input$Buttons
                             Input$Keys)))

(def controls
  {
   :zoom-in Input$Keys/MINUS
   :zoom-out Input$Keys/EQUALS
   :unpause-once Input$Keys/P
   :unpause-continously Input$Keys/SPACE
   :close-windows-key Input$Keys/ESCAPE
   :toggle-inventory  Input$Keys/I
   :toggle-entity-info Input$Keys/E
   :open-debug-button Input$Buttons/RIGHT
   }
  )

(def info-text
  (str/join "\n"
            ["[W][A][S][D] - Move"
             "[ESCAPE] - Close windows"
             "[I] - Inventory window"
             "[E] - Entity Info window"
             "[-]/[=] - Zoom"
             "[P]/[SPACE] - Unpause"
             "rightclick on tile or entity - open debug data window"
             "Leftmouse click - use skill/drop item on cursor"]))

(defn- unpause-once? [^Input input]
  (.isKeyJustPressed input (:unpause-once controls)))

(defn- unpause-continously? [^Input input]
  (.isKeyPressed      input (:unpause-continously controls)))

(defn- WASD-movement-vector [^Input input]
  (let [r (when (.isKeyPressed input Input$Keys/D) [1  0])
        l (when (.isKeyPressed input Input$Keys/A) [-1 0])
        u (when (.isKeyPressed input Input$Keys/W) [0  1])
        d (when (.isKeyPressed input Input$Keys/S) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn player-movement-vector [input]
  (WASD-movement-vector input))

(defn zoom-in? [^Input input]
  (.isKeyPressed input (:zoom-in  controls)))

(defn zoom-out? [^Input input]
  (.isKeyPressed input (:zoom-out controls)))

(defn close-windows? [^Input input]
  (.isKeyJustPressed input (:close-windows-key controls)))

(defn toggle-inventory? [^Input input]
  (.isKeyJustPressed input (:toggle-inventory controls) ))

(defn toggle-entity-info? [^Input input]
  (.isKeyJustPressed input (:toggle-entity-info controls)))

(defn unpause? [input]
  (or (unpause-once?        input)
      (unpause-continously? input)))

(defn open-debug-button-pressed? [^Input input]
  (.isButtonJustPressed input (:open-debug-button controls)))

(defn mouse-position [^Input input]
  [(.getX input)
   (.getY input)])

(defn left-mouse-button-just-pressed? [^Input input]
  (.isButtonJustPressed input Input$Buttons/LEFT))

(defn enter-just-pressed? [^Input input]
  (.isKeyJustPressed input Input$Keys/ENTER))
