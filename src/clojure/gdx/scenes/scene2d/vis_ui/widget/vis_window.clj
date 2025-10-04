(ns clojure.gdx.scenes.scene2d.vis-ui.widget.vis-window
  (:import (com.kotcrab.vis.ui.widget VisWindow)))

(defn create [{:keys [title
                      close-button?
                      center?
                      close-on-escape?
                      show-window-border?]}]
  (let [window (VisWindow. ^String title (boolean show-window-border?))]
    (when close-button?    (.addCloseButton window))
    (when center?          (.centerWindow   window))
    (when close-on-escape? (.closeOnEscape  window))
    window))
