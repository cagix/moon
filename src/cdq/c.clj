(ns cdq.c
  (:require [cdq.ctx.graphics :as graphics]
            [cdq.ctx.input :as input]
            [cdq.ui.stage :as stage]))

(defn world-mouse-position [{:keys [ctx/input
                                    ctx/graphics]}]
  (graphics/unproject-world graphics (input/mouse-position input)))

(defn ui-mouse-position [{:keys [ctx/input
                                 ctx/graphics]}]
  (graphics/unproject-ui graphics (input/mouse-position input)))

(defn mouseover-actor [{:keys [ctx/stage] :as ctx}]
  (stage/hit stage (ui-mouse-position ctx)))
