(ns clojure.ctx.mouse-over
  (:require [clojure.graphics.viewport :as viewport]
            [clojure.input :as input]
            [clojure.ui.stage :as stage]))

(defn world-mouse-position [{:keys [ctx/input
                                    ctx/world-viewport]}]
  (viewport/unproject world-viewport (input/mouse-position input)))

(defn ui-mouse-position [{:keys [ctx/input
                                 ctx/ui-viewport]}]
  (viewport/unproject ui-viewport (input/mouse-position input)))

(defn mouseover-actor [{:keys [ctx/stage] :as ctx}]
  (stage/hit stage (ui-mouse-position ctx)))
