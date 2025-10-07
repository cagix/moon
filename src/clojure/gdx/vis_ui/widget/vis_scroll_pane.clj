(ns clojure.gdx.vis-ui.widget.vis-scroll-pane
  (:import (com.kotcrab.vis.ui.widget VisScrollPane)))

(defn create [actor
              {:keys [flick-scroll?
                      fade-scroll-bars?]}]
  (doto (VisScrollPane. actor)
    (.setFlickScroll flick-scroll?)
    (.setFadeScrollBars fade-scroll-bars?)))
