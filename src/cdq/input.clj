(ns cdq.input
  (:require [com.badlogic.gdx.input :as input]
            [gdl.math.vector2 :as v]))

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

(defn- unpause-once? [input]
  (input/key-just-pressed? input (:unpause-once controls)))

(defn- unpause-continously? [input]
  (input/key-pressed?      input (:unpause-continously controls)))

(defn create! [input input-processor]
  (assert input-processor)
  (input/set-processor! input input-processor)
  input)

(defn- WASD-movement-vector [input]
  (let [r (when (input/key-pressed? input :d) [1  0])
        l (when (input/key-pressed? input :a) [-1 0])
        u (when (input/key-pressed? input :w) [0  1])
        d (when (input/key-pressed? input :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn player-movement-vector [input]
  (WASD-movement-vector input))

(defn zoom-in? [input]
  (input/key-pressed? input (:zoom-in  controls)))

(defn zoom-out? [input]
  (input/key-pressed? input (:zoom-out controls)))

(defn close-windows? [input]
  (input/key-just-pressed? input (:close-windows-key controls)))

(defn toggle-inventory? [input]
  (input/key-just-pressed? input (:toggle-inventory controls) ))

(defn toggle-entity-info? [input]
  (input/key-just-pressed? input (:toggle-entity-info controls)))

(defn unpause? [input]
  (or (unpause-once?        input)
      (unpause-continously? input)))

(defn open-debug-button-pressed? [input]
  (input/button-just-pressed? input (:open-debug-button controls)))

(defn mouse-position [input]
  (input/mouse-position input))
