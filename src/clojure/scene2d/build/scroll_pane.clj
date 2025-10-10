(ns clojure.scene2d.build.scroll-pane
  (:require [clojure.vis-ui.scroll-pane :as scroll-pane])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn create
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (scroll-pane/create actor)
    (Actor/.setName name)))
