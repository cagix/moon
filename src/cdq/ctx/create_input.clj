(ns cdq.ctx.create-input
  (:require [cdq.input]
            [clojure.string :as str]
            [com.badlogic.gdx.input :as input]
            [gdl.math.vector2 :as v]))

(defn do! [{:keys [ctx/input
                   ctx/stage]
            :as ctx}]
  (input/set-processor! input stage)
  (assoc ctx :ctx/input input))

(def controls
  {
   :zoom-in :minus
   :zoom-out :equals
   :unpause-once :p
   :unpause-continously :space
   :close-windows-key :escape
   :toggle-inventory  :i
   :toggle-entity-info :e
   :open-debug-button :right
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
  (input/key-just-pressed? input (:unpause-once controls)))

(defn- unpause-continously? [input]
  (input/key-pressed?      input (:unpause-continously controls)))

(defn- WASD-movement-vector [input]
  (let [r (when (input/key-pressed? input :d) [1  0])
        l (when (input/key-pressed? input :a) [-1 0])
        u (when (input/key-pressed? input :w) [0  1])
        d (when (input/key-pressed? input :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(extend-type com.badlogic.gdx.Input
  cdq.input/Input
  (player-movement-vector [input]
    (WASD-movement-vector input))

  (zoom-in? [input]
    (input/key-pressed? input (:zoom-in  controls)))

  (zoom-out? [input]
    (input/key-pressed? input (:zoom-out controls)))

  (close-windows? [input]
    (input/key-just-pressed? input (:close-windows-key controls)))

  (toggle-inventory? [input]
    (input/key-just-pressed? input (:toggle-inventory controls) ))

  (toggle-entity-info? [input]
    (input/key-just-pressed? input (:toggle-entity-info controls)))

  (unpause? [input]
    (or (unpause-once?        input)
        (unpause-continously? input)))

  (open-debug-button-pressed? [input]
    (input/button-just-pressed? input (:open-debug-button controls)))

  (mouse-position [input]
    (input/mouse-position input))

  (left-mouse-button-just-pressed? [input]
    (input/button-just-pressed? input :left))

  (enter-just-pressed? [input]
    (input/key-just-pressed? input :enter))

  (controls-info-text [_]
    info-text))
