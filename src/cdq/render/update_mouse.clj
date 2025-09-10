(ns cdq.render.update-mouse
  (:require [cdq.gdx.graphics :as graphics]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/input
           ctx/stage]
    :as ctx}]
  (let [mouse-position (input/mouse-position input)
        ui-mouse-position    (graphics/unproject-ui    ctx mouse-position)
        world-mouse-position (graphics/unproject-world ctx mouse-position)]
    (assoc ctx
           :ctx/mouseover-actor      (stage/hit stage ui-mouse-position)
           :ctx/ui-mouse-position    ui-mouse-position
           :ctx/world-mouse-position world-mouse-position)))
