(ns gdl.ui.actor
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.utils Align)
           (com.kotcrab.vis.ui.widget Tooltip VisLabel)))

(defn toggle-visible! [^Actor actor]
  (.setVisible actor (not (.isVisible actor))))

(defn add-tooltip!
  "tooltip-text is a (fn [context]) or a string. If it is a function will be-recalculated every show.
  Returns the actor."
  [^Actor actor tooltip-text]
  (let [text? (string? tooltip-text)
        label (VisLabel. (if text? tooltip-text ""))
        tooltip (proxy [Tooltip] []
                  ; hooking into getWidth because at
                  ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                  ; when tooltip position gets calculated we setText (which calls pack) before that
                  ; so that the size is correct for the newly calculated text.
                  (getWidth []
                    (let [^Tooltip this this]
                      (when-not text?
                        (.setText this (str (tooltip-text))))
                      (proxy-super getWidth))))]
    (.setAlignment label Align/center)
    (.setTarget  tooltip actor)
    (.setContent tooltip label))
  actor)

(defn remove-tooltip! [^Actor actor]
  (Tooltip/removeTooltip actor))
