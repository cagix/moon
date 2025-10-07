(ns clojure.scene2d.build.scroll-pane
  (:require [clojure.scene2d :as scene2d])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.kotcrab.vis.ui.widget VisScrollPane)))

(defmethod scene2d/build :actor.type/scroll-pane
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (doto (VisScrollPane. actor)
          (.setFlickScroll false)
          (.setFadeScrollBars false))
    (Actor/.setName name)))
