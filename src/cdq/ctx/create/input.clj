(ns cdq.ctx.create.input
  (:require [cdq.input]
            [clojure.string :as str]
            [clojure.math.vector2 :as v])
  (:import (com.badlogic.gdx Input
                             Input$Buttons
                             Input$Keys)))

(defn do! [{:keys [ctx/gdx
                   ctx/stage]
            :as ctx}]
  (let [input (:clojure.gdx/input gdx)]
    (.setInputProcessor input stage)
    (assoc ctx :ctx/input input)))

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

(defn- unpause-once? [input]
  (.isKeyJustPressed input (:unpause-once controls)))

(defn- unpause-continously? [input]
  (.isKeyPressed      input (:unpause-continously controls)))

(defn- WASD-movement-vector [input]
  (let [r (when (.isKeyPressed input Input$Keys/D) [1  0])
        l (when (.isKeyPressed input Input$Keys/A) [-1 0])
        u (when (.isKeyPressed input Input$Keys/W) [0  1])
        d (when (.isKeyPressed input Input$Keys/S) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(extend-type com.badlogic.gdx.Input
  cdq.input/Input
  (player-movement-vector [input]
    (WASD-movement-vector input))

  (zoom-in? [input]
    (.isKeyPressed input (:zoom-in  controls)))

  (zoom-out? [input]
    (.isKeyPressed input (:zoom-out controls)))

  (close-windows? [input]
    (.isKeyJustPressed input (:close-windows-key controls)))

  (toggle-inventory? [input]
    (.isKeyJustPressed input (:toggle-inventory controls) ))

  (toggle-entity-info? [input]
    (.isKeyJustPressed input (:toggle-entity-info controls)))

  (unpause? [input]
    (or (unpause-once?        input)
        (unpause-continously? input)))

  (open-debug-button-pressed? [input]
    (.isButtonJustPressed input (:open-debug-button controls)))

  (mouse-position [input]
    [(.getX input)
     (.getY input)])

  (left-mouse-button-just-pressed? [input]
    (.isButtonJustPressed input Input$Buttons/LEFT))

  (enter-just-pressed? [input]
    (.isKeyJustPressed input Input$Keys/ENTER))

  (controls-info-text [_]
    info-text))
