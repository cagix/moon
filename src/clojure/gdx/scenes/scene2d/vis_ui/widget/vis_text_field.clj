(ns clojure.gdx.scenes.scene2d.vis-ui.widget.vis-text-field
  (:import (clojure.lang ILookup)
           (com.kotcrab.vis.ui.widget VisTextField)))

(defn create [text]
  (proxy [VisTextField ILookup] [(str text)]
    (valAt [k]
      (case k
        :text-field/text (VisTextField/.getText this)))))
