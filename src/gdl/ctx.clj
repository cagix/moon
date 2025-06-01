(ns gdl.ctx
  (:require [gdl.input :as input]
            [gdl.ui.stage :as stage]
            [gdl.viewport :as viewport]))

(defn world-mouse-position [{:keys [ctx/input
                                    ctx/world-viewport]}]
  (viewport/unproject world-viewport (input/mouse-position input)))

(defn ui-mouse-position [{:keys [ctx/input
                                 ctx/ui-viewport]}]
  (viewport/unproject ui-viewport (input/mouse-position input)))

(defn mouseover-actor [{:keys [ctx/stage] :as ctx}]
  (stage/hit stage (ui-mouse-position ctx)))
