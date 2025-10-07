(ns clojure.scene2d.build.scroll-pane
  (:require [clojure.scene2d :as scene2d]
            [clojure.gdx.scenes.scene2d.actor :as actor])
  (:import (com.kotcrab.vis.ui.widget VisScrollPane)))

(defmethod scene2d/build :actor.type/scroll-pane
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (doto (VisScrollPane. actor)
          (.setFlickScroll false)
          (.setFadeScrollBars false))
    (actor/set-name! name)))
