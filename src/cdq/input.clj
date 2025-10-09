(ns cdq.input
  (:require [clojure.input :as input]
            [clojure.string :as str]
            [clojure.math.vector2 :as v])
  (:import (com.badlogic.gdx Input$Buttons
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

(defn- WASD-movement-vector [gdx]
  (let [r (when (input/key-pressed? gdx Input$Keys/D) [1  0])
        l (when (input/key-pressed? gdx Input$Keys/A) [-1 0])
        u (when (input/key-pressed? gdx Input$Keys/W) [0  1])
        d (when (input/key-pressed? gdx Input$Keys/S) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn player-movement-vector [gdx]
  (WASD-movement-vector gdx))
