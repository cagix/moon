(ns clojure.vis-ui.tooltip
  (:require [clojure.gdx.utils.align :as align]
            [clojure.vis-ui.label :as label])
  (:import (com.kotcrab.vis.ui.widget Tooltip)))

(defn add!
  [actor tooltip-text update-fn]
  (let [text? (string? tooltip-text)
        label (doto (label/create (if text? tooltip-text ""))
                (.setAlignment (align/k->value :center)))]
    (doto (proxy [Tooltip] []
            ; hooking into getWidth because at
            ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
            ; when tooltip position gets calculated we setText (which calls pack) before that
            ; so that the size is correct for the newly calculated text.
            (getWidth []
              (update-fn this)
              (proxy-super getWidth)))
      (.setTarget  actor)
      (.setContent label)))
  actor)

(defn remove! [actor]
  (Tooltip/removeTooltip actor))

(defn get-target [tooltip]
  (Tooltip/.getTarget tooltip))

(defn set-text! [tooltip text]
  (Tooltip/.setText tooltip (str text)))
