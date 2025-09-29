(ns com.kotcrab.vis.ui.widget.vis-check-box
  (:import (clojure.lang ILookup)
           (com.kotcrab.vis.ui.widget VisCheckBox)))

(defn create [text]
  (proxy [VisCheckBox ILookup] [(str text)]
    (valAt [k]
      (case k
        :check-box/checked? (VisCheckBox/.isChecked this)))))
