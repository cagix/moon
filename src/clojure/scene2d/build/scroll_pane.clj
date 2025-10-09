(ns clojure.scene2d.build.scroll-pane
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.kotcrab.vis.ui.widget VisScrollPane)))

(defn create
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (doto (VisScrollPane. actor)
          (.setFlickScroll false)
          (.setFadeScrollBars false))
    (Actor/.setName name)))
