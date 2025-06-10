(ns gdl.c
  (:require [gdl.graphics.viewport :as viewport]
            [gdl.input :as input]
            [gdl.ui.stage :as stage]))

(defn world-mouse-position [{:keys [ctx/gdx
                                    ctx/graphics]}]
  (viewport/unproject (:world-viewport graphics) (input/mouse-position gdx)))

(defn ui-mouse-position [{:keys [ctx/gdx
                                 ctx/graphics]}]
  (viewport/unproject (:ui-viewport graphics) (input/mouse-position gdx)))

(defn mouseover-actor [{:keys [ctx/stage] :as ctx}]
  (stage/hit stage (ui-mouse-position ctx)))
