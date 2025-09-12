(ns cdq.render.update-mouse
  (:require [cdq.gdx.graphics :as graphics]
            [clojure.gdx.input :as input]
            [clojure.scenes.scenes2d.stage :as stage]))

(defn do!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (let [mouse-position (input/mouse-position input)
        ui-mouse-position    (graphics/unproject-ui    graphics mouse-position)
        world-mouse-position (graphics/unproject-world graphics mouse-position)]
    (assoc ctx
           :ctx/mouseover-actor      (stage/hit stage ui-mouse-position)
           :ctx/ui-mouse-position    ui-mouse-position
           :ctx/world-mouse-position world-mouse-position)))
