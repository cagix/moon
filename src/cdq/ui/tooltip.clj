(ns cdq.ui.tooltip
  (:require [cdq.ui.ctx-stage :as ctx-stage]
            [clojure.gdx.scenes.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.utils Align)
           (com.kotcrab.vis.ui.widget Tooltip
                                      VisLabel)))

(defn add!
  "tooltip-text is a (fn [context]) or a string. If it is a function will be-recalculated every show.  Returns the actor."
  [actor tooltip-text]
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
                        (let [actor (.getTarget this)
                              ; acturs might be initialized without a stage yet so we do when-let
                              ; FIXME double when-let
                              ctx (when-let [stage (actor/get-stage actor)]
                                    (ctx-stage/get-ctx stage))]
                          (when ctx ; ctx is only set later for update!/draw! ... not at starting of initialisation
                            (.setText this (str (tooltip-text ctx))))))
                      (proxy-super getWidth))))]
    (.setAlignment label Align/center)
    (.setTarget  tooltip actor)
    (.setContent tooltip label))
  actor)

(defn remove! [actor]
  (Tooltip/removeTooltip actor))
